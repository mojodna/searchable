package net.mojodna.searchable.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import net.mojodna.searchable.Searchable.Indexed;

public class AnnotationUtils {
    private static final Logger log = Logger.getLogger( AnnotationUtils.class );
    
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
        if ( null == method || null == annotation )
            return false;
        
        Class clazz = method.getDeclaringClass();
        
        // check implemented interfaces
        for ( final Class iface : clazz.getInterfaces() ) {
            try {
                final Method m = iface.getMethod( method.getName(), (Class[]) method.getParameterTypes() );
                if ( m.isAnnotationPresent( annotation ) )
                    return true;
            }
            catch (final NoSuchMethodException e) {}
        }
        
        // check all declarations, in this class or in superclasses
        while ( null != clazz ) {
            try {
                final Method m = clazz.getMethod( method.getName(), (Class[]) method.getParameterTypes() );
                if ( m.isAnnotationPresent( annotation ) )
                    return true;
            }
            catch (final NoSuchMethodException e) {
                // no method declared or inherited here
                break;
            }
            // check the superclass for a method with an appropriate annotation
            clazz = clazz.getSuperclass();
        }
        
        return false;
    }
    
    class Parent {
        @Indexed
        public String getName() {
            return "rick";
        }
        @Indexed
        public String getColor() {
            return "blue";
        }
    }
    
    class Child extends Parent {
        public String getName() {
            return "seth";
        }
        public String getSky() {
            return "orange";
        }
    }
    
    class Grandchild extends Child {
        public String getColor() {
            return "green";
        }
        @Indexed
        public String getSky() {
            return "vanilla";
        }
    }
    
    public static void main(final String[] args) {
        final AnnotationUtils x = new AnnotationUtils();
        
        log.debug("=== Parent ===");
        final AnnotationUtils.Parent p = x.new Parent();
        for ( final Method m : p.getClass().getMethods() ) {
            if ( AnnotationUtils.isAnnotationPresent( m, Indexed.class ) )
                log.debug( m.getName() );
        }

        log.debug("=== Child ===");
        final AnnotationUtils.Child c = x.new Child();
        for ( final Method m : c.getClass().getMethods() ) {
            if ( AnnotationUtils.isAnnotationPresent( m, Indexed.class ) )
                log.debug( m.getName() );
        }

        log.debug("=== Grandchild ===");
        final AnnotationUtils.Grandchild gc = x.new Grandchild();
        for ( final Method m : gc.getClass().getMethods() ) {
            if ( AnnotationUtils.isAnnotationPresent( m, Indexed.class ) )
                log.debug( m.getName() );
        }

    }
}
