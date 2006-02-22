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

/**
 * Support class for classes that access an index.  Contains common methods
 * for indexing and searching as well as constants.
 * 
 * @author Seth Fitzsimmons
 */
public abstract class IndexSupport {
    private static final Logger log = Logger.getLogger( IndexSupport.class );
    
    /** Default index path ($TEMP/lucene) */
    public static final String DEFAULT_INDEX_PATH = System.getProperty("java.io.tmpdir") + File.separatorChar + "lucene";
    /** Default Analyzer */
    public static final Analyzer DEFAULT_ANALYZER = new StandardAnalyzer();
    /** Default boost value (1.0) */
    public static final float DEFAULT_BOOST_VALUE = 1F;
    /** Name of id field */
    public static final String ID_FIELD_NAME = "_id";
    /** Name of id type field (contains the class name for the id) */
    public static final String ID_TYPE_FIELD_NAME = "_id-type";
    /** Name of type field (contains the class name for the indexed item) */
    public static final String TYPE_FIELD_NAME = "_type";
    /** Compound id/type field */
    public static final String COMPOUND_ID_FIELD_NAME = "_cid";
    /** Prefix for keyword fields intended for sorting */
    public static final String SORTABLE_PREFIX = "_sort-";
    
    /** Collection of field names internal to searchable */
    protected static final Collection PRIVATE_FIELD_NAMES = Arrays.asList( new String[] { ID_FIELD_NAME, ID_TYPE_FIELD_NAME, TYPE_FIELD_NAME, COMPOUND_ID_FIELD_NAME } );
    /** A write lock to ensure that writing to the index is done in a synchronized manner */
    protected static Object writeLock = new Object();
    
    /** Analyzer in use */
    private Analyzer analyzer = DEFAULT_ANALYZER;
    /** Index directory */
    private Directory indexDirectory;

    /**
     * Constructor.
     * 
     * @throws IndexException
     */
    public IndexSupport() throws IndexException {
        setIndexPath( DEFAULT_INDEX_PATH );
    }
    
    /**
     * Gets the index path in use.
     * 
     * @return Index path or &lt;memory&gt; if in-memory.
     */
    public String getIndexPath() {
        if ( getIndexDirectory() instanceof FSDirectory )
            return ((FSDirectory) getIndexDirectory()).getFile().getAbsolutePath();
        else
            return "<memory>";
    }
    
    /**
     * Sets the index path to use for this index.
     * 
     * Note: this will always create the default index path.
     * 
     * @param indexPath Index path.
     * @throws IndexException
     */
    public void setIndexPath(final String indexPath) throws IndexException {
        final File indexFile = new File( indexPath );
        // TODO only create the default path if that's the one to be used
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
    
    /**
     * Gets the underlying Directory containing this index.
     * 
     * @return Directory holding this index.
     */
    protected Directory getIndexDirectory() {
        return indexDirectory;
    }
    
    /**
     * Sets the underlying Directory containing this index.
     * 
     * @param indexDirectory
     */
    protected void setIndexDirectory(final Directory indexDirectory) {
        this.indexDirectory = indexDirectory;
    }
    
    /**
     * Gets the Analyzer in use.
     * 
     * @return Analyzer in use.
     */
    public Analyzer getAnalyzer() {
        return analyzer;
    }
    
    /**
     * Sets the Analyzer in use.
     * 
     * @param analyzer Analyzer to use.
     */
    public void setAnalyzer(final Analyzer analyzer) {
        this.analyzer = analyzer;
    }
    
    /**
     * Initialize the index without (re-)creating it.
     * 
     * @throws IndexException
     */
    public void initialize() throws IndexException {
        initialize( false );
    }
    
    /**
     * Initialize the index.
     * 
     * @param createIndex (Re-)create the index.
     * @throws IndexException
     */
    public void initialize(final boolean createIndex) throws IndexException {
        log.info("Initializing index at " + getIndexPath() );
        synchronized ( writeLock ) {
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
    }
    
    /**
     * Creates a new index.
     * 
     * @throws IndexException
     */
    public void createIndex() throws IndexException {
        synchronized ( writeLock ) {
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
    }
    
    /**
     * Closes (and optimizes) the active index.
     * 
     * @throws IndexException
     */
    public void close() throws IndexException {
        optimizeIndex();
    }
    
    /**
     * Optimize the active index.
     * 
     * @throws IndexException
     */
    protected void optimizeIndex() throws IndexException {
        synchronized ( writeLock ) {
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
}
