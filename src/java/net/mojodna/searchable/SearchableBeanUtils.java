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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import net.mojodna.searchable.util.AnnotationUtils;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * Utility methods for working with Searchables.
 * 
 * @author Seth Fitzsimmons
 */
public class SearchableBeanUtils {
    /** Default id property name */
    public static final String ID_PROPERTY_NAME = "id";

    /**
     * Reflect on a set of classes to determine whether any fields have been
     * marked as default fields to search.
     * 
     * @param classes Classes to reflect on.
     * @return Array of default field names specified in all classes.
     */
    public static String[] getDefaultFieldNames(final Class<? extends Searchable>[] classes) {
        final Collection defaultFields = new HashSet();
        for (final Class<? extends Searchable> clazz : classes) {
            defaultFields.addAll( Arrays.asList( SearchableBeanUtils.getDefaultFieldNames( clazz ) ) );
        }

        return SearchableUtils.toStringArray( defaultFields );
    }
    
    /**
     * Reflect on the specified class to determine whether any fields have been
     * marked as default fields to search.
     * 
     * @param clazz Class to reflect on.
     * @return Array of default field names specified in class.
     */
    public static String[] getDefaultFieldNames(final Class<? extends Searchable> clazz) {
        final Searchable.DefaultFields annotation = (Searchable.DefaultFields) AnnotationUtils.getAnnotation( clazz, Searchable.DefaultFields.class );
        if ( null != annotation )
            return annotation.value();
        return null;
    }
    
    /**
     * Gets the name of the property that should be used to generate a search
     * excerpt.
     * 
     * @param clazz Class to reflect on.
     * @return Name of the excerptable property; null if none present.
     */
    public static String getExcerptPropertyName(final Class<? extends Result> clazz) {
        final PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors( clazz );
        for (final PropertyDescriptor d : descriptors) {
            if ( AnnotationUtils.isAnnotationPresent( d.getReadMethod(), Searchable.Excerptable.class ) )
                return d.getName();
        }
        
        return null;
    }
    
    /**
     * Gets the name of the property containing the bean's id.
     * 
     * @param clazz Class to reflect on.
     * @return Name of the id property.
     */
    public static String getIdPropertyName(final Class<? extends Searchable> clazz) {
        // look for Searchable.ID annotation
        final PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors( clazz );
        for ( final PropertyDescriptor descriptor : pds ) {
            if ( descriptor.getReadMethod().isAnnotationPresent( Searchable.ID.class ) ) {
                return descriptor.getName();
            }
        }
        
        return SearchableBeanUtils.ID_PROPERTY_NAME;
    }
}
