/*
Copyright 2005-2006 Seth Fitzsimmons <seth@note.amherst.edu>

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

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.mojodna.searchable.converter.UUIDConverter;
import net.mojodna.searchable.util.AnnotationUtils;
import net.mojodna.searchable.util.MultiFieldQueryPreparer;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.highlight.QueryHighlightExtractor;
import org.apache.lucene.store.RAMDirectory;

public abstract class AbstractSearcher extends IndexSupport {
    public static final int DEFAULT_HIGHLIGHT_FRAGMENT_SIZE_IN_BYTES = 60;
    public static final int DEFAULT_MAX_NUM_FRAGMENTS_REQUIRED = 4;
    public static final String DEFAULT_FRAGMENT_SEPARATOR = "&#8230;";
    public static final String DEFAULT_HIGHLIGHT_OPEN = "<strong>";
    public static final String DEFAULT_HIGHLIGHT_CLOSE = "</strong>";
    
    private static final Logger log = Logger.getLogger( AbstractSearcher.class );
    
    static {
        if ( null == ConvertUtils.lookup( UUID.class ) ) {
            ConvertUtils.register( new UUIDConverter(), UUID.class );
        }
    }
    
    public AbstractSearcher() throws IndexException {
        super();
    }
    
    protected ResultSet doSearch(final Query query) throws SearchException {
        return doSearch( query, 0, null );
    }
    
    protected ResultSet doSearch(final Query query, final Integer offset, final Integer count) throws SearchException {
        return doSearch( query, offset, count, Sort.RELEVANCE );
    }
    
    // TODO add support for String[] sortFields
    protected ResultSet doSearch(final Query query, final Integer offset, final Integer count, final String sortField) throws SearchException {
        Sort sort = Sort.RELEVANCE;
        if ( StringUtils.isNotBlank( sortField )) 
            sort = new Sort( IndexSupport.SORTABLE_PREFIX + sortField );
        return doSearch( query, offset, count, sort );
    }

    protected ResultSet doSearch(final Query query, final Integer offset, final Integer count, final String sortField, final boolean reverse) throws SearchException {
        Sort sort = Sort.RELEVANCE;
        if ( StringUtils.isNotBlank( sortField )) 
            sort = new Sort( IndexSupport.SORTABLE_PREFIX + sortField, reverse );

        return doSearch( query, offset, count, sort );
    }
    
    protected ResultSet doSearch(final Query query, final Integer offset, final Integer count, final Sort sort) throws SearchException {
        IndexSearcher searcher = null;
        try {
            searcher = new IndexSearcher( new RAMDirectory( getIndexDirectory() ) );

            log.debug("Searching with query: " + query.toString() );
            final Hits hits = searcher.search(query, sort);

            final List<Result> results = new LinkedList<Result>();
            final ResultSetImpl rs = new ResultSetImpl( hits.length() );
            rs.setQuery( query );

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
     * Create excerpts using properties specified as @Excerptable.
     * 
     * Does not use generics due to casting by subclasses.
     * 
     * @param results Results to excerpt.
     * @return ResultSet containing excerpted entries.
     * @throws SearchException
     */
    protected ResultSet excerpt(final ResultSet results) throws SearchException {
        for (final Object r : results.getResults() ) {
            doExcerpt( results.getQuery(), (Result) r );
        }
        return results;
    }
    
    protected Result doExcerpt(final Query query, final Result result) throws SearchException {
        final String excerptProperty = getExcerptProperty( result.getClass() );
        if ( null != excerptProperty ) {
            try {
                final QueryHighlightExtractor highlighter = new QueryHighlightExtractor( query, getAnalyzer(), DEFAULT_HIGHLIGHT_OPEN, DEFAULT_HIGHLIGHT_CLOSE);
                
                final Object body = PropertyUtils.getProperty( result, excerptProperty );
                final String extract = highlighter.getBestFragments( body.toString(), DEFAULT_HIGHLIGHT_FRAGMENT_SIZE_IN_BYTES, DEFAULT_MAX_NUM_FRAGMENTS_REQUIRED, DEFAULT_FRAGMENT_SEPARATOR);
                result.setSearchExtract( extract );
            }
            catch (final Exception e) {
                throw new SearchException( e );
            }
        }

        return result;
    }
    
    /**
     * Search query interface.
     */
    protected ResultSet doSearch(final String query) throws SearchException {
        return doSearch( query, 0, null );
    }

    protected ResultSet doSearch(final String query, final Class<? extends Searchable> clazz) throws SearchException {
        return doSearch( query, clazz, 0, null );
    }
    
    protected ResultSet doSearch(final String query, final String sortField) throws SearchException {
        return doSearch( query, 0, null, sortField );
    }

    protected ResultSet doSearch(final String query, final Class<? extends Searchable> clazz, final String sortField) throws SearchException {
        return doSearch( query, clazz, 0, null, sortField );
    }
    
    protected ResultSet doSearch(final String query, final Sort sort) throws SearchException {
        return doSearch( query, 0, null, sort );
    }

    protected ResultSet doSearch(final String query, final Class<? extends Searchable> clazz, final Sort sort) throws SearchException {
        return doSearch( query, clazz, 0, null, sort );
    }
    
    protected ResultSet doSearch(final String query, final Integer offset, final Integer count)  throws SearchException {
        return doSearch( query, offset, count, Sort.RELEVANCE );
    }

    protected ResultSet doSearch(final String query, final Class<? extends Searchable> clazz, final Integer offset, final Integer count)  throws SearchException {
        return doSearch( query, clazz, offset, count, Sort.RELEVANCE );
    }
    
    protected ResultSet doSearch(final String query, final Integer offset, final Integer count, final String sortField)  throws SearchException {
        return doSearch( query, offset, count, sortField, false );
    }
    
    protected ResultSet doSearch(final String query, final Class<? extends Searchable> clazz, final Integer offset, final Integer count, final String sortField)  throws SearchException {
        return doSearch( query, clazz, offset, count, sortField, false );
    }
    
    protected ResultSet doSearch(final String query, final Integer offset, final Integer count, final String sortField, final boolean reverse)  throws SearchException {
        return doSearch( query, offset, count, new Sort( sortField, reverse ) );
    }
    
    protected ResultSet doSearch(final String query, final Class<? extends Searchable> clazz, final Integer offset, final Integer count, final String sortField, final boolean reverse)  throws SearchException {
        return doSearch( query, clazz, offset, count, new Sort( sortField, reverse ) );
    }
    
    protected ResultSet doSearch(final String query, final Integer offset, final Integer count, final Sort sort)  throws SearchException {
        return doSearch( query, null, offset, count, sort );
    }
    
    protected ResultSet doSearch(final String _query, final Class<? extends Searchable> clazz, final Integer offset, final Integer count, final Sort sort)  throws SearchException {
        Collection<String> fields = Collections.EMPTY_LIST;
        if ( null != clazz )
            fields = getDefaultFields( clazz );
        if ( fields.isEmpty() )
            fields = getFieldsPresent();
        
        log.debug("Fields being searched: " + fields);

        final Query query = MultiFieldQueryPreparer.prepareQuery( _query, (String[]) fields.toArray(), getAnalyzer() );
        final ResultSet results = doSearch( query, offset, count, sort );
        
        log.debug("Found " + results.size() + " document(s) that matched query '" + _query + "':");
        
        return results;
    }
    
    protected boolean isFieldPresent(final String field) throws SearchException {
        IndexReader reader = null;
        try {
            reader = IndexReader.open( getIndexDirectory() );
            return reader.getFieldNames( true ).contains( field );
        }
        catch (final IOException e) {
            throw new SearchException( e );
        }
        finally {
            try {
            if ( null != reader )
                reader.close();
            }
            catch (final IOException e) {
                throw new SearchException("Unable to close reader.", e );
            }
        }
    }
    
    protected Collection<String> getFieldsPresent() throws SearchException {
        IndexReader reader = null;
        try {
            reader = IndexReader.open( getIndexDirectory() );
            return CollectionUtils.subtract( reader.getFieldNames( true ), IndexSupport.PRIVATE_FIELD_NAMES );
        }
        catch (final IOException e) {
            throw new SearchException( e );
        }
        finally {
            try {
            if ( null != reader )
                reader.close();
            }
            catch (final IOException e) {
                throw new SearchException("Unable to close reader.", e );
            }
        }
    }
    
    protected Collection<String> getDefaultFields(final Class<? extends Searchable> clazz) {
        final Searchable.DefaultFields annotation = (Searchable.DefaultFields) AnnotationUtils.getAnnotation( clazz, Searchable.DefaultFields.class );
        if ( null != annotation )
            return Arrays.asList( annotation.value() );
        return Collections.EMPTY_LIST;
    }
    
    protected String getExcerptProperty(final Class<? extends Result> clazz) {
        final PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors( clazz );
        for (final PropertyDescriptor d : descriptors) {
            if ( AnnotationUtils.isAnnotationPresent( d.getReadMethod(), Searchable.Excerptable.class ) )
                return d.getName();
        }
        
        return null;
    }
    
    protected Document getDocument(final int id) throws SearchException {
        IndexReader reader = null;
        try {
            reader = IndexReader.open( getIndexDirectory() );
            return reader.document( id );
        }
        catch (final IOException e) {
            throw new SearchException( e );
        }
        finally {
            try {
            if ( null != reader )
                reader.close();
            }
            catch (final IOException e) {
                throw new SearchException("Unable to close reader.", e );
            }
        }
    }
}
