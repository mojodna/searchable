/*
 Copyright 2005-2006 Seth Fitzsimmons <seth@mojodna.net>

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
	/** List of annotations used for indexing (does not include sorting) */
	Class[] INDEXING_ANNOTATIONS = { Indexed.class, Stored.class };
	
	/**
	 * Provide a list of default fields to search when searching for an object
	 * of the annotated type.
	 * 
	 * 	TODO move into Result for logic's sake?
	 */
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DefaultFields {
		/**
		 * @return Default fields to use when searching for objects of the
		 * annotated object's type.
		 */
		String[] value() default {};
	}

	/**
	 * Mark this property as excerptable when creating search extracts.
	 * 
	 * TODO move into Result for logic's sake?
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Excerptable {
		/**
		 * @return Whether to excerpt a field annotated with this.
		 */
		boolean value() default true;
	}

	/**
	 * Mark this property as the id field for this class.
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ID {
		/**
		 * @return Property value.
		 */
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
		/**
		 * @return Alias names.
		 */
		String[] aliases() default {};

		/**
		 * @return Boost factor.
		 */
		float boost() default DEFAULT_BOOST_VALUE;

		/**
		 * @return Indexed name.
		 */
		String name() default "";

		/**
		 * @return Whether this property should be nested.
		 */
		boolean nested() default false;

		/**
		 * @return Whether this property should be stored.
		 */
		boolean stored() default false;

		/**
		 * @return Whether the term vector for this property should be stored.
		 */
		boolean storeTermVector() default false;

		/**
		 * @return Whether this property should be tokenized.
		 */
		boolean tokenized() default true;

		/**
		 * @return Property value.
		 */
		boolean value() default true;
	}

	/**
	 * Mark this property as sortable.  Use the "nested" attribute to specify
	 * whether this field should be processed in a nested Searchable.
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Sortable {
		/**
		 * @return Property name.
		 */
		String name() default "";

		/**
		 * @return Whether to nest this sortable field (i.e. to be able to sort
		 * by this field in a nested context.)
		 */
		boolean nested() default false;
	}

	/**
	 * Mark this property to be stored (but not indexed) in the index.  Use the
	 * "name" attribute to override the field name and the "nested" attribute
	 * to specify whether this field should be processed in a nested Searchable.
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Stored {
		/**
		 * @return Alias names.
		 */
		String[] aliases() default {};

		/**
		 * @return Property name.
		 */
		String name() default "";

		/**
		 * @return whether to nest this property.
		 */
		boolean nested() default false;

		/**
		 * @return Property value.
		 */
		boolean value() default true;
	}

	/** Default boost value (1.0) */
	static final float DEFAULT_BOOST_VALUE = 1F;
}
