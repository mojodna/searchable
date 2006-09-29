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

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Iterator;
import java.util.Stack;

import net.mojodna.searchable.Searchable.Indexed;
import net.mojodna.searchable.Searchable.Sortable;
import net.mojodna.searchable.Searchable.Stored;
import net.mojodna.searchable.util.AnnotationUtils;
import net.mojodna.searchable.util.SearchableUtils;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * Utility methods for indexing Searchables.
 * 
 * @author Seth Fitzsimmons
 */
public abstract class AbstractBeanIndexer extends AbstractIndexer {
	private static final Logger log = Logger
			.getLogger(AbstractBeanIndexer.class);

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
	private Document addBeanFields(final Document doc, final Searchable bean,
			final PropertyDescriptor descriptor, final Stack<String> stack,
			final float inheritedBoost) throws IndexingException {
		final Method readMethod = descriptor.getReadMethod();
		for (final Class<? extends Annotation> annotationClass : Searchable.INDEXING_ANNOTATIONS) {
			if (null != readMethod
					&& AnnotationUtils.isAnnotationPresent(readMethod,
							annotationClass)) {

				// don't index elements marked as nested=false in a nested context
				if (!stack.isEmpty() && !isNested(descriptor)) {
					continue;
				}

				for (final String fieldname : SearchableUtils.getFieldnames(descriptor)) {
					log.debug("Indexing " + descriptor.getName() + " as "
							+ getFieldname(fieldname, stack));

					try {
						final Object prop = PropertyUtils.getProperty(bean,
								descriptor.getName());
						if (null == prop)
							continue;

						addFields(doc, fieldname, prop, descriptor, stack,
								inheritedBoost);
					} catch (final IndexingException e) {
						throw e;
					} catch (final Exception e) {
						throw new IndexingException("Unable to index bean.", e);
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
	protected Document addFields(final Document doc, final String fieldname,
			final Object prop, final PropertyDescriptor descriptor,
			final Stack<String> stack, final float inheritedBoost)
			throws IndexingException {
		if (prop instanceof Date) {
			// handle Dates specially
			float boost = SearchableUtils.getBoost(descriptor);

			// TODO allow resolution to be specified in annotation
			// TODO serialize as canonical date (for Solr)
			final Field field = new Field(getFieldname(fieldname, stack),
					DateTools.dateToString((Date) prop,
							DateTools.Resolution.SECOND), Field.Store.YES,
					Field.Index.UN_TOKENIZED);
			field.setBoost(inheritedBoost * boost);
			doc.add(field);
		} else if (prop instanceof Iterable) {
			// create multiple fields for things that can be iterated over
			float boost = SearchableUtils.getBoost(descriptor);

			for (final Object o : (Iterable) prop) {
				addFields(doc, fieldname, o, descriptor, stack, inheritedBoost
						* boost);
			}
		} else if (prop instanceof Object[]) {
			// create multiple fields for arrays of things
			float boost = SearchableUtils.getBoost(descriptor);

			for (final Object o : (Object[]) prop) {
				addFields(doc, fieldname, o, descriptor, stack, inheritedBoost
						* boost);
			}
		} else if (prop instanceof Searchable) {
			// nested Searchables
			stack.push(fieldname);

			processBean(doc, (Searchable) prop, stack, inheritedBoost
					* SearchableUtils.getBoost(descriptor));

			stack.pop();
		} else {
			final String value = prop.toString();
			float boost = SearchableUtils.getBoost(descriptor);

			final Field field = new Field(getFieldname(fieldname, stack),
					value, SearchableUtils.isStored(descriptor), SearchableUtils.getIndexStyle(descriptor),
					isVectorized(descriptor));
			field.setBoost(inheritedBoost * boost);
			doc.add(field);
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
	protected Document addSortableFields(final Document doc,
			final Searchable bean, final PropertyDescriptor descriptor,
			final Stack<String> stack) throws IndexingException {
		final Method readMethod = descriptor.getReadMethod();
		if (null != readMethod
				&& AnnotationUtils.isAnnotationPresent(readMethod,
						Sortable.class)) {

			// don't index elements marked as nested=false in a nested context
			if (!stack.isEmpty() && !isNestedSortable(descriptor)) {
				return doc;
			}

			for (final String fieldname : SearchableUtils.getFieldnames(descriptor)) {
				log.debug("Indexing " + descriptor.getName() + " as sortable ("
						+ getFieldname(fieldname, stack) + ").");

				try {
					final Object prop = PropertyUtils.getProperty(bean,
							descriptor.getName());
					if (null == prop)
						return doc;

					if (prop instanceof Date) {
						// handle Dates specially
						// TODO specify resolution
						doc.add(new Field(SORTABLE_PREFIX
								+ getFieldname(fieldname, stack), DateTools
								.dateToString((Date) prop,
										DateTools.Resolution.SECOND),
								Field.Store.YES, Field.Index.UN_TOKENIZED));
					} else if (!(prop instanceof Searchable)) {
						final String value = prop.toString();
						doc.add(new Field(SORTABLE_PREFIX
								+ getFieldname(fieldname, stack), value,
								Field.Store.YES, Field.Index.UN_TOKENIZED));
					}
				} catch (final Exception e) {
					throw new IndexingException("Unable to index bean.", e);
				}
			}
		}

		return doc;
	}

	/**
	 * Add a searchable bean to the index.
	 * 
	 * @param bean Bean to index.
	 * @return Document with this bean added.
	 * @throws IndexException
	 */
	protected Document doAdd(final Searchable bean) throws IndexException {
		// process a Searchable
		final Document doc = createDocument(getType(bean), getId(bean));

		processBean(doc, bean);

		save(doc);

		return doc;
	}

	/**
	 * Convenience method for creating Documents.
	 * 
	 * @param bean Searchable object to create a document based on.
	 * @return Document.
	 * @throws IndexingException
	 */
	protected Document doCreate(final Searchable bean) throws IndexingException {
		return createDocument(getType(bean), getId(bean));
	}

	/**
	 * Delete an object from the index.
	 * 
	 * @param bean Object to delete.
	 * @throws IndexException 
	 */
	protected void doDelete(final Searchable bean) throws IndexException {
		delete(getType(bean), getId(bean));
	}

	/**
	 * Convert a Stack to a fully-qualified field name.
	 * 
	 * @param stack Stack containing parent property names.
	 * @return Fully qualified field name.
	 */
	private String getFieldname(final Stack<String> stack) {
		if (!stack.isEmpty()) {
			final StringBuffer sb = new StringBuffer();
			for (final Iterator<String> i = stack.iterator(); i.hasNext();) {
				final String component = i.next();
				sb.append(component);
				if (i.hasNext())
					sb.append(".");
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	/**
	 * Generate a fully-qualified field name.
	 * 
	 * @param fieldname Property name.
	 * @param stack Stack containing parent property names.
	 * @return Fully qualified field name.
	 */
	private String getFieldname(final String fieldname,
			final Stack<String> stack) {
		if (!stack.isEmpty()) {
			final StringBuffer sb = new StringBuffer(getFieldname(stack));
			sb.append(".").append(fieldname);
			return sb.toString();
		} else {
			return fieldname;
		}
	}

	/**
	 * Searches for a suitable property to use as an id.  Uses the first
	 * property annotated with Searchable.ID.  If none are available, it
	 * falls back to the "id" field (if present).
	 * 
	 * Any properties used as ids must be Serializable.
	 * 
	 * @see AbstractSearcher#getId(Searchable)
	 * 
	 * @param bean Object to reflect on.
	 * @return Bean's id.
	 * @throws IndexingException
	 */
	protected Serializable getId(final Searchable bean)
			throws IndexingException {
		try {
			final Object id = PropertyUtils.getProperty(bean,
					SearchableBeanUtils.getIdPropertyName(bean.getClass()));
			if (id instanceof Serializable) {
				return (Serializable) id;
			} else {
				log.error("The id property for " + bean.getClass()
						+ " must be Serializable.");
				throw new IndexingException(
						"Id properties must be Serializable.");
			}
		} catch (final Exception e) {
			throw new IndexingException("Unable to determine value for id.", e);
		}
	}

	/**
	 * Gets the type of the object being indexed.  If a class has been enhanced
	 * by CGLIB, the base class name is returned.
	 * 
	 * @param bean Object being indexed.
	 * @return Type of the object being indexed.
	 */
	protected String getType(final Searchable bean) {
		if (bean.getClass().getName().contains("$$EnhancerByCGLIB$$")) {
			return bean.getClass().getName().substring(0,
					bean.getClass().getName().indexOf("$$EnhancerByCGLIB$$"));
		} else {
			return bean.getClass().getName();
		}
	}

	/**
	 * Should this property be treated as nested?
	 * 
	 * @param descriptor Property descriptor.
	 * @return Whether this property should be treated as nested.
	 */
	private boolean isNested(final PropertyDescriptor descriptor) {
		for (final Class<? extends Annotation> annotationClass : Searchable.INDEXING_ANNOTATIONS) {
			final Annotation annotation = AnnotationUtils.getAnnotation(
					descriptor.getReadMethod(), annotationClass);
			if (annotation instanceof Indexed) {
				final Indexed i = (Indexed) annotation;
				return i.nested();
			} else if (annotation instanceof Stored) {
				final Stored s = (Stored) annotation;
				return s.nested();
			}
		}

		return false;
	}

	/**
	 * Should this property be treated as a nested Sortable?
	 * 
	 * @param descriptor Property descriptor.
	 * @return Whether this property should be treated as a nested Sortable.
	 */
	private boolean isNestedSortable(final PropertyDescriptor descriptor) {
		final Searchable.Sortable annotation = (Searchable.Sortable) AnnotationUtils
				.getAnnotation(descriptor.getReadMethod(),
						Searchable.Sortable.class);
		if (null != annotation)
			return annotation.nested();

		return false;
	}

	/**
	 * Should the specified property have its term vectors stored in the index?
	 * 
	 * @param descriptor Property descriptor.
	 * @return Whether the specified property should have its term vectors stored.
	 */
	private Field.TermVector isVectorized(final PropertyDescriptor descriptor) {
		final Annotation annotation = AnnotationUtils.getAnnotation(descriptor
				.getReadMethod(), Searchable.Indexed.class);
		if (null != annotation) {
			if (((Searchable.Indexed) annotation).storeTermVector())
				return Field.TermVector.YES;
		}
		return Field.TermVector.NO;
	}

	/**
	 * Process a bean.
	 * 
	 * @param doc Document to add fields to.
	 * @param bean Bean to process.
	 * @return Document with additional fields.
	 * @throws IndexingException
	 */
	protected Document processBean(final Document doc, final Searchable bean)
			throws IndexingException {
		return processBean(doc, bean, new Stack<String>());
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
	private Document processBean(final Document doc, final Searchable bean,
			final Stack<String> stack) throws IndexingException {
		return processBean(doc, bean, stack, Searchable.DEFAULT_BOOST_VALUE);
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
	private Document processBean(final Document doc, final Searchable bean,
			final Stack<String> stack, final float boost)
			throws IndexingException {
		// iterate through fields
		for (final PropertyDescriptor d : PropertyUtils
				.getPropertyDescriptors(bean)) {
			if (SearchableUtils.containsIndexAnnotations(d))
				addBeanFields(doc, bean, d, stack, boost);

			if (SearchableUtils.containsSortableAnnotations(d))
				addSortableFields(doc, bean, d, stack);
		}
		return doc;
	}
}
