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
package net.mojodna.searchable.solr;

import java.util.Enumeration;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;

/**
 * Converts Lucene Documents to XML fragments suitable for POSTing to Solr.
 * 
 * @author Seth Fitzsimmons
 */
public class DocumentConverter {
	/**
	 * Version of Solr's schema format to generate.
	 */
	public static final String SCHEMA_VERSION = "1.1";

	/**
	 * Converts a Document to an XML Element.
	 * 
	 * @param doc Document to convert.
	 * @return XML Element.
	 */
	public static Element convert(final Document doc) {
		final Element root = new Element("doc");

		final Enumeration fields = doc.fields();
		while (fields.hasMoreElements()) {
			final Field f = (Field) fields.nextElement();
			final Element node = new Element("field");
			node.setAttribute("name", f.name());
			node.setContent(new Text(f.stringValue()));
			root.addContent(node);
		}

		return root;
	}

	/**
	 * Converts a Document to an XML fragment.
	 * 
	 * @param doc Document to convert.
	 * @return XML fragment.
	 */
	public static String convertToString(final Document doc) {
		final XMLOutputter out = new XMLOutputter();
		return out.outputString(convert(doc));
	}
}
