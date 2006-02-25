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

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

/**
 * Core methods for adding objects to an index.  This is intended to be
 * subclassed by a model-specific implementation that provides appropriate
 * add() methods.
 * 
 * @author Seth Fitzsimmons
 */
public abstract class AbstractIndexer extends IndexSupport {
    private static final Logger log = Logger.getLogger( AbstractIndexer.class );
    
    /**
     * Constructor.
     * 
     * @throws IndexException
     */
    public AbstractIndexer() throws IndexException {
        super();
    }
    
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
    protected void save(final Document document) throws IndexingException {
        log.debug("Committing document to index.");
        // delete document if necessary
        if ( null != document.get( TYPE_FIELD_NAME ) && null != document.get( ID_FIELD_NAME ) )
            delete( document.get( TYPE_FIELD_NAME ), document.get( ID_FIELD_NAME ) );
        
        synchronized ( writeLock ) {
            IndexWriter writer = null;
            try {
                writer = new IndexWriter( getIndexDirectory(), getAnalyzer(), false );
                log.debug("Writing document to index.");
                writer.addDocument( document );
            }
            catch (final IOException e) {
                log.error("Could not open index: " + e.getMessage(), e);
                throw new IndexingException( "Unable to commit document to index.", e );
            }
            finally {
                try {
                    if ( null != writer ) {
                        writer.close();
                    }
                }
                catch (final IOException e) {
                    log.warn("Could not close index: " + e.getMessage(), e);
                    throw new IndexingException( "Unable to commit document to index.", e );
                }
            }
        }
    }
    
    /**
     * Delete a document from the index.  This constructs a Term corresponding
     * to the compound key.
     * 
     * @param type Type of object that was indexed.
     * @param id Id of document that was indexed.
     */
    protected void delete(final String type, final Object id) throws IndexingException {
        log.debug("Deleting document.");
        synchronized ( writeLock ) {
            IndexReader reader = null;
            try {
                reader = IndexReader.open( getIndexDirectory() );
                reader.delete( new Term( COMPOUND_ID_FIELD_NAME, type + "-" + id ) );
            }
            catch (final IOException e) {
                log.error("Could not open index: " + e.getMessage(), e);
                throw new IndexingException( "Unable to delete document.", e );
            }
            finally {
                try {
                    if ( null != reader ) {
                        reader.close();
                    }
                }
                catch (final IOException e) {
                    log.warn("Could not close index: " + e.getMessage(), e);
                    throw new IndexingException( "Unable to delete document.", e );
                }
            }
        }
    }
}
