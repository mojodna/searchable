/*
Copyright 2005      Seth Fitzsimmons <seth@prx.org>

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
package net.mojodna.searchable;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.mojodna.searchable.converter.UUIDConverter;
import net.mojodna.searchable.example.SearchableBean;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.store.RAMDirectory;

public class Searcher extends IndexSupport {
    private static final Logger log = Logger.getLogger( Searcher.class );
    
    static {
        if ( null == ConvertUtils.lookup( UUID.class ) ) {
            ConvertUtils.register( new UUIDConverter(), UUID.class );
        }
    }
    
    public Searcher() throws IndexException {
        super();
    }
    
    public ResultSet search(final Query query) throws SearchException {
        return search( query, 0, null );
    }
    
    public ResultSet search(final Query query, final Integer offset, final Integer count) throws SearchException {
        return search( query, offset, count, Sort.RELEVANCE );
    }
    
    // TODO add support for String[] sortFields
    public ResultSet search(final Query query, final Integer offset, final Integer count, final String sortField) throws SearchException {
        Sort sort = Sort.RELEVANCE;
        if ( StringUtils.isNotBlank( sortField )) 
            sort = new Sort( IndexSupport.SORTABLE_PREFIX + sortField );
        return search( query, offset, count, sort );
    }

    public ResultSet search(final Query query, final Integer offset, final Integer count, final String sortField, final boolean reverse) throws SearchException {
        Sort sort = Sort.RELEVANCE;
        if ( StringUtils.isNotBlank( sortField )) 
            sort = new Sort( IndexSupport.SORTABLE_PREFIX + sortField, reverse );

        return search( query, offset, count, sort );
    }
    
    protected ResultSet search(final Query query, final Integer offset, final Integer count, final Sort sort) throws SearchException {
        IndexSearcher searcher = null;
        try {
            searcher = new IndexSearcher( new RAMDirectory( getIndexDirectory() ) );

            final Hits hits = searcher.search(query, sort);

            final List<Result> results = new LinkedList<Result>();
            final ResultSetImpl rs = new ResultSetImpl( hits.length() );

            final int numResults;
            if ( null != count )
                numResults = Math.min( offset + count, hits.length() );
            else
                numResults = hits.length();
            
            rs.setOffset( offset );
            
            for (int i = offset; i < numResults; i++) {
                final Document doc = hits.doc(i);
                Result result = null;
                
                final String className = doc.get( TYPE_FIELD_NAME );
                try {
                    try {
                        if ( null != className ) {
                            final Object o = Class.forName( className ).newInstance();
                            if ( o instanceof Result ) {
                                log.debug("Created new instance of: " + className);
                                result = (Result) o;
                            } else {
                                result = new GenericResult();
                            }
                        } else {
                            result = new GenericResult();
                        }
                    }
                    catch (final ClassNotFoundException e) {
                        result = new GenericResult();
                    }
                    if ( result instanceof Searchable ) {
                        // special handling for searchables
                        final String idField = SearchableBeanUtils.getIdPropertyName( (Searchable) result );
                        
                        final Map storedFields = new HashMap();
                        final Enumeration fields = doc.fields();
                        while (fields.hasMoreElements()) {
                            final Field f = (Field) fields.nextElement();
                            // exclude private fields
                            if ( !PRIVATE_FIELD_NAMES.contains( f.name() ) && !f.name().startsWith( IndexSupport.SORTABLE_PREFIX ) )
                                storedFields.put( f.name(), f.stringValue() );
                        }
                        result.setStoredFields( storedFields );

                        final String id = doc.get( ID_FIELD_NAME );
                        final Field idClass = doc.getField( ID_TYPE_FIELD_NAME );
                        if ( null != id ) {
                            log.debug("Setting id to '" + id + "' of type " + idClass.stringValue() );
                            try {
                                final Object idValue = ConvertUtils.convert( id, Class.forName( idClass.stringValue() ) );
                                PropertyUtils.setSimpleProperty(result, idField, idValue );
                            }
                            catch (final ClassNotFoundException e) {
                                log.warn("Id type was not a class that could be found: " + idClass.stringValue() );
                            }
                        } else {
                            log.warn("Id value was null.");
                        }
                    } else {
                        final GenericResult gr = new GenericResult();
                        gr.setId( doc.get( ID_FIELD_NAME ) );
                        gr.setType( doc.get( TYPE_FIELD_NAME ) );
                        result = gr;
                    }
                }
                catch (final Exception e) {
                    throw new SearchException("Could not reconstitute resultant object.", e );
                }
                
                result.setRanking( i );
                result.setScore( hits.score( i ) );
                
                results.add( result );
            }

            rs.setResults( results );
            return rs;
        }
        catch (final IOException e) {
            throw new SearchException( e );
        }
        finally {
            try {
            if ( null != searcher )
                searcher.close();
            }
            catch (final IOException e) {
                throw new SearchException("Unable to close searcher.", e );
            }
        }
    }
    
    /**
     * Search query interface.
     */
    public ResultSet search(final String query) throws SearchException {
        return search( query, 0, null, null );
    }

    public ResultSet search(final String query, final String sortField) throws SearchException {
        return search( query, 0, null, sortField );
    }
    
    public ResultSet search(final String query, final Integer offset, final Integer count)  throws SearchException {
        return search( query, offset, count, null );
    }
    
    public ResultSet search(final String query, final Integer offset, final Integer count, final String sortField)  throws SearchException {
        return search( query, offset, count, sortField, false );
    }
    
    public ResultSet search(final String _query, final Integer offset, final Integer count, final String sortField, final boolean reverse)  throws SearchException {
        // TODO attempt to load the list of default fields via annotations
        try {
            // "description" as a default field means little here
            final Query query = QueryParser.parse(_query, "description", getAnalyzer() );
            final ResultSet results = search( query, offset, count, sortField, reverse );
            
            log.debug("Found " + results.size() + " document(s) that matched query '" + _query + "':");
            
            return results;
        }
        catch (final ParseException e) {
            throw new SearchException( e );
        }
    }
    
    protected boolean isFieldPresent(final String field) throws IndexException {
        IndexReader reader = null;
        try {
            reader = IndexReader.open( getIndexDirectory() );
            return reader.getFieldNames( true ).contains( field );
        }
        catch (final IOException e) {
            throw new IndexException( e );
        }
        finally {
            try {
            if ( null != reader )
                reader.close();
            }
            catch (final IOException e) {
                throw new IndexException("Unable to close reader.", e );
            }
        }
    }
    
    protected Document getDocument(final int id) throws IndexException {
        IndexReader reader = null;
        try {
            reader = IndexReader.open( getIndexDirectory() );
            return reader.document( id );
        }
        catch (final IOException e) {
            throw new IndexException( e );
        }
        finally {
            try {
            if ( null != reader )
                reader.close();
            }
            catch (final IOException e) {
                throw new IndexException("Unable to close reader.", e );
            }
        }
    }
    
    public static void main(final String[] args) throws Exception {
        final Searcher s = new Searcher();
        // final List<Result> results = s.search("bio:kayak");
        final ResultSet<Result> results = s.search("green:mold", 0, 2, "name", true);
        log.debug( results );
        for ( final Result result : results ) {
            log.debug("Score: " + result.getScore() );
            log.debug("Stored fields: " + result.getStoredFields() );
            // to test UUIDConverter
            log.debug("UUID: " + ((SearchableBean) result).getUUID() );
        }
    }
}
