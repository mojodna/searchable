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
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

import com.whirlycott.stylefeeder.search.SearchException;

public class Searcher extends IndexSupport {
    private static final Logger log = Logger.getLogger( Searcher.class );
    
    static {
        if ( null == ConvertUtils.lookup( UUID.class ) ) {
            ConvertUtils.register( new UUIDConverter(), UUID.class );
        }
    }
    
    /**
     * Search query interface.
     */
    public List<Result> search(final String _query) throws SearchException {
        IndexSearcher searcher = null;
        try {
            // "description" as a default field means little here
            final Query query = QueryParser.parse(_query, "description", getAnalyzer() );
            searcher = new IndexSearcher( getIndexPath() );

            final Hits hits = searcher.search(query);
            log.debug("Found " + hits.length() + " document(s) that matched query '" + _query + "':");

            final List<Result> results = new LinkedList<Result>();

            for (int i = 0; i < hits.length(); i++) {
                final Document doc = hits.doc(i);
                Result result = null;
                
                final String className = doc.get( TYPE_FIELD_NAME );
                log.debug("Creating new instance of: " + className);
                try {
                    final Object o = Class.forName(className).newInstance();
                    if ( o instanceof Searchable ) {
                        result = (Result) o;
                        final String idField = SearchableBeanUtils.getIdPropertyName( (Searchable) o );
                        
                        final Map storedFields = new HashMap();
                        final Enumeration fields = doc.fields();
                        while (fields.hasMoreElements()) {
                            final Field f = (Field) fields.nextElement();
                            // exclude private fields
                            if ( !PRIVATE_FIELD_NAMES.contains( f.name() ) )
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
                        result = new GenericResult();
                    }
                }
                catch (final ClassNotFoundException e) {
                    log.debug("Apparently " + className + " is not a class.");
                    result = new GenericResult();
                }
                catch (final Exception e) {
                    throw new SearchException("Could not reconstitute resultant object.", e );
                }
                
                result.setRanking( i );
                result.setScore( hits.score( i ) );
                
                results.add( result );
            }

            return results;
        }
        catch (final ParseException e) {
            throw new SearchException( e );
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
    
    public static void main(final String[] args) throws Exception {
        final Searcher s = new Searcher();
        final List<Result> results = s.search("bio:kayak");
        log.debug( results );
        for ( final Result result : results ) {
            log.debug("Score: " + result.getScore() );
            log.debug("Stored fields: " + result.getStoredFields() );
            // to test UUIDConverter
            log.debug("UUID: " + ((SearchableBean) results.get(0)).getUUID() );
        }
    }
}
