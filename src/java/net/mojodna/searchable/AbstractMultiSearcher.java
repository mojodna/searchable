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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.mojodna.searchable.Searchable.DefaultFields;
import net.mojodna.searchable.util.AnnotationUtils;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.Sort;

/**
 * Base class for Searchers that search across multiple indexes.
 * 
 * @author Seth Fitzsimmons
 */
public abstract class AbstractMultiSearcher extends AbstractSearcher {
    private String[] indexPaths;
    private Class[] classes;
    
    /**
     * Constructor.  Use this when you want to search indexes that contain
     * arbitrary content.  By default, this will search across all fields in
     * all indexes.
     * 
     * @param indexPaths Index paths to search.
     * @throws IndexException
     */
    // TODO add constructor with default fields to use
    public AbstractMultiSearcher(final String[] indexPaths) throws IndexException {
        super();
        this.indexPaths = indexPaths;
    }
    
    /**
     * Constructor.  Use this when you want to search indexes that may contain
     * Searchables.  This will use default fields specified by the @DefaultFields
     * annotation and fall back to searching against fields present in a given index.
     * 
     * @param indexPaths Index paths to search.
     * @param classes Classes being searched for (used for dynamically determining
     * default fields.
     * @throws IndexException
     */
    public AbstractMultiSearcher(final String[] indexPaths, final Class[] classes) throws IndexException {
        super();

        if ( indexPaths.length != classes.length )
            throw new IllegalArgumentException("Array lengths must be equal.");
        
        this.indexPaths = indexPaths;
        this.classes = classes;
    }
    
    /**
     * Search the index with the specified query.  Overrides AbstractSearcher's
     * default behavior.
     * 
     * @param query Query to use.
     * @param offset Offset to begin result set at.
     * @param count Number of results to return.
     * @param sort Sort to use.
     * @return ResultSet containing results.
     * @throws SearchException
     */
    @Override
    public ResultSet doSearch(final String query, final Integer offset, final Integer count, final Sort sort) throws SearchException {
        MultiReader mr = null;
        MultiSearcher ms = null;
        
        try {
            // load readers and searchers
            final Map<Class,IndexReader> readerMap = new HashMap();
            final IndexReader[] readers = new IndexReader[ indexPaths.length ];
            final Searchable[] searchers = new Searchable[ indexPaths.length ];
            int i = 0;
            for (final String path : indexPaths ) {
                readers[i] = IndexReader.open( path );
                searchers[i] = new IndexSearcher( readers[i] );
                readerMap.put( classes[i], readers[i] );
                i++;
            }
            
            mr = new MultiReader( readers );
            ms = new MultiSearcher( searchers );
            
            final String[] defaultFields;
            
            if ( null != classes ) {
                final Collection fields = new HashSet();

                for (final Class clazz : classes) {
                    if ( clazz.isInstance( net.mojodna.searchable.Searchable.class ) && AnnotationUtils.isAnnotationPresent( clazz, DefaultFields.class ) ) {
                        // load fields specified in @DefaultFields annotation
                        fields.addAll( Arrays.asList( SearchableBeanUtils.getDefaultFieldNames( clazz ) ) );
                    } else {
                        // load fields present in the index corresponding to this class
                        fields.addAll( Arrays.asList( getFieldsPresent( readerMap.get( clazz ) ) ) );
                    }
                }
                
                defaultFields = SearchableUtils.toStringArray( fields );
            } else {
                // load all fields available from all indexes
                defaultFields = getFieldsPresent( mr );
            }
            
            // prepare the query using available default fields
            final Query q = prepareQuery( query, defaultFields );

            // use the overloaded doSearch method with the MultiSearcher
            // constructed previously
            return doSearch( q, ms, offset, count, sort );
        }
        catch (final IOException e) {
            throw new SearchException( e );
        }
        finally {
            try {
                // attempt to close readers and searchers
                if ( null != mr )
                    mr.close();
                if ( null != ms )
                    ms.close();
            }
            catch (final IOException e) {
                throw new SearchException( e );
            }
        }
    }
}
