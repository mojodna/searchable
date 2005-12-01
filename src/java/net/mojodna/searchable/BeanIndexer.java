package net.mojodna.searchable;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Field;

public class BeanIndexer extends AbstractIndexer {
    private static final Logger log = Logger.getLogger( BeanIndexer.class );
    
    private static final Class[] annotations = { Searchable.Indexed.class, Searchable.Stored.class };
    
    public void add(final Searchable bean) throws IndexingException {
        // process a Searchable
        begin( getType( bean ), getId( bean ) );
        
        // iterate through fields
        for ( final PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors( bean ) ) {
            if ( !containsAnnotations( descriptor ) )
                continue;

            final String fieldName = descriptor.getName();

            log.debug("Indexing property " + fieldName );
            addFields( bean, descriptor );
        }
        
        commit();
    }
    
    /**
     * Delete method corresponding to a type of Object being indexed here.
     */
    public void delete(final Searchable bean) throws IndexingException {
        delete( getType( bean ), getId( bean ) );
    }
    
    /**
     * Searches for a suitable property to use as an id.  Uses the first
     * property annotated with Searchable.ID.  If none are available, it
     * falls back to the "id" field (if present).
     */
    private Object getId(final Searchable bean) throws IndexingException {
        try {
            return PropertyUtils.getProperty( bean, SearchableBeanUtils.getIdPropertyName( bean ) );
        } catch (final Exception e) {
            throw new IndexingException("Unable to determine value for id.", e );
        }
    }
    
    private String getType(final Searchable bean) {
        return bean.getClass().getName();
    }
    
    private boolean containsAnnotations(final PropertyDescriptor descriptor) {
        final Method readMethod = descriptor.getReadMethod();
        for ( final Class annotationClass : annotations ) {
            if ( null != readMethod && readMethod.isAnnotationPresent( annotationClass ) )
                return true;
        }
        return false;
    }
    
    private void addFields(final Searchable bean, final PropertyDescriptor descriptor) throws IndexingException {
        // TODO handle Dates and primitives
        // Dates *must* be handled as keywords and can/should be passed into
        // Lucene as Date objects (rather than String representations).
        // Numbers and Booleans (and their primitive forms) should probably be
        // handled as keywords as well
        
        final Method readMethod = descriptor.getReadMethod();
        for ( final Class annotationClass : annotations ) {
            if ( null != readMethod && readMethod.isAnnotationPresent( annotationClass ) ) {
                String fieldname = descriptor.getName();
                try {
                    final String value = PropertyUtils.getProperty( bean, descriptor.getName() ).toString();
                    
                    final Annotation annotation = readMethod.getAnnotation( annotationClass );
                    if ( annotation instanceof Searchable.Indexed ) {
                        final Searchable.Indexed i = (Searchable.Indexed) annotation;
                        
                        if ( StringUtils.isNotBlank( i.name() ) )
                            fieldname = i.name();

                        final Field field = new Field( fieldname, value, i.stored(), true, i.tokenized(), i.storeTermVector() );
                        field.setBoost( i.boost() );
                        addField( field );
                    } else if ( annotation instanceof Searchable.Stored ) {
                        final Searchable.Stored s = (Searchable.Stored) annotation;
                        
                        if ( StringUtils.isNotBlank( s.name() ) )
                            fieldname = s.name();

                        final Field field = new Field( fieldname, value, true, false, false );
                        addField( field );
                    }
                }
                catch (final Exception e) {
                    throw new IndexingException("Unable to index bean.", e );
                }
            }
        }
    }
}
