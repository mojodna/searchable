/*
 * Copyright 2005-2006 Seth Fitzsimmons <seth@mojodna.net>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.mojodna.searchable.solr;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.mojodna.searchable.IndexSupport;
import net.mojodna.searchable.IndexingException;
import net.mojodna.searchable.Searchable;
import net.mojodna.searchable.SearchableBeanUtils;
import net.mojodna.searchable.Searchable.DefaultFields;
import net.mojodna.searchable.util.AnnotationUtils;
import net.mojodna.searchable.util.SearchableUtils;

import org.apache.lucene.document.Field;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Generates a schema.xml for Solr. Abuses Field in ways it was not meant to be
 * used.
 * 
 * @author Seth Fitzsimmons
 */
public class SchemaGenerator {
    /**
     * Default field to search (field to copy default fields into).
     */
    public static final String DEFAULT_FIELD_NAME = "_text";

    /**
     * Version of Solr's schema format to generate.
     */
    public static final String SCHEMA_VERSION = "1.1";

    /**
     * Generate a Solr schema.xml for the specified classes.
     * 
     * @param name
     *            Config name.
     * @param classes
     *            Classes to generate a schema for.
     * @return XML document containing Solr schema.
     * @throws IndexingException
     */
    public static Document generateSchema(final String name, final Class<? extends Searchable>... classes) throws IndexingException {
        final Document doc = new Document();
        doc.addContent(new Comment("Schema generated at " + new Date() + " for " + Arrays.asList(classes)));
        doc.addContent(new Comment("attribute \"name\" is the name of this schema and is only used for display purposes.\n"
                + "Applications should change this to reflect the nature of the search collection.\n"
                + "version=\"1.1\" is Solr's version number for the schema syntax and semantics.  It should\n" + "not normally be changed by applications.\n"
                + "1.0: multiValued attribute did not exist, all fields are multiValued by nature\n"
                + "1.1: multiValued attribute introduced, false by default"));

        final Element root = new Element("schema");
        doc.setRootElement(root);
        root.setAttribute("name", name);
        root.setAttribute("version", SCHEMA_VERSION);
        root.addContent(new Comment("field type definitions. The \"name\" attribute is\n" + "just a label to be used by field definitions.  The \"class\"\n"
                + "attribute and any other attributes determine the real\n" + "behavior of the fieldtype."));
        root.addContent(makeTypes());

        root.addContent(makeFields(classes));

        root.addContent(new Comment("field to use to determine and enforce document uniqueness."));
        root.addContent(new Element("uniqueKey").addContent(IndexSupport.COMPOUND_ID_FIELD_NAME));

        root.addContent(new Comment("field for the QueryParser to use when an explicit fieldname is absent"));
        root.addContent(new Element("defaultSearchField").addContent(DEFAULT_FIELD_NAME));

        root.addContent(new Comment("SolrQueryParser configuration:\n" + "defaultOperator=\"AND|OR\""));
        root.addContent(new Element("solrQueryParser").setAttribute("defaultOperator", "AND"));

        root.addContent(new Comment("copyField commands copy one field to another at the time a document\n"
                + "is added to the index.  It's used either to index the same field different\n"
                + "ways, or to add multiple fields to the same field for easier/faster searching."));

        Set<String> defaultFields = new HashSet<String>();
        for (final Class<? extends Searchable> clazz : classes) {
            if (AnnotationUtils.isAnnotationPresent(clazz, DefaultFields.class)) {
                // load fields listed as default fields
                defaultFields.addAll(Arrays.asList(SearchableBeanUtils.getDefaultFieldNames(clazz)));
            }
        }

        for (final String fieldName : defaultFields) {
            root.addContent(new Element("copyField").setAttribute("source", fieldName).setAttribute("dest", DEFAULT_FIELD_NAME));
        }

        root.addContent(new Comment("Similarity is the scoring routine for each document vs a query.\n"
                + "A custom similarity may be specified here, but the default is fine\n" + "for most applications."));
        root.addContent(new Comment("<similarity class=\"org.apache.lucene.search.DefaultSimilarity\"/>"));

        return doc;
    }

    private static final String getType(final Class clazz, final String propertyName, boolean sortable) {
        final Class<?> returnType = SearchableUtils.getReturnType(clazz, propertyName);

        if (Boolean.class.isAssignableFrom(returnType) || Boolean.TYPE.isAssignableFrom(returnType)) {
            return "boolean";
        }
        if (Integer.class.isAssignableFrom(returnType) || Integer.TYPE.isAssignableFrom(returnType)) {
            if (sortable) {
                return "sinteger";
            }
            return "integer";
        }
        if (Long.class.isAssignableFrom(returnType) || Long.TYPE.isAssignableFrom(returnType)) {
            if (sortable) {
                return "slong";
            }
            return "long";
        }
        if (Float.class.isAssignableFrom(returnType) || Float.TYPE.isAssignableFrom(returnType)) {
            if (sortable) {
                return "sfloat";
            }
            return "float";
        }
        if (Double.class.isAssignableFrom(returnType) || Double.TYPE.isAssignableFrom(returnType)) {
            if (sortable) {
                return "sdouble";
            }
            return "double";
        }
        if (Date.class.isAssignableFrom(returnType)) {
            // TODO when returning canonical date times, use "date"
            return "string";
        }

        return "text";
    }

    /**
     * Command-line support for generating Solr schema.xml files.
     * 
     * @param args
     */
    public static void main(final String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -cp <class search path of directories and zip/jar files> -jar searchable.jar schema_name class [class...]");
            System.exit(1);
        }

        final String name = args[0];
        final Class[] classes = new Class[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            try {
                classes[i - 1] = Class.forName(args[i]);
            } catch (final ClassNotFoundException e) {
                System.err.println("Could not find class: " + args[i]);
                System.exit(1);
            }
        }

        try {
            final XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            out.output(generateSchema(name, classes), System.out);
        } catch (final Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static Element makeFields(final Class<? extends Searchable>[] classes) throws IndexingException {
        final Map<String, Field> fieldMap = new HashMap<String, Field>();

        for (Class<? extends Searchable> clazz : classes) {
            final Field[] fields = SearchableUtils.getFields(clazz);
            for (Field field : fields) {
                if (fieldMap.containsValue(field)) {
                    throw new IndexingException("Conflicting field names exist: " + field.name());
                }
                fieldMap.put(field.name(), field);
            }
        }

        final Element fields = new Element("fields");
        fields.addContent(new Element("field").setAttribute("name", DEFAULT_FIELD_NAME).setAttribute("type", "text").setAttribute("indexed", "true")
                .setAttribute("stored", "false").setAttribute("multiValued", "true"));
        fields.addContent(new Element("field").setAttribute("name", IndexSupport.COMPOUND_ID_FIELD_NAME).setAttribute("type", "string").setAttribute("indexed",
                "true").setAttribute("stored", "true"));
        fields.addContent(new Element("field").setAttribute("name", IndexSupport.ID_FIELD_NAME).setAttribute("type", "string").setAttribute("indexed", "true")
                .setAttribute("stored", "true"));
        fields.addContent(new Element("field").setAttribute("name", IndexSupport.ID_TYPE_FIELD_NAME).setAttribute("type", "string").setAttribute("indexed",
                "true").setAttribute("stored", "true"));
        fields.addContent(new Element("field").setAttribute("name", IndexSupport.TYPE_FIELD_NAME).setAttribute("type", "string")
                .setAttribute("indexed", "true").setAttribute("stored", "true"));

        for (final Field field : fieldMap.values()) {
            if (field.name().startsWith(IndexSupport.SORTABLE_PREFIX)) {
                continue;
            }

            final String[] propertyInfo = field.stringValue().split("#");
            final Class<?> clazz;
            try {
                clazz = Class.forName(propertyInfo[0]);
            } catch (final ClassNotFoundException e) {
                throw new IndexingException(e);
            }
            final String propertyName = propertyInfo[1];
            final boolean sortable = fieldMap.containsKey(IndexSupport.SORTABLE_PREFIX + field.name());

            final Element fieldElement = new Element("field");
            fieldElement.setAttribute("name", field.name());
            fieldElement.setAttribute("type", getType(clazz, propertyName, sortable));

            fieldElement.setAttribute("indexed", Boolean.valueOf(field.isIndexed()).toString());
            fieldElement.setAttribute("stored", Boolean.valueOf(field.isStored() || sortable).toString());
            fieldElement.setAttribute("multiValued", Boolean.valueOf(SearchableUtils.isMultiValued(clazz, propertyName)).toString());
            fields.addContent(fieldElement);
        }

        return fields;
    }

    private static Element makeFieldtype(final String name, final String clazz) {
        return makeFieldtype(name, clazz, null);
    }

    private static Element makeFieldtype(final String name, final String clazz, final Boolean sortMissingLast) {
        final Element fieldtype = new Element("fieldtype");
        fieldtype.setAttribute("name", name);
        fieldtype.setAttribute("class", clazz);
        if (null != sortMissingLast) {
            fieldtype.setAttribute("sortMissingLast", sortMissingLast.toString());
        }
        return fieldtype;
    }

    private static Element makeTypes() {
        final Element types = new Element("types");
        types.addContent(new Comment("The StrField type is not analyzed, but indexed/stored verbatim.\n"
                + "- StrField and TextField support an optional compressThreshold which\n"
                + "limits compression (if enabled in the derived fields) to values which\n" + "exceed a certain size (in characters)."));
        types.addContent(makeFieldtype("string", "solr.StrField", true));

        types.addContent(new Comment("boolean type: \"true\" or \"false\""));
        types.addContent(makeFieldtype("boolean", "solr.BoolField", true));

        types.addContent(new Comment("The optional sortMissingLast and sortMissingFirst attributes are\n"
                + "currently supported on types that are sorted internally as a strings.\n"
                + "- If sortMissingLast=\"true\" then a sort on this field will cause documents\n"
                + "without the field to come after documents with the field,\n" + "regardless of the requested sort order (asc or desc).\n"
                + "- If sortMissingFirst=\"true\" then a sort on this field will cause documents\n"
                + "without the field to come before documents with the field,\n" + "regardless of the requested sort order.\n"
                + "- If sortMissingLast=\"false\" and sortMissingFirst=\"false\" (the default),\n"
                + "then default lucene sorting will be used which places docs without the field\n"
                + "first in an ascending sort and last in a descending sort."));
        types.addContent(new Comment("numeric field types that store and index the text\n"
                + "value verbatim (and hence don't support range queries since the\n" + "lexicographic ordering isn't equal to the numeric ordering)"));
        types.addContent(makeFieldtype("integer", "solr.IntField"));
        types.addContent(makeFieldtype("long", "solr.LongField"));
        types.addContent(makeFieldtype("float", "solr.FloatField"));
        types.addContent(makeFieldtype("double", "solr.DoubleField"));

        types.addContent(new Comment("Numeric field types that manipulate the value into\n"
                + "a string value that isn't human readable in it's internal form,\n" + "but with a lexicographic ordering the same as the numeric ordering\n"
                + "so that range queries correctly work."));
        types.addContent(makeFieldtype("sint", "solr.SortableIntField", true));
        types.addContent(makeFieldtype("slong", "solr.SortableLongField", true));
        types.addContent(makeFieldtype("sfloat", "solr.SortableFloatField", true));
        types.addContent(makeFieldtype("sdouble", "solr.SortableDoubleField", true));

        types.addContent(new Comment("The format for this date field is of the form 1995-12-31T23:59:59Z, and\n"
                + "is a more restricted form of the canonical representation of dateTime\n" + "http://www.w3.org/TR/xmlschema-2/#dateTime\n"
                + "The trailing \"Z\" designates UTC time and is mandatory.\n" + "Optional fractional seconds are allowed: 1995-12-31T23:59:59.999Z\n"
                + "All other components are mandatory."));
        types.addContent(makeFieldtype("date", "solr.DateField", true));

        types.addContent(new Comment("solr.TextField allows the specification of custom text analyzers\n"
                + "specified as a tokenizer and a list of token filters. Different\n" + "analyzers may be specified for indexing and querying.\n\n"
                + "The optional positionIncrementGap puts space between multiple fields of\n"
                + "this type on the same document, with the purpose of preventing false phrase\n" + "matching across fields.\n\n"
                + "For more info on customizing your analyzer chain, please see...\n" + " http://wiki.apache.org/solr/AnalyzersTokenizersTokenFilters\n"));
        types.addContent(new Comment("Standard analyzer commonly used by Lucene developers"));
        Element textLu = makeFieldtype("text_lu", "solr.TextField").setAttribute("positionIncrementGap", "100");
        types.addContent(textLu);
        Element analyzer = new Element("analyzer");
        textLu.addContent(analyzer);
        analyzer.addContent(new Element("tokenizer").setAttribute("class", "solr.StandardTokenizerFactory"));
        analyzer.addContent(new Element("filter").setAttribute("class", "solr.StandardFilterFactory")).addContent(
                new Element("filter").setAttribute("class", "solr.LowerCaseFilterFactory")).addContent(
                new Element("filter").setAttribute("class", "solr.StopFilterFactory")).addContent(
                new Element("filter").setAttribute("class", "solr.EnglishPorterFilterFactory")).addContent(
                new Element("filter").setAttribute("class", "solr.RemoveDuplicatesTokenFilterFactory"));

        types.addContent(new Comment("One could also specify an existing Analyzer class that has a\n"
                + "default constructor via the class attribute on the analyzer element\n" + "<fieldtype name=\"text_lu\" class=\"solr.TextField\">\n"
                + "<analyzer class=\"org.apache.lucene.analysis.el.GreekAnalyzer\"/>\n" + "</fieldType>"));

        types.addContent(new Comment("A text field that only splits on whitespace for more exact matching"));
        types.addContent(makeFieldtype("text_ws", "solr.TextField").setAttribute("positionIncrementGap", "100").addContent(
                new Element("analyzer").addContent(new Element("tokenizer").setAttribute("class", "solr.WhitespaceTokenizerFactory"))));

        types.addContent(new Comment("A text field that uses WordDelimiterFilter to enable splitting and matching of\n"
                + "words on case-change, alpha numeric boundaries, and non-alphanumeric chars\n"
                + "so that a query of \"wifi\" or \"wi fi\" could match a document containing \"Wi-Fi\".\n"
                + "Synonyms and stopwords are customized by external files, and stemming is enabled\n"
                + "Duplicate tokens at the same position (which may result from Stemmed Synonyms or\n" + "WordDelim parts) are removed."));
        types.addContent(makeFieldtype("text", "solr.TextField").setAttribute("positionIncrementGap", "100").addContent(
                new Element("analyzer").setAttribute("type", "index").addContent(
                        new Element("tokenizer").setAttribute("class", "solr.WhitespaceTokenizerFactory")).addContent(
                        new Comment("in this example, we will only use synonyms at query time\n"
                                + "<filter class=\"solr.SynonymFilterFactory\" synonyms=\"index_synonyms.txt\" ignoreCase=\"true\" expand=\"false\"/>"))
                        .addContent(new Element("filter").setAttribute("class", "solr.StopFilterFactory").setAttribute("ignoreCase", "true")).addContent(
                                new Element("filter").setAttribute("class", "solr.WordDelimiterFilterFactory").setAttribute("generateWordParts", "1")
                                        .setAttribute("generateNumberParts", "1").setAttribute("catenateWords", "1").setAttribute("catenateNumbers", "1")
                                        .setAttribute("catenateAll", "0")).addContent(
                                new Element("filter").setAttribute("class", "solr.LowerCaseFilterFactory")).addContent(
                                new Element("filter").setAttribute("class", "solr.EnglishPorterFilterFactory").setAttribute("protected", "protwords.txt"))
                        .addContent(new Element("filter").setAttribute("class", "solr.RemoveDuplicatesTokenFilterFactory"))).addContent(
                new Element("analyzer").setAttribute("type", "query").addContent(
                        new Element("tokenizer").setAttribute("class", "solr.WhitespaceTokenizerFactory")).addContent(
                        new Element("filter").setAttribute("class", "solr.SynonymFilterFactory").setAttribute("synonyms", "synonyms.txt").setAttribute(
                                "ignoreCase", "true").setAttribute("expand", "true")).addContent(
                        new Element("filter").setAttribute("class", "solr.StopFilterFactory").setAttribute("ignoreCase", "true")).addContent(
                        new Element("filter").setAttribute("class", "solr.WordDelimiterFilterFactory").setAttribute("generateWordParts", "1").setAttribute(
                                "generateNumberParts", "1").setAttribute("catenateWords", "1").setAttribute("catenateNumbers", "1").setAttribute("catenateAll",
                                "0")).addContent(new Element("filter").setAttribute("class", "solr.LowerCaseFilterFactory")).addContent(
                        new Element("filter").setAttribute("class", "solr.EnglishPorterFilterFactory").setAttribute("protected", "protwords.txt")).addContent(
                        new Element("filter").setAttribute("class", "solr.RemoveDuplicatesTokenFilterFactory"))));

        types.addContent(new Comment("Less flexible matching, but less false matches.  Probably not ideal for product names\n"
                + "but may be good for SKUs.  Can insert dashes in the wrong place and still match."));
        types.addContent(new Element("fieldtype").setAttribute("name", "textTight").setAttribute("class", "solr.TextField").setAttribute(
                "positionIncrementGap", "100").addContent(
                new Element("analyzer").addContent(new Element("tokenizer").setAttribute("class", "solr.WhitespaceTokenizerFactory")).addContent(
                        new Element("filter").setAttribute("class", "solr.SynonymFilterFactory").setAttribute("synonyms", "synonyms.txt").setAttribute(
                                "ignoreCase", "true").setAttribute("expand", "false")).addContent(
                        new Element("filter").setAttribute("class", "solr.StopFilterFactory").setAttribute("ignoreCase", "true")).addContent(
                        new Element("filter").setAttribute("class", "solr.WordDelimiterFilterFactory").setAttribute("generateWordParts", "0").setAttribute(
                                "generateNumberParts", "0").setAttribute("catenateWords", "1").setAttribute("catenateNumbers", "1").setAttribute("catenateAll",
                                "0")).addContent(new Element("filter").setAttribute("class", "solr.LowerCaseFilterFactory")).addContent(
                        new Element("filter").setAttribute("class", "solr.EnglishPorterFilterFactory").setAttribute("protected", "protwords.txt")).addContent(
                        new Element("filter").setAttribute("class", "solr.RemoveDuplicatesTokenFilterFactory"))));
        return types;
    }
}
