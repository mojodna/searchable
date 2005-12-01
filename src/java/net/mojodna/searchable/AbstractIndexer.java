package net.mojodna.searchable;

import java.io.IOException;

import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

public abstract class AbstractIndexer extends IndexSupport {
    private static final Logger log = Logger.getLogger( AbstractIndexer.class );
    
    private ThreadLocal<Document> documentHolder = new ThreadLocal();
    private ThreadLocal<String> idHolder = new ThreadLocal();
    private ThreadLocal<String> typeHolder = new ThreadLocal();
    private ThreadLocal<Boolean> pendingHolder = new ThreadLocal();
    
    /**
     * Subclass this class and call this method with an appropriate value for
     * type if you wish to index non-Searchables.
     */
    protected void begin(final String type, final Object id) {
        if ( isPending() )
            abort();
        
        log.debug("Creating document with type '" + type + "' and id '" + id + "'.");
        setType( type );
        setId( id.toString() );
        setDocument( new Document() );
        addField( Field.Keyword( TYPE_FIELD_NAME, type) );
        addField( Field.Keyword( ID_FIELD_NAME, id.toString() ) );
        addField( Field.Keyword( ID_TYPE_FIELD_NAME, id.getClass().getName() ) );
        addField( Field.Keyword( COMPOUND_ID_FIELD_NAME, type + "-" + id ) );
        setPending( true );
    }
    
    public void abort() {
        reset();
    }
    
    public void commit() throws IndexingException {
        if ( isPending() && null != getDocument() ) {
            log.debug("Committing document.");
            // delete document if necessary
            delete( getType(), getId() );
            
            IndexWriter writer = null;
            try {
                writer = new IndexWriter( getIndexPath(), getAnalyzer(), false );
                log.debug("Writing document to index.");
                writer.addDocument( getDocument() );
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
        
        reset();
    }
    
    /**
     * Delete a document from the index.
     */
    protected void delete(final String type, final Object id) throws IndexingException {
        log.debug("Deleting document.");
        IndexReader reader = null;
        try {
            reader = IndexReader.open( getIndexPath() );
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
    
    protected void addField(final Field field) {
        getDocument().add( field );
    }
    
    private void reset() {
        documentHolder.set( null );
        idHolder.set( null );
        setPending( true );
        typeHolder.set( null );
    }
    
    /**
     * Active document accessor.
     */
    private Document getDocument() {
        return documentHolder.get();
    }
    
    /**
     * Active document setter method.
     */
    private void setDocument(final Document document) {
        documentHolder.set( document );
    }
    
    /**
     * Active id accessor.
     */
    private String getId() {
        return idHolder.get();
    }
    
    /**
     * Active id setter method.
     */
    private void setId(final String id) {
        idHolder.set( id );
    }
    
    /**
     * Active type accessor.
     */
    private String getType() {
        return typeHolder.get();
    }
    
    /**
     * Active type setter method.
     */
    private void setType(final String type) {
        typeHolder.set( type );
    }
    
    /**
     * Has the active document been written to the index yet?
     */
    private boolean isPending() {
        return BooleanUtils.isTrue( pendingHolder.get() );
    }
    
    /**
     * Set the pending status of the active document.
     */
    private void setPending(final Boolean pending) {
        pendingHolder.set( pending );
    }
}
