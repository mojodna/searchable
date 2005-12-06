package net.mojodna.searchable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public abstract class IndexSupport {
    private static final Logger log = Logger.getLogger( IndexSupport.class );
    
    public static final String DEFAULT_INDEX_PATH = System.getProperty("java.io.tmpdir") + File.separatorChar + "lucene";
    public static final float DEFAULT_BOOST_VALUE = 1F;
    public static final String ID_FIELD_NAME = "_id";
    public static final String ID_TYPE_FIELD_NAME = "_id-type";
    public static final String TYPE_FIELD_NAME = "_type";
    public static final String COMPOUND_ID_FIELD_NAME = "_cid";
    protected static final Collection PRIVATE_FIELD_NAMES = Arrays.asList( new String[] { ID_FIELD_NAME, ID_TYPE_FIELD_NAME, TYPE_FIELD_NAME, COMPOUND_ID_FIELD_NAME } );
    
    private Analyzer analyzer = new StandardAnalyzer();
    private Directory indexDirectory;

    public IndexSupport() throws IndexException {
        setIndexPath( DEFAULT_INDEX_PATH );
    }
    
    public String getIndexPath() {
        if ( getIndexDirectory() instanceof FSDirectory )
            return ((FSDirectory) getIndexDirectory()).getFile().getAbsolutePath();
        else
            return "<memory>";
    }
    
    public void setIndexPath(final String indexPath) throws IndexException {
        final File indexFile = new File( indexPath );
        if ( !indexFile.exists() ) {
            indexFile.mkdirs();
        }
        
        try {
            setIndexDirectory( FSDirectory.getDirectory( indexFile, false ) );
        }
        catch (final IOException e) {
            throw new IndexException( e );
        }
    }
    
    protected Directory getIndexDirectory() {
        return indexDirectory;
    }
    
    protected void setIndexDirectory(final Directory indexDirectory) {
        this.indexDirectory = indexDirectory;
    }
    
    public Analyzer getAnalyzer() {
        return analyzer;
    }
    
    public void setAnalyzer(final Analyzer analyzer) {
        this.analyzer = analyzer;
    }
    
    public void initialize() throws IndexException {
        initialize( false );
    }
    
    public void initialize(final boolean createIndex) throws IndexException {
        log.info("Initializing index at " + getIndexPath() );
        IndexReader reader = null;
        try {
            // (re-)create the index if requested
            if ( createIndex )
                createIndex();
            
            // attempt to open an IndexReader
            reader = IndexReader.open( getIndexDirectory() );
        }
        catch (final IOException e) {
            log.debug("Could not open index: " + e.getMessage() );
            // attempt to create the index, as it appears to not exist
            createIndex();
        }
        finally {
            if ( null != reader ) {
                try {
                    reader.close();
                }
                catch (final IOException e) {
                    log.warn("Could not close index: " + e.getMessage(), e);
                    throw new IndexException( "Unable to initialize index.", e );
                }
            }
        }
    }
    
    /**
     * Create a new Lucene index, creating directories if necessary.
     */
    public void createIndex() throws IndexException {
        IndexWriter writer = null;
        try {
            writer = new IndexWriter( getIndexDirectory(), getAnalyzer(), true );
        }
        catch (final IOException e) {
            log.error("Could not create index: " + e.getMessage(), e);
            throw new IndexException( "Unable to create index.", e );
        }
        finally {
            if ( null != writer ) {
                try {
                    writer.close();
                }
                catch (final IOException e) {
                    log.warn("Could not close index: " + e.getMessage(), e);
                    throw new IndexException( "Unable to create index.", e );
                }
            }
        }
    }
    
    public void close() throws IndexException {
        optimizeIndex();
    }
    
    /**
     * Optimize the active index.
     */
    protected void optimizeIndex() throws IndexException {
        IndexWriter writer = null;
        try {
            writer = new IndexWriter( getIndexDirectory(), getAnalyzer(), false );
            writer.optimize();
        }
        catch (final IOException e) {
            log.error("Could not optimize index: " + e.getMessage(), e);
            throw new IndexException( "Unable to optimize index.", e );
        }
        finally {
            if ( null != writer ) {
                try {
                    writer.close();
                }
                catch (final IOException e) {
                    log.warn("Could not close index: " + e.getMessage(), e);
                    throw new IndexException( "Unable to optimize index.", e );
                }
            }
        }
    }
}
