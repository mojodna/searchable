/*
 * Created on Nov 27, 2005 by phil
 *
 */
package com.whirlycott.stylefeeder.search;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.whirlycott.stylefeeder.common.Account;
import com.whirlycott.stylefeeder.common.Bookmark;
import com.whirlycott.stylefeeder.util.Config;

public class IndexManager {

    /**
     * Logger.
     */
    private static final Logger log = Logger.getLogger(IndexManager.class);

    private static final IndexManager singleton = new IndexManager();

    private final String searchIndexPath = Config.getInstance().getString("search.index.dir");

    private final boolean recreateIndex = Config.getInstance().getBoolean("search.index.rebuild");

    private IndexWriter writer = null;

    private IndexManager() {
        super();
        log.debug("Created singleton instace of index singleton");
        try {
            createIndexWriter(recreateIndex);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    protected void finalize() {
        if (writer != null)
            try {
                log.debug("Closing writer to index.");
                writer.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
    }

    public static IndexManager getInstance() {
        return singleton;
    }

    protected void createIndexWriter(final boolean _forceCreate) throws IOException {
        log.debug("Getting index writer...");
        writer = new IndexWriter(searchIndexPath, getAnalyzer(), _forceCreate);
    }

    private Analyzer getAnalyzer() {
        return new StandardAnalyzer();
    }

    public void add(final Bookmark _b) throws IndexingException {
        log.debug("Adding bookmark to index: " + _b.getId());
        assert StringUtils.isNotBlank(_b.getId()) : "A bookmark had a blank id";
        try {
            if (writer == null)
                createIndexWriter(false);

            addToIndex(_b);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IndexingException(e);
        }
    }

    public void add(final Account _a) throws IndexingException {
        log.debug("Adding account to index: " + _a.getId());
        try {
            if (writer == null)
                createIndexWriter(false);

            addToIndex(_a);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IndexingException(e);
        }
    }

    public void add(final Iterator<Bookmark> _bookmarks) throws IndexingException {
        log.debug("Adding bookmarks via an iterator");
        try {
            if (writer == null)
                createIndexWriter(false);

            while (_bookmarks.hasNext()) {
                add(_bookmarks.next());
            }

            log.debug("Closing index.");
            writer.close();
        } catch (Exception e) {
            log.error(e);
            throw new IndexingException(e);
        }
    }

    protected synchronized void addToIndex(final Object _o) throws IndexingException {

        final Document doc = new Document();
        final String type = _o.getClass().getName();
        log.debug("----------------------------------------------------------");
        log.debug("Indexing an object of type: " + type);
        doc.add(org.apache.lucene.document.Field.Keyword("_class", type));

        final Field[] fields = _o.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            log.debug("Reflecting on field: " + fields[i].toGenericString());
            if (fields[i].isAnnotationPresent(Searchable.class)) {
                final int value = fields[i].getAnnotation(Searchable.class).value();
                log.debug("Found a searchable annotation with value: " + value);

                org.apache.lucene.document.Field docField = null;
                final String name = fields[i].getName();
                String content = null;
                try {
                    content = PropertyUtils.getSimpleProperty(_o, fields[i].getName()).toString();
                } catch (final Exception e) {
                    log.error(e.getMessage(), e);
                }
                switch (value) {
                case 1:
                    docField = org.apache.lucene.document.Field.Keyword(name, content);
                    break;
                case 2:
                    docField = org.apache.lucene.document.Field.UnIndexed(name, content);
                    break;
                case 3:
                    docField = org.apache.lucene.document.Field.UnStored(name, content);
                    break;
                case 4:
                    docField = org.apache.lucene.document.Field.Text(name, content);
                    break;
                default:
                    throw new IndexingException("Found a field marked as Searchable with no value");
                }

                log.debug("Indexing docfield");
                log.debug(name);
                log.debug(content);
                doc.add(docField);
            }

            log.debug("Adding document to index...");
            try {
                writer.addDocument(doc);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new IndexingException(e);
            }
        }
        log.debug("----------------------------------------------------------");
    }

    public synchronized void delete(final Bookmark _b) {

    }

    public synchronized void delete(final Account _a) {

    }

    /**
     * Search query interface.
     * 
     * @param _query
     * @return
     * @throws ParseException
     * @throws SearchException
     */
    public List<Object> search(final String _query) throws ParseException, SearchException {
        try {
            final Query query = QueryParser.parse(_query, "description", new StandardAnalyzer());
            final Directory dir = FSDirectory.getDirectory(searchIndexPath, false);
            final IndexSearcher searcher = new IndexSearcher(dir);

            final Hits hits = searcher.search(query);
            log.debug("Found " + hits.length() + " document(s) that matched query '" + _query + "':");

            final List<Object> results = new LinkedList<Object>();

            for (int i = 0; i < hits.length(); i++) {
                final Document doc = hits.doc(i);
                final Enumeration fields = doc.fields();
                while (fields.hasMoreElements()) {
                    final org.apache.lucene.document.Field f = (org.apache.lucene.document.Field) fields.nextElement();
                    log.debug("Found field: " + f.name() + " with value: " + f.stringValue());
                }

                assert doc != null : "Retrieved a null Document from the index";

                final String className = doc.getField("_class").stringValue();
                log.debug("Creating new instance of: " + className);
                final Object o = Class.forName(className).newInstance();

                final org.apache.lucene.document.Field idValue = doc.getField("id");
                if (idValue != null) {
                    log.debug(idValue.stringValue());
                    log.debug("Setting id to " + idValue.stringValue());
                    PropertyUtils.setSimpleProperty(o, "id", idValue.stringValue());
                    results.add(o);
                } else {
                    log.warn("Field value for 'id' was null");
                }
                
                log.debug("------------------------------------------------");

            }

            return results;
        } catch (final Exception e) {
            throw new SearchException(e);
        }
    }
}
