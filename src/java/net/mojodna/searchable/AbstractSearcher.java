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

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.mojodna.searchable.converter.UUIDConverter;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.highlight.QueryHighlightExtractor;

/**
 * Core methods for searching an index.  This is intended to be subclassed by
 * a model-specific implementation that provides appropriate search() methods.
 * This contains convenience methods for searching and excerpting (with
 * highlights).
 * 
 * @see org.apache.lucene.search.highlight.QueryHighlightExtractor
 * 
 * @author Seth Fitzsimmons
 */
public abstract class AbstractSearcher extends IndexSupport {
    private static final Logger log = Logger.getLogger( AbstractSearcher.class );
    
    /** Default fragment size for highlighting */
    public static final int DEFAULT_HIGHLIGHT_FRAGMENT_SIZE_IN_BYTES = 60;
    /** Default max number of fragments required for highlighting */
    public static final int DEFAULT_HIGHLIGHTER_MAX_NUM_FRAGMENTS_REQUIRED = 4;
    /** Default fragment separator */
    public static final String DEFAULT_HIGHLIGHT_FRAGMENT_SEPARATOR = "&#8230;";
    /** Default text to place at the beginning of the highlighted region */
    public static final String DEFAULT_HIGHLIGHT_OPEN = "<strong>";
    /** Default text to place at the end of the highlighted region */
    public static final String DEFAULT_HIGHLIGHT_CLOSE = "</strong>";
    
    private int highlightFragmentSize = DEFAULT_HIGHLIGHT_FRAGMENT_SIZE_IN_BYTES;
    private int highlightMaxNumFragmentsRequired = DEFAULT_HIGHLIGHTER_MAX_NUM_FRAGMENTS_REQUIRED;
    private String highlightFragmentSeparator = DEFAULT_HIGHLIGHT_FRAGMENT_SEPARATOR;
    private String highlightOpen = DEFAULT_HIGHLIGHT_OPEN;
    private String highlightClose = DEFAULT_HIGHLIGHT_CLOSE;
    
    /**
     * Static initialization.
     */
    static {
        // register the UUID converter with ConvertUtils if one has not already
        // been registered
        if ( null == ConvertUtils.lookup( UUID.class ) ) {
            ConvertUtils.register( new UUIDConverter(), UUID.class );
        }
    }
    
    /**
     * Gets the text to place at the end of the highlighted region.
     * 
     * @return Text to place at the end of the highlighted region.
     */
    public String getHighlightClose() {
        return highlightClose;
    }

    /**
     * Sets the text to place at the end of the highlighted region.
     * 
     * @param highlightClose Text to place at the end of the highlighted region.
     */
    public void setHighlightClose(final String highlightClose) {
        this.highlightClose = highlightClose;
    }

    /**
     * Gets the text to use between fragments.
     * 
     * @return Text to use between fragments.
     */
    public String getHighlightFragmentSeparator() {
        return highlightFragmentSeparator;
    }

    /**
     * Sets the text to use between fragments.
     * 
     * @param highlightFragmentSeparator Text to use between fragments.
     */
    public void setHighlightFragmentSeparator(final String highlightFragmentSeparator) {
        this.highlightFragmentSeparator = highlightFragmentSeparator;
    }

    /**
     * Gets the max size (in bytes) of fragments to extract.
     * 
     * @return Max size (in bytes) of fragments to extract.
     */
    public int getHighlightFragmentSize() {
        return highlightFragmentSize;
    }

    /**
     * Sets the max size (in bytes) of fragments to extract.
     * 
     * @param highlightFragmentSize Max size (in bytes) of fragments to extract.
     */
    public void setHighlightFragmentSize(final int highlightFragmentSize) {
        this.highlightFragmentSize = highlightFragmentSize;
    }

    /**
     * Gets the max number of fragments to display.
     * 
     * @return Max number of fragments to display.
     */
    public int getHighlightMaxNumFragmentsRequired() {
        return highlightMaxNumFragmentsRequired;
    }

    /**
     * Sets the max number of fragments to display.
     * 
     * @param highlightMaxNumFragmentsRequired Max number of fragments to display.
     */
    public void setHighlightMaxNumFragmentsRequired(final int highlightMaxNumFragmentsRequired) {
        this.highlightMaxNumFragmentsRequired = highlightMaxNumFragmentsRequired;
    }

    /**
     * Gets the text to place at the beginning of the highlighted region.
     * 
     * @return Text to place at the beginning of the highlighted region.
     */
    public String getHighlightOpen() {
        return highlightOpen;
    }

    /**
     * Sets the text to place at the beginning of the highlighted region.
     * 
     * @param highlightOpen Text to place at the beginning of the highlighted region.
     */
    public void setHighlightOpen(final String highlightOpen) {
        this.highlightOpen = highlightOpen;
    }

    /**
     * Searches for a suitable property to use as an id.  Uses the first
     * property annotated with Searchable.ID.  If none are available, it
     * falls back to the "id" field (if present).
     * 
     * Any properties used as ids must be Serializable.
     * 
     * This is a convenience method for indexes of Searchables and will be of
     * no use when they are not in use.
     * 
     * @see AbstractBeanIndexer#getId(Searchable)
     * 
     * @param bean Object to reflect on.
     * @throws SearchException
     */
    protected Serializable getId(final Searchable bean) throws SearchException {
        try {
            final Object id = PropertyUtils.getProperty( bean, SearchableBeanUtils.getIdPropertyName( bean.getClass() ) );
            if ( id instanceof Serializable ) {
                return (Serializable) id;
            } else {
                log.error("The id property for " + bean.getClass() + " must be Serializable.");
                throw new SearchException("Id properties must be Serializable.");
            }
        } catch (final Exception e) {
            throw new SearchException("Unable to determine value for id.", e );
        }
    }

    /** Convenience methods for searching. */
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @return ResultSet containing results.
     */
    protected ResultSet doSearch(final Query query) throws IndexException {
        return doSearch( query, 0, null );
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final Query query, final Integer offset, final Integer count) throws IndexException {
        return doSearch( query, offset, count, Sort.RELEVANCE );
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @param sortField Field to sort by.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    // TODO add support for String[] sortFields
    protected ResultSet doSearch(final Query query, final Integer offset, final Integer count, final String sortField) throws IndexException {
        Sort sort = Sort.RELEVANCE;
        if ( StringUtils.isNotBlank( sortField )) 
            sort = new Sort( IndexSupport.SORTABLE_PREFIX + sortField );
        return doSearch( query, offset, count, sort );
    }

    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @param sortField Field to sort by.
     * @param reverse Whether to reverse the resultset. 
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final Query query, final Integer offset, final Integer count, final String sortField, final boolean reverse) throws IndexException {
        Sort sort = Sort.RELEVANCE;
        if ( StringUtils.isNotBlank( sortField )) 
            sort = new Sort( IndexSupport.SORTABLE_PREFIX + sortField, reverse );

        return doSearch( query, offset, count, sort );
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @param sort Sort to use.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final Query query, final Integer offset, final Integer count, final Sort sort) throws IndexException {
        try {
            return doSearch( query, getIndexSearcher(), offset, count, sort );
        }
        catch (final IOException e) {
            throw new SearchException( e );
        }
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param searcher Lucene Searcher to perform the search with.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @param sort Sort to use.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final Query query, final Searcher searcher, final Integer offset, final Integer count, final Sort sort) throws SearchException, IOException {
    	// execute the search
    	log.debug("Searching with query: " + query.toString() );
    	final Hits hits = searcher.search(query, sort);

    	// create a container for results
    	final List<Result> results = new LinkedList<Result>();

    	// instantiate and initialize the ResultSet
    	final ResultSetImpl rs = new ResultSetImpl( hits.length() );
    	rs.setQuery( query );

    	final int numResults;
    	if ( null != count )
    		numResults = Math.min( offset + count, hits.length() );
    	else
    		numResults = hits.length();

    	rs.setOffset( offset );

    	// loop through results starting at offset and stopping after numResults
    	for (int i = offset; i < numResults; i++) {
    		final Document doc = hits.doc(i);
    		Result result = null;

    		// load the class name
    		final String className = doc.get( TYPE_FIELD_NAME );
    		try {
    			// attempt to instantiate an instance of the specified class
    			try {
    				if ( null != className ) {
    					final Object o = Class.forName( className ).newInstance();
    					if ( o instanceof Result ) {
    						log.debug("Created new instance of: " + className);
    						result = (Result) o;
    					}
    				}
    			}
    			catch (final ClassNotFoundException e) {
    				// class was invalid, or something
    			}

    			// fall back to a GenericResult as a container
    			if ( null == result )
    				result = new GenericResult();

    			if ( result instanceof Searchable ) {
    				// special handling for searchables
    				final String idField = SearchableBeanUtils.getIdPropertyName( ((Searchable) result).getClass() );

    				// attempt to load the id and set the id property on the Searchable appropriately
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

    			// load stored fields and put them in the Result
    			final Map<String,String> storedFields = new HashMap<String,String>();
    			final Enumeration fields = doc.fields();
    			while (fields.hasMoreElements()) {
    				final Field f = (Field) fields.nextElement();
    				// exclude private fields
    				if ( !PRIVATE_FIELD_NAMES.contains( f.name() ) && !f.name().startsWith( IndexSupport.SORTABLE_PREFIX ) )
    					storedFields.put( f.name(), f.stringValue() );
    			}
    			result.setStoredFields( storedFields );
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
    
    /**
     * Set the searchExtract property on a result if an excerpt can be found.
     * 
     * @param query Query that was used to find this result.
     * @param result Result to load searchExtract for
     * @return Result with searchExtract loaded.
     * @throws SearchException
     */
    protected Result doExcerpt(final Query query, final Result result) throws SearchException {
        final String excerptProperty = SearchableBeanUtils.getExcerptPropertyName( result.getClass() );
        if ( null != excerptProperty ) {
            try {
                final QueryHighlightExtractor highlighter = new QueryHighlightExtractor( query, getAnalyzer(), getHighlightOpen(), getHighlightClose());
                
                final Object body = PropertyUtils.getProperty( result, excerptProperty );
                if ( null != body ) {
                    final String extract = highlighter.getBestFragments( body.toString(), getHighlightFragmentSize(), getHighlightMaxNumFragmentsRequired(), getHighlightFragmentSeparator());
                    result.setSearchExtract( extract );
                }
            }
            catch (final Exception e) {
                throw new SearchException( e );
            }
        }

        return result;
    }

    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @return ResultSet containing results.
     */
    protected ResultSet doSearch(final String query) throws IndexException {
        return doSearch( query, 0, null );
    }

    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param clazz Type of object being searched for.
     * @return ResultSet containing results.
     */
    protected ResultSet doSearch(final String query, final Class<? extends Searchable> clazz) throws IndexException {
        return doSearch( query, clazz, 0, null );
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param sortField Field to sort by.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final String query, final String sortField) throws IndexException {
        return doSearch( query, 0, null, sortField );
    }

    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param clazz Type of object being searched for.
     * @param sortField Field to sort by.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final String query, final Class<? extends Searchable> clazz, final String sortField) throws IndexException {
        return doSearch( query, clazz, 0, null, sortField );
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param sort Sort to use.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final String query, final Sort sort) throws IndexException {
        return doSearch( query, 0, null, sort );
    }

    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param clazz Type of object being searched for.
     * @param sort Sort to use.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final String query, final Class<? extends Searchable> clazz, final Sort sort) throws IndexException {
        return doSearch( query, clazz, 0, null, sort );
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final String query, final Integer offset, final Integer count)  throws IndexException {
        return doSearch( query, offset, count, Sort.RELEVANCE );
    }

    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param clazz Type of object being searched for.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final String query, final Class<? extends Searchable> clazz, final Integer offset, final Integer count)  throws IndexException {
        return doSearch( query, clazz, offset, count, Sort.RELEVANCE );
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @param sortField Field to sort by.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final String query, final Integer offset, final Integer count, final String sortField)  throws IndexException {
        return doSearch( query, offset, count, sortField, false );
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param clazz Type of object being searched for.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @param sortField Field to sort by.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final String query, final Class<? extends Searchable> clazz, final Integer offset, final Integer count, final String sortField)  throws IndexException {
        return doSearch( query, clazz, offset, count, sortField, false );
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @param sortField Field to sort by.
     * @param reverse Whether to reverse the resultset. 
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final String query, final Integer offset, final Integer count, final String sortField, final boolean reverse)  throws IndexException {
        return doSearch( query, offset, count, new Sort( sortField, reverse ) );
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param clazz Type of object being searched for.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @param sortField Field to sort by.
     * @param reverse Whether to reverse the resultset. 
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final String query, final Class<? extends Searchable> clazz, final Integer offset, final Integer count, final String sortField, final boolean reverse)  throws IndexException {
        return doSearch( query, clazz, offset, count, new Sort( sortField, reverse ) );
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param query Query to use.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @param sort Sort to use.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final String query, final Integer offset, final Integer count, final Sort sort)  throws IndexException {
        return doSearch( query, null, offset, count, sort );
    }
    
    /**
     * Search the index with the specified query.
     * 
     * @param _query Query to use.
     * @param clazz Type of object being searched for.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @param sort Sort to use.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    protected ResultSet doSearch(final String _query, final Class<? extends Searchable> clazz, final Integer offset, final Integer count, final Sort sort)  throws IndexException {
        String[] fields = new String[0];
        if ( null != clazz )
            fields = SearchableBeanUtils.getDefaultFieldNames( clazz );
        if ( null == fields || fields.length == 0 )
            fields = getFieldsPresent();
        
        log.debug("Fields being searched: " + Arrays.asList( fields ) );
        
        final Query query = prepareQuery( _query, fields );
        final ResultSet results = doSearch( query, offset, count, sort );
        
        log.debug("Found " + results.size() + " document(s) that matched query '" + _query + "':");
        
        return results;
    }
    
    /**
     * Prepare a query against a set of default fields.
     * 
     * @param query String representation of query.
     * @param defaultFields Default fields to search against.
     * @return Lucene query representation.
     * @throws SearchException
     */
    protected Query prepareQuery(final String query, final String[] defaultFields) throws SearchException {
    	final MultiFieldQueryParser mfp = new MultiFieldQueryParser( defaultFields, getAnalyzer() );
    	mfp.setDefaultOperator( MultiFieldQueryParser.AND_OPERATOR );
    	try {
    		return mfp.parse( query );
    	}
    	catch (final ParseException e) {
    		throw new SearchException("Unable to prepare query.", e);
    	}
    }
    
    /**
     * Is the specified field present in the index?
     * 
     * @param field Field to search for.
     * @return Whether the specified field is present in the index.
     * @throws SearchException
     */
    protected boolean isFieldPresent(final String field) throws IndexException {
        try {
            return isFieldPresent( field, getIndexReader() );
        }
        catch (final IOException e) {
            throw new SearchException( e );
        }
    }
    
    /**
     * Is the specified field present in the index?
     * 
     * @param field Field to search for.
     * @param reader IndexReader to use to obtain fields.
     * @return Whether the specified field is present in the index.
     * @throws SearchException
     */
    protected boolean isFieldPresent(final String field, final IndexReader reader) throws SearchException, IOException {
    	return reader.getFieldNames( true ).contains( field );
    }
    
    /**
     * Gets a Collection of all fields present in the index.
     * 
     * @return Collection of field names.
     * @throws SearchException
     */
    protected String[] getFieldsPresent() throws IndexException {
        try {
            return getFieldsPresent( getIndexReader() );
        }
        catch (final IOException e) {
            throw new SearchException( e );
        }
    }
    
    /**
     * Gets a list of all fields present in the index.
     * 
     * @param reader IndexReader to use to obtain fields.
     * @return Array of field names.
     * @throws SearchException
     */
    protected String[] getFieldsPresent(final IndexReader reader) throws SearchException, IOException {
    	return SearchableUtils.toStringArray( CollectionUtils.subtract( reader.getFieldNames( true ), IndexSupport.PRIVATE_FIELD_NAMES ) );
    }
    
    /**
     * Loads a document from the index.
     * 
     * @param id Document id.
     * @return Document.
     * @throws SearchException
     */
    protected Document getDocument(final int id) throws IndexException {
        try {
            return getIndexReader().document( id );
        }
        catch (final IOException e) {
            throw new SearchException( e );
        }
    }
}
