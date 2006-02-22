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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker interface for beans that should be searchable. Implementors of this
 * interface may use the provided annotations.
 * 
 * @author Seth Fitzsimmons
 */
public interface Searchable extends Result {
    /**
     * Mark this property as the id field for this class.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ID {
        boolean value() default true;
    }
    
    /**
     * Mark this property as indexed.  Use the "name" attribute to override the
     * field name, the "nested" attribute to specify whether it should be
     * indexed when the object is a property on another Searchable, the
     * "stored" attribute to specify whether it should be stored (defaults to
     * false), the "boost" attribute to set a boost value, the "tokenized"
     * attribute to change whether it is tokenized (defaults to true), and the
     * storeTermVector" attribute to specify whether a term vector should be
     * stored.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Indexed {
        String[] aliases() default {};
        float boost() default 1.0F;
        String name() default "";
        boolean nested() default true;
        boolean stored() default false;
        boolean storeTermVector() default false;
        boolean tokenized() default true;
        boolean value() default true;
    }
    
    /**
     * Mark this property to be stored (but not indexed) in the index.  Use the
     * "name" attribute to override the field name and the "nested" attribute
     * to specify whether this field should be processed in a nested Searchable.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Stored {
        String[] aliases() default {};
        String name() default "";
        boolean nested() default true;
        boolean value() default true;
    }
    
    /**
     * Mark this property as sortable.  Use the "nested" attribute to specify
     * whether this field should be processed in a nested Searchable.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Sortable {
        String name() default "";
        boolean nested() default false;
    }
    
    /**
     * Provide a list of default fields to search when searching for an object
     * of the annotated type.
     */
    // TODO move into Result for logic's sake?
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DefaultFields {
        String[] value() default {};
    }
    
    /**
     * Mark this property as excerptable when creating search extracts.
     */
    // TODO move into Result for logic's sake?
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Excerptable {
        boolean value() default true;
    }
}
