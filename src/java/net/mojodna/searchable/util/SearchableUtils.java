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
package net.mojodna.searchable.util;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.mojodna.searchable.IndexSupport;
import net.mojodna.searchable.Searchable;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.lucene.document.Field;

/**
 * Searchable-specific utility functions.
 *
 * @author Seth Fitzsimmons
 */
public final class SearchableUtils {
	private static final Map<String, Class<?>> returnTypeCache = new HashMap<String, Class<?>>();

	private static final Map<String, String> returnTypeMissCache = new HashMap<String, String>();

	/**
	 * Does this property contain any index-specific annotations?
	 * 
	 * @param descriptor Property descriptor.
	 * @return Whether this property contains any index-specific annotations. 
	 */
	public static final boolean containsIndexAnnotations(
			final PropertyDescriptor descriptor) {
		final Method readMethod = descriptor.getReadMethod();

		boolean containsIndexAnnotations = false;

		for (final Class<? extends Annotation> annotationClass : Searchable.INDEXING_ANNOTATIONS) {
			if (AnnotationUtils
					.isAnnotationPresent(readMethod, annotationClass))
				containsIndexAnnotations = true;
		}

		return containsIndexAnnotations;
	}

	/**
	 * Does this property contain any sortable-specific annotations?
	 * 
	 * @param descriptor Property descriptor.
	 * @return Whether this property contains any sortable-specific annotations.
	 */
	public static final boolean containsSortableAnnotations(
			final PropertyDescriptor descriptor) {
		return AnnotationUtils.isAnnotationPresent(descriptor.getReadMethod(),
				Searchable.Sortable.class);
	}

	/**
	 * Gets the boost factor for a specified property.
	 * 
	 * @param descriptor Property descriptor.
	 * @return Boost factor for a specified property.
	 */
	public static final float getBoost(final PropertyDescriptor descriptor) {
		final Annotation annotation = AnnotationUtils.getAnnotation(descriptor
				.getReadMethod(), Searchable.Indexed.class);
		if (annotation instanceof Searchable.Indexed) {
			final Searchable.Indexed i = (Searchable.Indexed) annotation;
			return i.boost();
		}

		return Searchable.DEFAULT_BOOST_VALUE;
	}

	/**
	 * @param clazz
	 * @return Field names.
	 */
	public static final String[] getFieldNames(
			final Class<? extends Searchable> clazz) {
		Set<String> fieldNames = new HashSet<String>();
		for (Field fields : getFields(clazz)) {
			fieldNames.add(fields.name());
		}
		return fieldNames.toArray(new String[] {});
	}

	/**
	 * @param clazz
	 * @return Fields.
	 */
	public static final Field[] getFields(
			final Class<? extends Searchable> clazz) {
		Set<Field> fields = new HashSet<Field>();

		for (final PropertyDescriptor d : PropertyUtils
				.getPropertyDescriptors(clazz)) {

			if (containsIndexAnnotations(d)) {
				final Field f = new Field(d.getName(), clazz.getName(),
						isStored(d), getIndexStyle(d));
				f.setBoost(getBoost(d));
				fields.add(f);
			}

			if (containsSortableAnnotations(d)) {
				fields.add(new Field(
						IndexSupport.SORTABLE_PREFIX + d.getName(), clazz
								.getName(), Field.Store.YES, Field.Index.NO));
			}
		}

		return fields.toArray(new Field[] {});
	}

	/**
	 * How should the specified property be indexed?
	 * 
	 * @param descriptor Property descriptor.
	 * @return How the specified property should be indexed.
	 */
	public static final Field.Index getIndexStyle(
			final PropertyDescriptor descriptor) {
		final Annotation annotation = AnnotationUtils.getAnnotation(descriptor
				.getReadMethod(), Searchable.Indexed.class);
		if (null != annotation) {
			if (((Searchable.Indexed) annotation).tokenized()) {
				return Field.Index.TOKENIZED;
			} else {
				return Field.Index.UN_TOKENIZED;
			}
		}
		return Field.Index.NO;
	}

	/**
	 * @param clazz
	 * @param propertyName
	 * @return Return type.
	 */
	public static final Class<?> getReturnType(final Class<?> clazz,
			final String propertyName) {
		final String key = clazz.getName() + "#" + propertyName;

		if (returnTypeCache.containsKey(key)) {
			return returnTypeCache.get(key);
		}

		if (returnTypeMissCache.containsKey(key)) {
			return null;
		}

		for (final PropertyDescriptor d : PropertyUtils
				.getPropertyDescriptors(clazz)) {
			if (d.getName().equals(propertyName)) {
				final Class<?> returnType = d.getReadMethod().getReturnType();
				returnTypeCache.put(key, returnType);
				return returnType;
			}
		}

		returnTypeMissCache.put(key, key);
		return null;
	}

	/**
	 * @param clazz
	 * @param propertyName
	 * @return Whether this property may contain multiple values (i.e. is a Collection).
	 */
	public static final boolean isMultiValued(final Class<?> clazz,
			final String propertyName) {
		final Class<?> returnType = getReturnType(clazz, propertyName);
		if (returnType.isArray()) {
			return true;
		}
		if (Iterable.class.isAssignableFrom(returnType)) {
			return true;
		}
		return false;
	}

	/**
	 * Is the specified character a reserved Lucene character.
	 *
	 * @param c
	 * @return Whether the specified character is reserved.
	 */
	public final static boolean isReservedCharacter(char c) {
		return (c == '+') || (c == '-') || (c == '&') || (c == '|')
				|| (c == '!') || (c == '(') || (c == ')') || (c == '{')
				|| (c == '}') || (c == '[') || (c == ']') || (c == '^')
				|| (c == '"') || (c == '~') || (c == '*') || (c == '?')
				|| (c == ':') || (c == '\\');
	}

	/**
	 * Should the specified property be stored in the index?
	 * 
	 * @param descriptor Property descriptor.
	 * @return Whether the specified property should be stored in the index.
	 */
	public static final Field.Store isStored(
			final PropertyDescriptor descriptor) {
		for (final Class<? extends Annotation> annotationClass : Searchable.INDEXING_ANNOTATIONS) {
			final Annotation annotation = AnnotationUtils.getAnnotation(
					descriptor.getReadMethod(), annotationClass);
			if (annotation instanceof Searchable.Indexed) {
				if (((Searchable.Indexed) annotation).stored())
					return Field.Store.YES;
				else
					return Field.Store.NO;
			} else if (annotation instanceof Searchable.Stored) {
				return Field.Store.YES;
			}
		}

		return Field.Store.NO;
	}

	/**
	 * Converts a Collection to an array of Strings.
	 * 
	 * @param fields Collection to convert.
	 * @return Array of Strings.
	 */
	public final static String[] toStringArray(final Collection fields) {
		final String[] defaultFields = new String[fields.size()];
		int i = 0;
		for (final Object f : fields) {
			defaultFields[i++] = f.toString();
		}
		return defaultFields;
	}
}
