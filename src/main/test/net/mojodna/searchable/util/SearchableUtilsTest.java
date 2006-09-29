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

import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;
import net.mojodna.searchable.AbstractResult;
import net.mojodna.searchable.IndexSupport;
import net.mojodna.searchable.Searchable;
import net.mojodna.searchable.Searchable.DefaultFields;
import net.mojodna.searchable.solr.SchemaGenerator;

import org.apache.lucene.document.Field;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Tests for SearchableUtils.
 *
 * @author Seth Fitzsimmons
 */
public class SearchableUtilsTest extends TestCase {
	/**
	 */
	private class SearchableExample1 extends AbstractResult implements
			Searchable {
		/**
		 * @return Name
		 */
		@Indexed
		public String getName() {
			return getClass().getName();
		}
	}

	/**
	 */
	private class SearchableExample2 extends AbstractResult implements
			Searchable {
		/**
		 * @return Name
		 */
		@Sortable
		public String getName() {
			return getClass().getName();
		}
	}

	/**
	 */
	@DefaultFields("name")
	private class SearchableExample3 extends AbstractResult implements
			Searchable {
		/**
		 * @return Name
		 */
		@Indexed(boost = 3.0F)
		@Sortable
		public String getName() {
			return getClass().getName();
		}

		/**
		 * @return Nicknames.
		 */
		public String[] getNicknames() {
			return null;
		}

		/**
		 * @return Other names.
		 */
		@Indexed
		public Collection<String> getOtherNames() {
			return Collections.emptyList();
		}

		/**
		 * @return Favorite numbers.
		 */
		@Indexed
		public int[] getFavoriteNumbers() {
			return null;
		}
	}

	/**
	 * @throws Exception
	 */
	public void testGetIndexedFields() throws Exception {
		final Field[] fields = SearchableUtils
				.getFields(SearchableExample1.class);
		assertEquals(1, fields.length);
		assertEquals("name", fields[0].name());
		assertEquals(Searchable.DEFAULT_BOOST_VALUE, fields[0].getBoost());
		assertFalse(fields[0].isStored());
		assertTrue(fields[0].isIndexed());
		assertTrue(fields[0].isTokenized());
	}

	/**
	 * @throws Exception
	 */
	public void testGetFieldNames() throws Exception {
		final String[] fieldNames = SearchableUtils
				.getFieldNames(SearchableExample1.class);
		assertEquals(1, fieldNames.length);
		assertEquals("name", fieldNames[0]);
	}

	/**
	 * @throws Exception
	 */
	public void testGetSortableFields() throws Exception {
		final Field[] fields = SearchableUtils
				.getFields(SearchableExample2.class);
		assertEquals(1, fields.length);
		assertEquals(IndexSupport.SORTABLE_PREFIX + "name", fields[0].name());
		assertTrue(fields[0].isStored());
		assertFalse(fields[0].isIndexed());
		assertFalse(fields[0].isTokenized());
	}

	/**
	 * @throws Exception
	 */
	public void testGetFields() throws Exception {
		final Field[] fields = SearchableUtils
				.getFields(SearchableExample3.class);
		assertEquals(2, fields.length);

		for (Field field : fields) {
			if (field.name().equals("name")) {
				// @Indexed
				assertEquals("name", field.name());
				assertEquals(3.0F, field.getBoost());
				assertFalse(field.isStored());
				assertTrue(field.isIndexed());
				assertTrue(field.isTokenized());
			} else {
				// @Sortable
				assertEquals(IndexSupport.SORTABLE_PREFIX + "name", field
						.name());
				assertTrue(field.isStored());
				assertFalse(field.isIndexed());
				assertFalse(field.isTokenized());
			}
		}
	}

	/**
	 * @throws Exception
	 */
	public void testIsMultiValued() throws Exception {
		assertFalse(SearchableUtils.isMultiValued(SearchableExample3.class,
				"name"));
		assertTrue(SearchableUtils.isMultiValued(SearchableExample3.class,
				"nicknames"));
		assertTrue(SearchableUtils.isMultiValued(SearchableExample3.class,
				"otherNames"));
		assertTrue(SearchableUtils.isMultiValued(SearchableExample3.class,
				"favoriteNumbers"));
	}

	/**
	 * @throws Exception
	 */
	public void testGenerateSchema() throws Exception {
		XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
		out.output(SchemaGenerator.generateSchema("test",
				SearchableExample1.class, SearchableExample2.class,
				SearchableExample3.class), System.out);
	}
}
