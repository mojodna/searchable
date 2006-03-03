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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexModifier;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.TermQuery;

/**
 * Core methods for adding objects to an index.  This is intended to be
 * subclassed by a model-specific implementation that provides appropriate
 * add() methods.
 * 
 * @author Seth Fitzsimmons
 */
public abstract class AbstractIndexer extends IndexSupport {
    private static final Logger log = Logger.getLogger( AbstractIndexer.class );
    
    /** Pending documents to delete. */
    private Collection<Integer> pendingDeletes = new LinkedList<Integer>();
    
    /**
     * Creates a document with searchable-specific properties initialized.
     * 
     * Call this method with appropriate values for type if you wish to index
     * non-Searchables.
     * 
     * @param type Type of object being indexed (usually a fully qualified class name).
     * @param id Id of object being indexed.
     * @return Initialized document.
     */
    protected Document createDocument(final String type, final Object id) {
        log.debug("Creating document with type '" + type + "' and id '" + id + "'.");

        final Document doc = new Document();
        doc.add( Field.Keyword( TYPE_FIELD_NAME, type) );
        doc.add( Field.Keyword( ID_FIELD_NAME, id.toString() ) );
        doc.add( Field.Keyword( ID_TYPE_FIELD_NAME, id.getClass().getName() ) );
        doc.add( Field.Keyword( COMPOUND_ID_FIELD_NAME, type + "-" + id ) );
        return doc;
    }
    
    /**
     * Saves a document to the underlying index.
     * 
     * @param document Document to save.
     * @throws IndexingException
     */
    protected void save(final Document document) throws IndexException {
    	try {
    		save( document, getIndexModifier() );
    	}
    	catch (final IOException e) {
    		log.error("Could not open index: " + e.getMessage(), e);
    		throw new IndexingException( "Unable to commit document to index.", e );
    	}
    }
    
    /**
     * Saves a document to the underlying index.
     * 
     * @param document Document to save.
     * @param writer IndexModifier to use to save the document.
     * @throws IndexingException
     * @throws IOException
     */
    private void save(final Document document, final IndexModifier modifier) throws IndexException, IOException {
    	long begin = System.currentTimeMillis();
        // delete document if necessary
        if ( null != document.get( TYPE_FIELD_NAME ) && null != document.get( ID_FIELD_NAME ) )
            delete( document.get( TYPE_FIELD_NAME ), document.get( ID_FIELD_NAME ) );

        log.debug("Writing document to index.");
        modifier.addDocument( document );
        
        long afterWrite = System.currentTimeMillis();
        
        log.debug("Save took " + (afterWrite - begin) + "ms");
    }
    
    /**
     * Deletes a document from the index.  This constructs a Term corresponding
     * to the compound key.
     * 
     * @param type Type of object that was indexed.
     * @param id Id of document that was indexed.
     * @throws IndexingException
     */
    protected void delete(final String type, final Object id) throws IndexException {
		try {
			if ( isBatchMode() ) {
				final Hits hits = getIndexSearcher().search( new TermQuery( new Term( COMPOUND_ID_FIELD_NAME, type + "-" + id ) ) );
				for (final Iterator i = hits.iterator(); i.hasNext(); ) {
					Hit hit = (Hit) i.next();
					pendingDeletes.add( hit.getId() );
				}
			} else {
				delete( type, id, getIndexModifier() );
			}
		}
		catch (final IOException e) {
			log.error("Could not open index: " + e.getMessage(), e);
			throw new IndexingException( "Unable to delete document.", e );
		}
    }
    
    /**
     * Deletes a document from the index.  This constructs a Term corresponding
     * to the compound key.
     * 
     * @param type Type of object that was indexed.
     * @param id Id of document that was indexed.
     * @param reader IndexReader to use to delete the document.
     * @throws IndexingException
     * @throws IOException
     */
    private void delete(final String type, final Object id, final IndexModifier modifier) throws IndexingException, IOException  {
        log.debug("Deleting document " + type + "-" + id + ".");
        modifier.delete( new Term( COMPOUND_ID_FIELD_NAME, type + "-" + id ) );
    }
    
    /**
     * Flushes pending deletes.
     * 
     * @throws IndexException
     */
    protected void flushDeletes() throws IndexException {
    	try {
    		flushDeletes( getIndexModifier() );
    	}
    	catch (final IOException e) {
    		throw new IndexingException("Unable to flush pending deletes.", e);
    	}
    }
    
    /**
     * Flushes pending deletes.
     * 
     * @param reader IndexReader to use for deletes.
     * @throws IndexingException
     * @throws IOException
     */
    protected void flushDeletes(final IndexModifier modifier) throws IndexingException, IOException {
    	for (final Iterator<Integer> i = pendingDeletes.iterator(); i.hasNext(); ) {
    		final Integer docId = i.next();
    		modifier.deleteDocument( docId );
    		i.remove();
    	}
    }
}
