Searchable
==========

Annotation-Driven Indexing and Searching with Lucene
----------------------------------------------------

### Overview
_Searchable is a toolkit for Lucene that harnesses the power of annotations to specify what properties to index and how to treat them._

### Basics
Searchable requires JDK 1.5 and Lucene 1.9+.

At its core, Searchable provides a set of annotations that can be used to instruct its indexer how to deal with properties present in an annotated bean.  It can also be used without the annotations (by writing a custom **Indexer**/**Searcher** combination).  However, Searchable really shines when custom **Indexers** and **Searchers** are combined with the annotations.

Please see the examples below for pointers on usage.

### Annotations
In order for the annotations to have any effect, they must annotate a class that extends **Searchable**.  With the exception of @DefaultFields, they must all be placed on the reader method of a property (`getXXX()`).  Unlike traditional Annotation behavior, they may be placed on an interface method or a method that is overridden.

#### @ID

This allows a property other than _id_ to be specified as the id field.  There should only be one per class hierarchy.

e.g.:
	
	@ID
	public Integer getKey() { ... }
	

#### @Indexed

This specifies that a field should be indexed.  The following attributes are available:

* aliases - Array of aliases to also use as field names.  Default: none.
* boost - Boost factor (as a float) for this field.  Default: 1.0.
* name - Field name to use for this property.  Default: property name.
* nested - Whether to index this field in a nested context (i.e. a _Searchable_ as a property of another _Searchable_).  Default: false.
* stored - Store this property in the index.  Default: false.
* storeTermVector - Store term vectors for this field.  Default: false.
* tokenized - Tokenize the value of this property before adding it to the index.  Default: true.

e.g.:
	
	@Indexed(boost=2.0F)
	public String getName() { ... }
	

#### @Stored

This specifies that a field should be stored in the index.  In typical usage, this should not be necessary, although you may find that it comes in hand from time to time.  The following attributes are available:

* aliases - Array of aliases to also use as field names.  Default: none.
* name - Field name to use for this property.  Default: property name.
* nested - Whether to store this field in a nested context (i.e. a _Searchable_ as a property of another _Searchable_).  Default: false.

e.g.:
	
	@Stored
	public String getDescription() { ... }

#### @Sortable

This specifies that a custom Keyword field should be created to use for sorting results in Lucene.  The following attribute is available:

* nested - Whether to create a sorted field for this property in a nested context (i.e. a _Searchable_ as a property of another _Searchable_).  Default: false.

e.g.:
	
	@Indexed
	@Sortable
	public String getName() { ... }
	

#### @DefaultFields

This contains an array of field names that should be used as the default list.  If this annotation is not present and no fields are specified when searching, the default is to use all fields present in the index.

Used during the search process.

e.g.:
	
	@DefaultFields( { "name", "address.city" } )
	public class Person implements Searchable { ... }
	

#### @Excerptable

This specifies that the annotated property should be used when creating a search extract.  **NOTE**: The resultant object must be hydrated **in your code** before it can be excerpted.  In many cases, this involves reloading the object from Hibernate Session by using _session.load()_.

e.g.:
	
	@Indexed
	@Excerptable
	public String getDescription() { ... }

### Extension Points

_AbstractSearcher_, _AbstractMultiSearcher_, _AbstractIndexer_, and _AbstractBeanIndexer_ are provided as abstract base classes with the majority of necessary functionality provided as protected methods.  _AbstractSearcher_ exposes multiple signatures for certain methods that allow alternate implementations of Lucene _IndexReader_s and _Searcher_s.  _AbstractMultiSearcher_ makes use of these to implement cross-index searching (an example is provided below), but a similar approach could be used to implement remote searching.

`setAnalyzer(Analyzer)` and `setIndexPath(String)` should be used to override the default behavior of Searchable.  Both are best called in your constructor.

`getIndexReader()`, `getIndexModifier()`, and `getIndexSearcher()` provide shared access to _IndexReader_s, _IndexModifier_s, and _IndexSearcher_s over the index specified using `setIndexPath(String)`.  In certain circumstances, you may wish to override these methods to provide alternate implementations (a _MultiSearcher_ for example; if you wish to provide a _RemoteSearchable_, you must define an additional method, as `getIndexSearcher()` returns a _Searcher_, not a _Searchable_).

_Searcher_ and _Indexer_ are provided as interfaces that may be extended to expose additional functionality to your application in a generic fashion.

_Result_ and _ResultImpl_ have been split into an interface and an implementation in order to hide the `add(Result)` and `replace(Result, Result)` methods as well as to provide additional flexibility for alternate implementations.  One such alternate implementation would also implement [DisplayTag](http://displaytag.sf.net/)'s _PaginatedList_.

_AbstractResult_ is made available as a base class suitable for extension by objects that implement _Result_ and are not required to extend anything else.

### Batch Indexing

_BatchIndexer_ extends the _Indexer_ interface by introducing three methods: `flush()` (to be implemented by the indexer), `setBatchMode(boolean)`, and `isBatchMode()`, of which the latter two are provided by the indexing infrastructure.  `flush()` is executed during `close()` immediately before the index is optimized.  A typical implementation calls `flushDeletes()`. which flushes any document deletions that had previously been queued (rather than flushing them immediately, as in a non-batch indexer).  The hybrid example below demonstrates a _BatchIndexer_ in action.

### Limitations

_AbstractSearcher_ does not yet support default field arrays as arguments to the various `doSearch()` methods.

The @DefaultFields and @Excerptable annotations are only available on objects that implement _Searchable_.  Ideally, they would be available for any @Result, as there's nothing that necessarily limits their use to _Searchable_s.

Fields that are stored in the index are returned in a storedFields Map (specified in the _Result_ interface) regardless of whether or not the names correspond to properties on the object being returned.  In the future, Lucene could be used as a persistence tool of sorts by reconstituting the object as much as possible.  Thus, if all properties of an object were indexed (and stored), the object could be fully reconstituted and no external persistence mechanism would be necessary.  The trade-off is index size, so this would not be feasible for large datasets.

### Examples

#### Simple Example

This example demonstrates how to use Searchable without using the annotations.

_AddressIndexer.java:_
	
	/**
	 * Indexes Addresses.
	 */
	public class AddressIndexer extends AbstractIndexer implements Indexer {
		public AddressIndexer() {
			// use /tmp/addresses as the index path
			setIndexPath("/tmp/addresses");
		}
		
		public void add(Address address) throws IndexingException {
			// create a document with "address" as the type
			Document doc = createDocument( "address", address.getId() );

			// add fields for each property of Address
			// implementation of Address is left to your imagination
			doc.add( Field.UnStored("street", address.getStreet() ) );
			doc.add( Field.UnStored("city", address.getCity() ) );
			doc.add( Field.UnStored("state", address.getState() ) );
			doc.add( Field.UnStored("zip", address.getZip() ) );
			
			// save the document to the index
			save( doc );
		}
		
		public void delete(Address address) throws IndexingException {
			delete( "address", address.getId() );
		}
	}
	
_AddressSearcher.java:_
	
	/**
	 * Searches Addresses.
	 */
	public class AddressSearcher extends AbstractSearcher implements Searcher {
		public AddressSearcher() {
			// use /tmp/addresses as the index path
			setIndexPath("/tmp/addresses");
		}
		
		public ResultSet search(String query) throws SearchException {
			return doSearch( query );
		}
	}
	
_AddressIndexTest.java:_
	
	// ...
	public void test() throws Exception {
		AddressIndexer indexer = new AddressIndexer();
		indexer.add( makeAddress() );
		indexer.close();
		
		AddressSearcher searcher = new AddressSearcher();
		ResultSet rs = searcher.search("city:Cambridge");
		Result result = rs.iterator().next();
		// Address does not implement Result, nor was it indexed with its
		// fully qualified class name, so the result is a GenericResult
		assertTrue( result instanceof GenericResult );
		GenericResult gr = (GenericResult) result;
		assertEquals( "address", gr.getType() );
	}
	

#### Simple Annotation-Driven Example

This example demonstrates the basics of annotation-driven indexing.

_SearchableBean.java:_
	
	/**
	 * Implementation of Searchable to be indexed.
	 *
	 * Extends AbstractResult to avoid needing to implement Result methods
	 * (inherited from Searchable).
	 */
	public class SearchableBean extends AbstractResult implements Searchable {
		private Integer id;
		private String name;
		
		public SearchableBean(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
		
		public Integer getId() {
			return id;
		}
		
		public void setId(Integer id) {
			this.id = id;
		}
		
		@Indexed
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
	}
	
_IndexManager.java:_
	
	// ...
	public void index(SearchableBean bean) throws IndexingException {
		BeanIndexer bi = new BeanIndexer();
		bi.add( bean );
		bi.close();
	}
	
	public ResultSet search(String query) throws SearchException {
		BeanSearcher bs = new BeanSearcher();
		// searching will attempt to reconstitute objects and set their ids
		// this allows the calling layer to know a) the type and b) the id
		// in order to refresh it properly
		return bs.search( query );
	}
	
_IndexManagerTest.java:_
	
	// ...
	public void testSearch() throws Exception {
		IndexManager im = new IndexManager();
		im.index( new SearchableBean() );
		ResultSet rs = im.search("name:seth");
		assertEquals( 1, rs.size() );
		Result result = rs.iterator().next();
		assertTrue( result instanceof SearchableBean );
		assertEquals( 1, result.getId() );
		// name has not been set
		assertNull( result.getName() );
	}
	
#### Hybrid Example

This example demonstrates how to use Searchable in a hybrid mode.  _TeapotIndexer_ can operate in batch mode.

_Teapot.java:_
	
	/**
	 * A representation of a teapot.  When searching the Teapot index, only the
	 * "name" and "description" fields will be searched by default (if
	 * @DefaultFields were not specified, all indexed fields would be searched).
	 */
	@DefaultFields( {"name", "description" } )
	public interface Teapot extends Searchable {
		/**
		 * Name of the Teapot.  This serves as the id, is indexed, and can be
		 * sorted by.  This is the only field that is indexed when a Teapot is
		 * nested in another object (a TeaSet, for example).
		 */
		@ID
		@Indexed
		@Sortable
		public String getName();
		
		/**
		 * Color of the Teapot.  Aliased to "colour" for Brits.
		 */
		@Indexed(aliases="colour", nested=false)
		public String getColor();
		
		/**
		 * Material the Teapot is made of.  Indexed and stored.
		@Index(stored=true, nested=false)
		public String getMaterial();
		
		/**
		 * The type of tea this Teapot contains.  Not searchable, but stored.
		 */
		@Stored(nested=false)
		public String getTeaType();
		
		/**
		 * Description.  Indexed and used when creating an excerpt.
		 */
		@Indexed(nested=false)
		@Excerptable
		public String getDescription();
	}

_TeapotIndexer.java:_

	/**
	 * Indexes teapots.
	 */
	public class TeapotIndexer extends AbstractBeanIndexer implements BatchIndexer<Teapot> {
		public TeapotIndexer() {
			// use /tmp/teapots as the index path
			setIndexPath("/tmp/teapots");
		}

		public void add(Teapot tp) throws IndexingException {
			Document doc = doCreate( tp );

			processBean( tp );
			postProcessTeapot( tp );

			// save the document to the index
			save( doc );
		}

		public void delete(Teapot tp) throws IndexingException {
			doDelete( tp );
		}
		
		public void flush() throws IndexException {
			// flush any pending deletes
			flushDeletes();
		}
		
		protected Document postProcessTeapot(Teapot tp) {
			// add an "owner" field not specified by the teapot
			doc.add( Field.UnStored("owner", "Nathan") );
		}
	}

_TeapotSearcher.java:_

	/**
	 * Searches teapots.
	 */
	public class TeapotSearcher extends AbstractSearcher implements Searcher<Teapot> {
		public TeapotSearcher() {
			// use /tmp/teapots as the index path
			setIndexPath("/tmp/teapots");
		}

		public ResultSet<Teapot> search(String query) throws SearchException {
			return excerpt( refresh( doSearch( query ) ) );
		}
		
		protected ResultSet<Teapot> load(ResultSet<Teapot> results) {
			ResultSetImpl rs = (ResultSetImpl) results;
			for (Teapot tp : results) {
				// session is a HibernateSession
				// your actual implementation may involve a DAO
				// replaces stub Teapot with a fully loaded one
				// ResultSetImpl handles transferring of Result properties
				rs.replace( tp, (Teapot) session.load( tp ) );
			}
			return results;
		}
	}

_TeapotTest.java:_

	// ...
	public void test() throws Exception {
		BatchIndexer<Teapot> indexer = new TeapotIndexer();
		// run TeapotIndexer in batch mode
		indexer.setBatchMode( true );
		indexer.add( makeTeapot() );
		indexer.close();

		Searcher<Teapot> searcher = new TeapotSearcher();
		ResultSet<Teapot> rs = searcher.search("material:china OR wireframe");
		Iterator<Teapot> i = rs.iterator();
		Teapot firstResult = i.next();
		Teapot secondResult = i.next();
		assertEquals( "china", firstResult.getMaterial() );
		assertTrue( secondResult.getDescription().contains("wireframe") );
		assertTrue( secondResult.getSearchExtract().contains("wireframe") );
	}

#### Searching Multiple Indexes Example

This example demonstrates how to use Searchable to search multiple indexes for different types of objects.  This assumes a 1 class/index breakdown, but that's not strictly necessary.

_AddressAndTeapotSearcher.java:_

	/**
	 * Searches Addresses and Teapots.
	 */
	public class AddressAndTeapotSearcher extends AbstractMultiSearcher implements Searcher {
		/**
		 * Constructs this as a MultiSearcher for Addresses and Teapots.  The order and length
		 * of both arrays must be equivalent.
		 */
		public AddressAndTeapotSearcher() {
			super( new String[] { "/tmp/addresses", "/tmp/teapots"},
				   new Class[] { Address.class, Teapot.class } );
		}
		
		/**
		 * Searches the teapot index using default fields determined by @DefaultFields
		 * annotation on the Teapot.  Also searches the address index using all available
		 * fields as defaults.  If Address implemented Searchable and contained a
		 * @DefaultField annotation, those fields would be used instead.
		 */
		public ResultSet search(String query) throws SearchException {
			return doSearch( query );
		}
	}
