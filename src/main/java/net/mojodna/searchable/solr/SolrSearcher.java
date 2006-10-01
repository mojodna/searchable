/*
 * Copyright 2005-2006 Seth Fitzsimmons <seth@mojodna.net>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.mojodna.searchable.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.mojodna.searchable.GenericResult;
import net.mojodna.searchable.IndexException;
import net.mojodna.searchable.IndexSupport;
import net.mojodna.searchable.IndexingException;
import net.mojodna.searchable.Result;
import net.mojodna.searchable.ResultSet;
import net.mojodna.searchable.ResultSetImpl;
import net.mojodna.searchable.SearchException;
import net.mojodna.searchable.Searchable;
import net.mojodna.searchable.SearchableBeanUtils;
import net.mojodna.searchable.Searcher;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Queries a Solr server for results.
 * 
 * @author Seth Fitzsimmons
 */
public class SolrSearcher implements Searcher<Searchable> {
    private HttpClient httpClient;

    private String solrHost = "localhost";

    private String solrPath = "/solr/select";

    private int solrPort = 8983;
    
    private static final Logger log = Logger.getLogger(SolrSearcher.class);

    public ResultSet search(final String query) throws IndexException {
        return search(query, null, null);
    }

    public ResultSet search(final String query, final Integer start, final Integer count) throws IndexException {
        try {
            final GetMethod get = new GetMethod(solrPath);
            final List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new NameValuePair("q", query));
            if (null != start) {
                params.add(new NameValuePair("start", start.toString()));
            }
            if (null != count) {
                params.add(new NameValuePair("rows", count.toString()));
            }
            params.add(new NameValuePair("version", "2.1"));
            params.add(new NameValuePair("indent", "on"));
            get.setQueryString(params.toArray(new NameValuePair[] {}));
            final int responseCode = getHttpClient().executeMethod(get);
            
            final SAXBuilder builder = new SAXBuilder();
            final Document response = builder.build(get.getResponseBodyAsStream());
            final Element resultNode = response.getRootElement().getChild("result");
            
            final ResultSetImpl resultSet = new ResultSetImpl(new Integer(resultNode.getAttributeValue("numFound")));
            resultSet.setOffset(new Integer(resultNode.getAttributeValue("start")));
            List<Element> docs = resultNode.getChildren("doc");
            for (int i = 0; i < docs.size(); i++) {
                final Element doc = docs.get(i);
                Result result = null;

                // load the class name
                String className = null;
                String id = null;
                String idType = null;
                for (final Iterator it = doc.getChildren("str").iterator(); it.hasNext(); ) {
                    final Element str = (Element) it.next();
                    final String name = str.getAttributeValue("name");
                    if (IndexSupport.TYPE_FIELD_NAME.equals(name)) {
                        className = str.getTextTrim();
                    } else if (IndexSupport.ID_FIELD_NAME.equals(name)) {
                        id = str.getTextTrim();
                    } else if (IndexSupport.ID_TYPE_FIELD_NAME.equals(name)) {
                        idType = str.getTextTrim();
                    }
                }
                
                try {
                    // attempt to instantiate an instance of the specified class
                    try {
                        if (null != className) {
                            final Object o = Class.forName(className).newInstance();
                            if (o instanceof Result) {
                                log.debug("Created new instance of: " + className);
                                result = (Result) o;
                            }
                        }
                    } catch (final ClassNotFoundException e) {
                        // class was invalid, or something
                    }

                    // fall back to a GenericResult as a container
                    if (null == result)
                        result = new GenericResult();

                    if (result instanceof Searchable) {
                        // special handling for searchables
                        final String idField = SearchableBeanUtils
                                .getIdPropertyName(((Searchable) result).getClass());

                        // attempt to load the id and set the id property on the Searchable appropriately
                        if (null != id) {
                            log.debug("Setting id to '" + id + "' of type "
                                    + idType);
                            try {
                                final Object idValue = ConvertUtils.convert(id,
                                        Class.forName(idType));
                                PropertyUtils.setSimpleProperty(result, idField,
                                        idValue);
                            } catch (final ClassNotFoundException e) {
                                log
                                        .warn("Id type was not a class that could be found: "
                                                + idType);
                            }
                        } else {
                            log.warn("Id value was null.");
                        }
                    } else {
                        final GenericResult gr = new GenericResult();
                        gr.setId(id);
                        gr.setType(className);
                        result = gr;
                    }

                } catch (final Exception e) {
                    throw new SearchException(
                            "Could not reconstitute resultant object.", e);
                }

                result.setRanking(i);

                resultSet.add(result);
            }
            
            return resultSet;
        } catch (final JDOMException e) {
            throw new IndexingException(e);
        } catch (final IOException e) {
            throw new IndexingException(e);
        }
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        SolrSearcher searcher = new SolrSearcher();
        searcher.setHttpClient(new HttpClient());
        ResultSet results = searcher.search("wisdm");
        System.out.println(results.getResults());
    }

    /**
     * Provide an HttpClient to use for making requests.
     * 
     * @param httpClient
     */
    public void setHttpClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Set the hostname of the Solr server.
     * 
     * @param solrHost
     *            Solr hostname.
     */
    public void setSolrHost(final String solrHost) {
        this.solrHost = solrHost;
    }

    /**
     * Set the path on the Solr server to use.
     * 
     * @param solrPath
     *            Solr URL.
     */
    public void setSolrPath(final String solrPath) {
        this.solrPath = solrPath;
    }

    /**
     * Set the port to access the Solr server on.
     * 
     * @param solrPort
     *            Solr port.
     */
    public void setSolrPort(final int solrPort) {
        this.solrPort = solrPort;
    }
    
    private HttpClient getHttpClient() throws URIException {
        final HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(new URI("http", null, solrHost, solrPort, solrPath));
        httpClient.setHostConfiguration(hostConfig);
        return httpClient;
    }
}
