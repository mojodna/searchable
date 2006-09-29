/*
 Copyright 2005-2006 Seth Fitzsimmons <seth@mojodna.net>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package net.mojodna.searchable.solr;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.util.Stack;

import net.mojodna.searchable.AbstractBeanIndexer;
import net.mojodna.searchable.BatchIndexer;
import net.mojodna.searchable.IndexException;
import net.mojodna.searchable.IndexingException;
import net.mojodna.searchable.Searchable;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Processes Searchable beans and passes them to Solr for indexing.
 * 
 * @author Seth Fitzsimmons
 */
public class SolrIndexer extends AbstractBeanIndexer implements
		BatchIndexer<Searchable> {
	private static final Logger log = Logger.getLogger(SolrIndexer.class);

	private HttpClient httpClient;

	private String solrHost = "localhost";

	private String solrPath = "/solr/update";

	private int solrPort = 8983;

	public Document add(final Searchable bean) throws IndexException {
		return doAdd(bean);
	}

	/**
	 * Prevent _sortable-* fields from being created.
	 */
	@Override
	protected Document addSortableFields(final Document doc,
			final Searchable bean, final PropertyDescriptor descriptor,
			final Stack<String> stack) {
		return doc;
	}

	/**
	 * Commit pending documents to the index.
	 * @throws IOException
	 */
	protected void commit() throws IOException {
		final PostMethod post = new PostMethod(solrPath);
		post.setRequestEntity(new StringRequestEntity("<commit/>", "text/xml",
				"UTF-8"));
		log.debug("Committing.");
		getHttpClient().executeMethod(post);
	}

	public void delete(final Searchable bean) throws IndexException {
		doDelete(bean);
	}

	@Override
	protected void delete(final Serializable key) throws IndexingException {
		final Element delete = new Element("delete").addContent(new Element(
				"id").addContent(key.toString()));

		// now do something with the delete block
		try {
			final XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
			final String deleteString = out.outputString(delete);
			final PostMethod post = new PostMethod(solrPath);
			post.setRequestEntity(new StringRequestEntity(deleteString,
					"text/xml", "UTF-8"));
			log.debug("Deleting:\n" + deleteString);
			getHttpClient().executeMethod(post);

			if (!isBatchMode()) {
				commit();
			}
		} catch (final IOException e) {
			throw new IndexingException(e);
		}
	}

	public void flush() throws IndexingException {
		try {
			commit();
		} catch (IOException e) {
			throw new IndexingException(e);
		}
	}

	private HttpClient getHttpClient() throws URIException {
		final HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost(new URI("http", null, solrHost, solrPort, solrPath));
		httpClient.setHostConfiguration(hostConfig);
		return httpClient;
	}

	@Override
	public void optimize() throws IndexingException {
		try {
			PostMethod post = new PostMethod(solrPath);
			post.setRequestEntity(new StringRequestEntity("<optimize/>",
					"text/xml", "UTF-8"));
			log.debug("Optimizing.");
			getHttpClient().executeMethod(post);
		} catch (final IOException e) {
			throw new IndexingException(e);
		}
	}

	/**
	 * Serialize the Document and hand it to Solr.
	 */
	@Override
	protected void save(final Document doc) throws IndexingException {
		final Element add = new Element("add");
		add.addContent(DocumentConverter.convert(doc));

		// now do something with the add block
		try {
			final XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
			final String addString = out.outputString(add);
			final PostMethod post = new PostMethod(solrPath);
			post.setRequestEntity(new StringRequestEntity(addString,
					"text/xml", "UTF-8"));
			log.debug("Adding:\n" + addString);
			getHttpClient().executeMethod(post);

			if (!isBatchMode()) {
				commit();
			}
		} catch (final IOException e) {
			throw new IndexingException(e);
		}
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
	 * @param solrHost Solr hostname.
	 */
	public void setSolrHost(final String solrHost) {
		this.solrHost = solrHost;
	}

	/**
	 * Set the path on the Solr server to use.
	 * 
	 * @param solrPath Solr URL.
	 */
	public void setSolrPath(final String solrPath) {
		this.solrPath = solrPath;
	}

	/**
	 * Set the port to access the Solr server on.
	 * 
	 * @param solrPort Solr port.
	 */
	public void setSolrPort(final int solrPort) {
		this.solrPort = solrPort;
	}
}
