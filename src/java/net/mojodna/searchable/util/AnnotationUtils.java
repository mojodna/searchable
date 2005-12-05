package net.mojodna.searchable.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

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

        Class clazz = method.getDeclaringClass();
        
        final Collection<Class> classesToCheck = new HashSet();
        while ( null != clazz ) {
            classesToCheck.add( clazz );
            
            // add implemented interfaces to the list of classes to check
            for ( final Class iface : clazz.getInterfaces() ) {
                classesToCheck.add( iface );
            }
            
            clazz = clazz.getSuperclass();
        }
        
        // check all superclasses and inherited interfaces
        for ( final Class c : classesToCheck ) {
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
}
