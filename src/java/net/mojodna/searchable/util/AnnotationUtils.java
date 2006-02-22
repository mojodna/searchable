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
package net.mojodna.searchable.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

/**
 * Annotation utility methods.
 * 
 * @author Seth Fitzsimmons
 */
public class AnnotationUtils {
    /**
     * Get a specific annotation present on a method.
     * 
     * This differs from AnnotatedElement.getAnnotation(Annotation) in that it
     * looks up the class hierarchy for inherited annotations.  (@Inherit only
     * applies to class-level annotations.)  It also checks declarations within
     * interfaces.
     * 
     * @see java.lang.reflect.AnnotatedElement#getAnnotations()
     * 
     * @param method Method to check for present annotations.
     * @param annotation Annotation to look for.
     * @return Instance of the specified annotation or null if not present.
     */
    public static Annotation getAnnotation(final Method method, final Class<? extends Annotation> annotation) {
        if ( null == method || null == annotation )
            return null;

        // check all superclasses and inherited interfaces
        for ( final Class c : AnnotationUtils.getClasses( method.getDeclaringClass() ) ) {
            try {
                final Method m = c.getMethod( method.getName(), (Class[]) method.getParameterTypes() );
                if ( m.isAnnotationPresent( annotation ) )
                    return m.getAnnotation( annotation );
            }
            catch (final NoSuchMethodException e) {}
        }
        
        return null;
    }
    
    /**
     * Get a specific annotation present on a class.
     * 
     * This differs from AnnotatedElement.getAnnotation(Annotation) in that it
     * looks up the class hierarchy for inherited annotations.  (@Inherit only
     * applies to class-level annotations.)  It also checks declarations within
     * interfaces.
     * 
     * @see java.lang.reflect.AnnotatedElement#getAnnotations()
     * 
     * @param clazz Class to check for present annotations.
     * @param annotation Annotation to look for.
     * @return Instance of the specified annotation or null if not present.
     */
    public static Annotation getAnnotation(Class clazz, final Class<? extends Annotation> annotation) {
        return getAnnotation( clazz, annotation, false );
    }
    
    /**
     * Get a specific annotation present on or in a class.
     * 
     * This differs from AnnotatedElement.getAnnotation(Annotation) in that it
     * looks up the class hierarchy for inherited annotations.  (@Inherit only
     * applies to class-level annotations.)  It also checks declarations within
     * interfaces.
     * 
     * @see java.lang.reflect.AnnotatedElement#getAnnotations()
     * 
     * @param clazz Class to check for present annotations.
     * @param annotation Annotation to look for.
     * @param includeMethods Whether to include methods when searching.
     * @return Instance of the specified annotation or null if not present.
     */
    public static Annotation getAnnotation(Class clazz, final Class<? extends Annotation> annotation, final boolean includeMethods) {
        if ( null == clazz || null == annotation )
            return null;
        
        if ( includeMethods ) {
            for (final Method m : clazz.getMethods() ) {
                if ( isAnnotationPresent( m, annotation ) )
                    return getAnnotation( m, annotation );
            }
        }

        // check all superclasses and inherited interfaces
        for ( final Class c : AnnotationUtils.getClasses( clazz ) ) {
            if ( c.isAnnotationPresent( annotation ) )
                return c.getAnnotation( annotation );
        }
        
        return null;
    }
    
    /**
     * Determine whether a method (or methods that it overrides) are annotated
     * with a specific annotation.
     * 
     * This differs from AnnotatedElement.getAnnotations() in that it looks up
     * the class hierarchy for inherited annotations.  (@Inherit only applies
     * to class-level annotations.)  It also checks declarations within
     * interfaces.
     * 
     * @see java.lang.reflect.AnnotatedElement#getAnnotations()
     * 
     * @param method Method to check for present annotations.
     * @param annotation Annotation to look for.
     * @return Whether the specified annotation is present on a given method.
     */
    public static boolean isAnnotationPresent(final Method method, final Class<? extends Annotation> annotation) {
        return ( null != getAnnotation( method, annotation ) ); 
    }

    /**
     * Gets a Collection of classes extended and interfaces implemented by the
     * specified class (including itself).
     * 
     * This could be done with ClassUtils, but this is more direct.
     * 
     * @param clazz Class to inspect.
     * @return Collection of classes extended and interfaces implemented.
     */
    private static Collection<Class> getClasses(Class clazz) {
        final Collection<Class> classes = new HashSet();
        while ( null != clazz ) {
            classes.add( clazz );
            
            // add implemented interfaces to the list of classes to check
            for ( final Class iface : clazz.getInterfaces() ) {
                classes.add( iface );
            }
            
            clazz = clazz.getSuperclass();
        }
        return classes;
    }
}
