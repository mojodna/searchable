package net.mojodna.searchable;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Stack;

import net.mojodna.searchable.util.AnnotationUtils;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

public class BeanIndexer extends AbstractIndexer {
    private static final Logger log = Logger.getLogger( BeanIndexer.class );
    
    private static final Class[] annotations = { Searchable.Indexed.class, Searchable.Stored.class };
    
    public BeanIndexer() throws IndexException {
        super();
    }
    
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
    protected Object getId(final Searchable bean) throws IndexingException {
        try {
            return PropertyUtils.getProperty( bean, SearchableBeanUtils.getIdPropertyName( bean ) );
        } catch (final Exception e) {
            throw new IndexingException("Unable to determine value for id.", e );
        }
    }
    
    protected String getType(final Searchable bean) {
        return bean.getClass().getName();
    }
    
    private boolean containsAnnotations(final PropertyDescriptor descriptor) {
        final Method readMethod = descriptor.getReadMethod();
        
        for ( final Class annotationClass : annotations ) {
            if ( AnnotationUtils.isAnnotationPresent( readMethod, annotationClass ) )
                return true;
        }
        
        return false;
    }
    
    private Collection<String> getFieldnames(final PropertyDescriptor descriptor, final Stack stack) {
        final Collection<String> fieldnames = new LinkedList();
        
        String fieldname = descriptor.getName();
        
        for ( final Class annotationClass : annotations ) {
            final Annotation annotation = AnnotationUtils.getAnnotation( descriptor.getReadMethod(), annotationClass );
            if ( annotation instanceof Searchable.Indexed ) {
                final Searchable.Indexed i = (Searchable.Indexed) annotation;
                if ( StringUtils.isNotBlank( i.name() ) )
                    fieldname = i.name();
                
                // add any aliases
                fieldnames.addAll( Arrays.asList( i.aliases() ) );
            } else if ( annotation instanceof Searchable.Stored ) {
                final Searchable.Stored s = (Searchable.Stored) annotation;
                if ( StringUtils.isNotBlank( s.name() ) )
                    fieldname = s.name();
                
                // add any aliases
                fieldnames.addAll( Arrays.asList( s.aliases() ) );
            }
        }
        
        // add the default field name
        fieldnames.add( fieldname );
        
        final Collection<String> prefixedFieldnames = new LinkedList();
        for (final String name : fieldnames ) {
            if ( !stack.isEmpty() )
                prefixedFieldnames.add( stack.peek() + "." + name );
            else
                prefixedFieldnames.add( name );
        }
        
        return prefixedFieldnames;
    }
    
    private boolean isNested(final PropertyDescriptor descriptor) {
        for ( final Class annotationClass : annotations ) {
            final Annotation annotation = AnnotationUtils.getAnnotation( descriptor.getReadMethod(), annotationClass );
            if ( annotation instanceof Searchable.Indexed ) {
                final Searchable.Indexed i = (Searchable.Indexed) annotation;
                return i.nested();
            } else if ( annotation instanceof Searchable.Stored ) {
                final Searchable.Stored s = (Searchable.Stored) annotation;
                return s.nested();
            }
        }
        
        return false;
    }
    
    private float getBoost(final PropertyDescriptor descriptor) {
        for ( final Class annotationClass : annotations ) {
            final Annotation annotation = AnnotationUtils.getAnnotation( descriptor.getReadMethod(), annotationClass );
            if ( annotation instanceof Searchable.Indexed ) {
                final Searchable.Indexed i = (Searchable.Indexed) annotation;
                return i.boost();
            }
        }
        
        return DEFAULT_BOOST_VALUE;
    }
    
    protected Document processBean(final Document doc, final Searchable bean) throws IndexingException {
        return processBean( doc, bean, new Stack() );
    }
    
    private Document processBean(final Document doc, final Searchable bean, final Stack stack) throws IndexingException {
        return processBean( doc, bean, stack, DEFAULT_BOOST_VALUE );
    }
    
    private Document processBean(final Document doc, final Searchable bean, final Stack stack, final float boost) throws IndexingException {
        // iterate through fields
        for ( final PropertyDescriptor d : PropertyUtils.getPropertyDescriptors( bean ) ) {
            if ( !containsAnnotations( d ) ) {
                continue;
            }

            addFields( doc, bean, d, stack, boost );
        }
        return doc;
    }
    
    private Document addFields(final Document doc, final Searchable bean, final PropertyDescriptor descriptor, final Stack stack, final float inheritedBoost) throws IndexingException {
        final Method readMethod = descriptor.getReadMethod();
        for ( final Class annotationClass : annotations ) {
            if ( null != readMethod && AnnotationUtils.isAnnotationPresent( readMethod, annotationClass ) ) {
                
                // don't index elements marked as nested=false in a nested context
                if ( !stack.isEmpty() && !isNested( descriptor ) ) {
                    continue;
                }

                for (final String fieldname : getFieldnames( descriptor, stack ) ) {
                    log.debug("Indexing " + descriptor.getName() + " as " + fieldname );
                    
                    try {
                        final Object prop = PropertyUtils.getProperty( bean, descriptor.getName() );
                        if ( null == prop )
                            continue;
                        
                        if ( prop instanceof Date ) {
                            // handle Dates specially
                            float boost = DEFAULT_BOOST_VALUE;
                            
                            final Annotation annotation = AnnotationUtils.getAnnotation( readMethod, annotationClass );
                            if ( annotation instanceof Searchable.Indexed ) {
                                final Searchable.Indexed i = (Searchable.Indexed) annotation;
                                boost = i.boost();
                            }
                            
                            final Field field = Field.Keyword( fieldname, (Date) prop );
                            field.setBoost( inheritedBoost * boost );
                            doc.add( field );
                        } else if ( prop instanceof Searchable  ) {
                            // nested Searchables
                            stack.push( fieldname );
                            
                            processBean( doc, (Searchable) prop, stack, inheritedBoost * getBoost( descriptor ) );
                            
                            stack.pop();
                        } else {
                            final String value = prop.toString();
                            
                            final Annotation annotation = AnnotationUtils.getAnnotation( readMethod, annotationClass );
                            if ( annotation instanceof Searchable.Indexed ) {
                                final Searchable.Indexed i = (Searchable.Indexed) annotation;
                                
                                final Field field = new Field( fieldname, value, i.stored(), true, i.tokenized(), i.storeTermVector() );
                                field.setBoost( inheritedBoost * i.boost() );
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
        }
        
        return doc;
    }
}
