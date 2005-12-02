package net.mojodna.searchable;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Stack;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

public class BeanIndexer extends AbstractIndexer {
    private static final Logger log = Logger.getLogger( BeanIndexer.class );
    
    private static final Class[] annotations = { Searchable.Indexed.class, Searchable.Stored.class };
    
    public void add(final Searchable bean) throws IndexingException {
        // process a Searchable
        final Document doc = createDocument( getType( bean ), getId( bean ) );
        
        processBean( doc, bean );
        
        save( doc );
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
    
    private String getFieldname(final PropertyDescriptor descriptor, final Stack stack) {
        String fieldname = descriptor.getName();
        
        for ( final Class annotationClass : annotations ) {
            final Annotation annotation = descriptor.getReadMethod().getAnnotation( annotationClass );
            if ( annotation instanceof Searchable.Indexed ) {
                final Searchable.Indexed i = (Searchable.Indexed) annotation;
                if ( StringUtils.isNotBlank( i.name() ) )
                    fieldname = i.name();
            } else if ( annotation instanceof Searchable.Stored ) {
                final Searchable.Stored s = (Searchable.Stored) annotation;
                if ( StringUtils.isNotBlank( s.name() ) )
                    fieldname = s.name();
            }
        }
        
        if ( !stack.isEmpty() )
            fieldname = stack.peek() + "." + fieldname;
        
        return fieldname;
    }
    
    private Document processBean(final Document doc, final Searchable bean) throws IndexingException {
        return processBean( doc, bean, new Stack() );
    }
    
    private Document processBean(final Document doc, final Searchable bean, final Stack stack) throws IndexingException {
        // iterate through fields
        for ( final PropertyDescriptor d : PropertyUtils.getPropertyDescriptors( bean ) ) {
            if ( !containsAnnotations( d ) )
                continue;

            log.debug("Indexing property " + getFieldname( d, stack ) );
            addFields( doc, bean, d, stack );
        }
        return doc;
    }
    
    private Document addFields(final Document doc, final Searchable bean, final PropertyDescriptor descriptor, final Stack stack) throws IndexingException {
        final Method readMethod = descriptor.getReadMethod();
        for ( final Class annotationClass : annotations ) {
            if ( null != readMethod && readMethod.isAnnotationPresent( annotationClass ) ) {
                String fieldname = descriptor.getName();
                fieldname = getFieldname( descriptor, stack );

                try {
                    if ( descriptor.getPropertyType().equals( Date.class ) ) {
                        // handle Dates specially
                        final Date value = (Date) PropertyUtils.getProperty( bean, descriptor.getName() );
                        float boost = DEFAULT_BOOST_VALUE;
                        
                        if ( null == value )
                            continue;
                        
                        final Annotation annotation = readMethod.getAnnotation( annotationClass );
                        if ( annotation instanceof Searchable.Indexed ) {
                            final Searchable.Indexed i = (Searchable.Indexed) annotation;
                            boost = i.boost();
                        }
                        
                        final Field field = Field.Keyword( fieldname, value );
                        field.setBoost( boost );
                        doc.add( field );
                    } else if ( Searchable.class.isAssignableFrom( descriptor.getPropertyType() ) ) {
                        // nested Searchables
                        stack.push( fieldname );
                        
                        final Searchable value = (Searchable) PropertyUtils.getProperty( bean, descriptor.getName() );
                        if ( null != value )
                            processBean( doc, value, stack );
                        
                        stack.pop();
                    } else {
                        final Object prop = PropertyUtils.getProperty( bean, descriptor.getName() );
                        if ( null == prop )
                            continue;
                        
                        final String value = prop.toString();
                        
                        final Annotation annotation = readMethod.getAnnotation( annotationClass );
                        if ( annotation instanceof Searchable.Indexed ) {
                            final Searchable.Indexed i = (Searchable.Indexed) annotation;
                            
                            final Field field = new Field( fieldname, value, i.stored(), true, i.tokenized(), i.storeTermVector() );
                            field.setBoost( i.boost() );
                            doc.add( field );
                        } else if ( annotation instanceof Searchable.Stored ) {
                            final Field field = new Field( fieldname, value, true, false, false );
                            doc.add( field );
                        }
                    }
                }
                catch (final Exception e) {
                    throw new IndexingException("Unable to index bean.", e );
                }
            }
        }
        
        return doc;
    }
}
