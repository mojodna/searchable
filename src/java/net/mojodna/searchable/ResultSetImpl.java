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
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.search.Query;

/**
 * Basic implementation of a ResultSet.
 * 
 * @author Seth Fitzsimmons
 */
public class ResultSetImpl implements ResultSet {
	private int offset;
    private Query query;
	private List<Result> results = new LinkedList<Result>();
	private int size;
	
    /**
     * Basic constructor.
     */
	public ResultSetImpl() {
		super();
	}
	
    /**
     * Construct with a specified size.
     * 
     * @param size Total number of results available.
     */
	public ResultSetImpl(final int size) {
		super();
		setSize( size );
	}
	
	public int count() {
		return results.size();
	}
	
	public int offset() {
		return offset;
	}
	
    /**
     * Sets the offset of the first result in this set.
     * 
     * @param offset Offset of the first result in this set.
     */
	public void setOffset(final int offset) {
		this.offset = offset;
	}
    
    public Query getQuery() {
        return query;
    }
    
    /**
     * Sets the Query that was used to get this set.
     * 
     * @param query Query used.
     */
    public void setQuery(final Query query) {
        this.query = query;
    }
	
    /**
     * Adds a result to the end of the resultset.  This is convenient for
     * constructing ResultSets on the fly.
     * 
     * @param result Result to add.
     */
    public void add(final Result result) {
        results.add( result );
    }
    
    /**
     * Replaces an existing Result with something presumably equivalent.
     * Result-specific properties are copied between objects as part of this
     * process.
     * 
     * @param r1 Result to replace.
     * @param r2 Replacement result.
     */
    public void replace(final Result r1, final Result r2) {
        r2.setRanking( r1.getRanking() );
        r2.setScore( r1.getScore() );
        r2.setSearchExtract( r1.getSearchExtract() );
        r2.setStoredFields( r1.getStoredFields() );
        results.set( results.indexOf( r1 ), r2 );
    }
    
	public List<? extends Result> getResults() {
		return results;
	}
	
    /**
     * Sets the available results in this set.
     * 
     * @param results Available results.
     */
	public void setResults(final List<Result> results) {
		this.results = results;
	}
	
	public int size() {
		return size;
	}
	
    /**
     * Sets the total number of available results.
     * 
     * @param size Total number of results available.
     */
	public void setSize(final int size) {
		this.size = size;
	}
	
	public boolean isEmpty() {
		return results.isEmpty();
	}
	
	public Iterator<? extends Result> iterator() {
		return results.iterator();
	}
}
