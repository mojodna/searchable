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

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Stack;

import net.mojodna.searchable.Searchable.Indexed;
import net.mojodna.searchable.Searchable.Sortable;
import net.mojodna.searchable.Searchable.Stored;
import net.mojodna.searchable.util.AnnotationUtils;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * Utility methods for indexing Searchables.
 * 
 * @author Seth Fitzsimmons
 */
public abstract class AbstractBeanIndexer extends AbstractIndexer {
    private static final Logger log = Logger.getLogger( AbstractBeanIndexer.class );
    
    /** List of annotations used for indexing (does not include sorting) */
    private static final Class[] annotations = { Indexed.class, Stored.class };
    
    /**
     * Constructor.
     * 
     * @throws IndexException
     */
    public AbstractBeanIndexer() throws IndexException {
        super();
    }
    
    /**
     * Add a searchable bean to the index.
     * 
     * @param bean Bean to index.
     * @throws IndexingException
     */
    protected void doAdd(final Searchable bean) throws IndexingException {
        // process a Searchable
        final Document doc = createDocument( getType( bean ), getId( bean ) );
        
        processBean( doc, bean );
        
        save( doc );
    }
    
    /**
     * Delete an object from the index.
     * 
     * @param bean Object to delete.
     */
    protected void doDelete(final Searchable bean) throws IndexingException {
        delete( getType( bean ), getId( bean ) );
    }
    
    /**
     * Searches for a suitable property to use as an id.  Uses the first
     * property annotated with Searchable.ID.  If none are available, it
     * falls back to the "id" field (if present).
     * 
     * @param bean Object to reflect on.
     */
    protected Object getId(final Searchable bean) throws IndexingException {
        try {
            return PropertyUtils.getProperty( bean, SearchableBeanUtils.getIdPropertyName( bean.getClass() ) );
        } catch (final Exception e) {
            throw new IndexingException("Unable to determine value for id.", e );
        }
    }
    
    /**
     * Gets the type of the object being indexed.
     * 
     * @param bean Object being indexed.
     * @return Type of the object being indexed.
     */
    protected String getType(final Searchable bean) {
        return bean.getClass().getName();
    }
    
    /**
     * Does this property contain any index-specific annotations?
     * 
     * @param descriptor Property descriptor.
     * @return Whether this property contains any index-specific annotations. 
     */
    private boolean containsAnnotations(final PropertyDescriptor descriptor) {
        final Method readMethod = descriptor.getReadMethod();
        
        for ( final Class annotationClass : annotations ) {
            if ( AnnotationUtils.isAnnotationPresent( readMethod, annotationClass ) )
                return true;
        }
        
        return false;
    }
    
    /**
     * Generate a fully-qualified field name.
     * 
     * @param fieldname Property name.
     * @param stack Stack containing parent property names.
     * @return Fully qualified field name.
     */
    private String getFieldname(final String fieldname, final Stack<String> stack) {
        if ( !stack.isEmpty() ) {
            final StringBuffer sb = new StringBuffer();
            for ( final String component : stack ) {
                sb.append( component )
                  .append(".");
            }
            sb.append( fieldname );
            return sb.toString();
        } else {
            return fieldname;
        }
    }
    
    /**
     * Generate a list of fully qualified field names for a given property.
     * 
     * @param descriptor Property descriptor.
     * @param stack Stack containing parent property names.
     * @return Collection of fully qualified field names.
     */
    private Collection<String> getFieldnames(final PropertyDescriptor descriptor, final Stack<String> stack) {
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
            } else if ( annotation instanceof Searchable.Sortable ) {
                final Searchable.Sortable s = (Sortable) annotation;
                if ( StringUtils.isNotBlank( s.name() ) )
                    fieldname = s.name();
            }
        }
        
        // add the default field name
        fieldnames.add( fieldname );
        
        final Collection<String> prefixedFieldnames = new LinkedList();
        for (final String name : fieldnames ) {
            prefixedFieldnames.add( getFieldname( name, stack ) );
        }
        
        return prefixedFieldnames;
    }
    
    /**
     * Should this property be treated as nested?
     * 
     * @param descriptor Property descriptor.
     * @return Whether this property should be treated as nested.
     */
    private boolean isNested(final PropertyDescriptor descriptor) {
        for ( final Class annotationClass : annotations ) {
            final Annotation annotation = AnnotationUtils.getAnnotation( descriptor.getReadMethod(), annotationClass );
            if ( annotation instanceof Indexed ) {
                final Indexed i = (Indexed) annotation;
                return i.nested();
            } else if ( annotation instanceof Stored ) {
                final Stored s = (Stored) annotation;
                return s.nested();
            } else if ( annotation instanceof Sortable ) {
                final Sortable s = (Sortable) annotation;
                return s.nested();
            }
        }
        
        return false;
    }
    
    /**
     * Gets the boost factor for a specified property.
     * 
     * @param descriptor Property descriptor.
     * @return Boost factor for a specified property.
     */
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
    
    /**
     * Should the specified property be tokenized?
     * 
     * @param descriptor Property descriptor.
     * @return Whether the specified property should be tokenized.
     */
    private boolean isTokenized(final PropertyDescriptor descriptor) {
        final Annotation annotation = AnnotationUtils.getAnnotation( descriptor.getReadMethod(), Searchable.Indexed.class );
        if ( null != annotation ) {
            return ((Searchable.Indexed) annotation).tokenized();
        }
        return false;
    }

    /**
     * Should the specified property be indexed?
     * 
     * @param descriptor Property descriptor.
     * @return Whether the specified property should be indexed.
     */
    private boolean isIndexed(final PropertyDescriptor descriptor) {
        return ( null != AnnotationUtils.getAnnotation( descriptor.getReadMethod(), Searchable.Indexed.class ) ); 
    }

    /**
     * Should the specified property be stored in the index?
     * 
     * @param descriptor Property descriptor.
     * @return Whether the specified property should be stored in the index.
     */
    private boolean isStored(final PropertyDescriptor descriptor) {
        for ( final Class annotationClass : annotations ) {
            final Annotation annotation = AnnotationUtils.getAnnotation( descriptor.getReadMethod(), annotationClass );
            if ( annotation instanceof Searchable.Indexed ) {
                return ((Searchable.Indexed) annotation).stored();
            } else if ( annotation instanceof Searchable.Stored ) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Should the specified property have its term vectors stored in the index?
     * 
     * @param descriptor Property descriptor.
     * @return Whether the specified property should have its term vectors stored.
     */
    private boolean isVectorized(final PropertyDescriptor descriptor) {
        final Annotation annotation = AnnotationUtils.getAnnotation( descriptor.getReadMethod(), Searchable.Indexed.class );
        if ( null != annotation ) {
            return ((Searchable.Indexed) annotation).storeTermVector();
        }
        return false;
    }
    
    /**
     * Process a bean.
     * 
     * @param doc Document to add fields to.
     * @param bean Bean to process.
     * @return Document with additional fields.
     * @throws IndexingException
     */
    protected Document processBean(final Document doc, final Searchable bean) throws IndexingException {
        return processBean( doc, bean, new Stack() );
    }
    
    /**
     * Process a bean.
     * 
     * @param doc Document to add fields to.
     * @param bean Bean to process.
     * @param stack Stack containing parent field names.
     * @return Document with additional fields.
     * @throws IndexingException
     */
    private Document processBean(final Document doc, final Searchable bean, final Stack<String> stack) throws IndexingException {
        return processBean( doc, bean, stack, DEFAULT_BOOST_VALUE );
    }
    
    /**
     * Process a bean.
     * 
     * @param doc Document to add fields to.
     * @param bean Bean to process.
     * @param stack Stack containing parent field names.
     * @param boost Boost factor to apply to fields.
     * @return Document with additional fields.
     * @throws IndexingException
     */
    private Document processBean(final Document doc, final Searchable bean, final Stack<String> stack, final float boost) throws IndexingException {
        // iterate through fields
        for ( final PropertyDescriptor d : PropertyUtils.getPropertyDescriptors( bean ) ) {
            if ( !containsAnnotations( d ) ) {
                continue;
            }

            addBeanFields( doc, bean, d, stack, boost );
            addSortableFields( doc, bean, d, stack );
        }
        return doc;
    }
    
    /**
     * Add fields for each indexed/stored property.
     * 
     * @param doc Document to add fields to.
     * @param bean Bean to process.
     * @param descriptor Property descriptor.
     * @param stack Stack containing parent field names.
     * @param inheritedBoost Inherited boost factor.
     * @return Document with additional fields.
     * @throws IndexingException
     */
    private Document addBeanFields(final Document doc, final Searchable bean, final PropertyDescriptor descriptor, final Stack<String> stack, final float inheritedBoost) throws IndexingException {
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
                        
                        addFields( doc, fieldname, prop, descriptor, stack, inheritedBoost );
                    }
                    catch (final IndexingException e) {
                        throw e;
                    }
                    catch (final Exception e) {
                        throw new IndexingException("Unable to index bean.", e );
                    }
                }
            }
        }
        
        return doc;
    }
    
    /**
     * Create fields for each property.
     * 
     * @param doc Document to add fields to.
     * @param fieldname Field name to use.
     * @param prop Property value.
     * @param descriptor Property descriptor.
     * @param stack Stack containing parent field names.
     * @param inheritedBoost Inherited boost factor.
     * @return Document with additional fields.
     * @throws IndexingException
     */
    private Document addFields(final Document doc, final String fieldname, final Object prop, final PropertyDescriptor descriptor, final Stack<String> stack, final float inheritedBoost) throws IndexingException {
        if ( prop instanceof Date ) {
            // handle Dates specially
            float boost = getBoost( descriptor );
            
            final Field field = Field.Keyword( fieldname, (Date) prop );
            field.setBoost( inheritedBoost * boost );
            doc.add( field );
        } else if ( prop instanceof Iterable ) {
            // create multiple fields for things that can be iterated over
            float boost = getBoost( descriptor );
            
            for (final Object o : (Iterable) prop) {
                addFields( doc, fieldname, o, descriptor, stack, inheritedBoost * boost );
            }
        } else if ( prop instanceof Object[] ) {
            // create multiple fields for arrays of things
            float boost = getBoost( descriptor );
            
            for (final Object o : (Object[]) prop) {
                addFields( doc, fieldname, o, descriptor, stack, inheritedBoost * boost );
            }
        } else if ( prop instanceof Searchable  ) {
            // nested Searchables
            stack.push( descriptor.getName() );
            
            processBean( doc, (Searchable) prop, stack, inheritedBoost * getBoost( descriptor ) );
            
            stack.pop();
        } else {
            final String value = prop.toString();
            float boost = getBoost( descriptor );

            final Field field = new Field( fieldname, value, isStored( descriptor ), isIndexed( descriptor ), isTokenized( descriptor ), isVectorized( descriptor ) );
            field.setBoost( inheritedBoost * boost );
            doc.add( field );
        }
        
        return doc;
    }
    
    /**
     * Add sortable fields.
     * 
     * @param doc Document to add fields to.
     * @param bean Bean to process.
     * @param descriptor Property descriptor.
     * @param stack Stack containing parent field names.
     * @return Document with additional fields.
     * @throws IndexingException
     */
    private Document addSortableFields(final Document doc, final Searchable bean, final PropertyDescriptor descriptor, final Stack<String> stack) throws IndexingException {
        final Method readMethod = descriptor.getReadMethod();
        if ( null != readMethod && AnnotationUtils.isAnnotationPresent( readMethod, Sortable.class ) ) {
            
            // don't index elements marked as nested=false in a nested context
            if ( !stack.isEmpty() && !isNested( descriptor ) ) {
                return doc;
            }

            for (final String fieldname : getFieldnames( descriptor, stack ) ) {
                log.debug("Indexing " + descriptor.getName() + " as sortable (" + fieldname + ")." );
                
                try {
                    final Object prop = PropertyUtils.getProperty( bean, descriptor.getName() );
                    if ( null == prop )
                        return doc;
                    
                    if ( prop instanceof Date ) {
                        // handle Dates specially
                        doc.add( Field.Keyword( SORTABLE_PREFIX + fieldname, (Date) prop ) );
                    } else {
                        final String value = prop.toString();
                        doc.add( Field.Keyword( SORTABLE_PREFIX + fieldname, value ) );
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
