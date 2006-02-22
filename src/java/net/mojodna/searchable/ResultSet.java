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

import java.util.Iterator;
import java.util.List;

import org.apache.lucene.search.Query;

/**
 * Represents a result set returned by a specific query.  This provides
 * additional data about the number of results present (which may differ from
 * the size of the underlying collection.
 * 
 * Implement to suit yourself.  Implementing as a PaginatedListAdapter for
 * displaytag (http://displaytag.sf.net/) would be convenient.
 * 
 * @author Seth Fitzsimmons
 */
public interface ResultSet<E extends Result> extends Iterable<E> {
    /**
     * Number of results in this set.
     * 
     * @return Number of results in this set.
     */
	public int count();
    
    /**
     * Offset of first result in this set.
     * 
     * @return Offset of first result in this set.
     */
	public int offset();
    
    /**
     * Gets the Query that was used to generate this set.
     * 
     * @return Query.
     */
    public Query getQuery();
    
    /**
     * Gets the list of results present in this set.
     * 
     * @return List of results.
     */
	public List<E> getResults();
    
    /**
     * Total number of results available.
     * 
     * @return Total number of results available.
     */
	public int size();
    
    /**
     * Is this empty?
     * 
     * @return Whether this resultset is empty.
     */
	public boolean isEmpty();
    
    /**
     * Gets an iterator over this resultset.
     * 
     * @return Iterator over this resultset.
     */
	public Iterator<E> iterator();
}
