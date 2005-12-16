/*
Copyright 2005      Seth Fitzsimmons <seth@prx.org>

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

public class ResultSetImpl implements ResultSet {
	private int offset;
	private List<Result> results = new LinkedList();
	private int size;
	
	public ResultSetImpl() {
		super();
	}
	
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
	
	public void setOffset(final int offset) {
		this.offset = offset;
	}
	
	public List<Result> getResults() {
		return results;
	}
	
    public void add(final Result result) {
        results.add( result );
    }
    
	public void setResults(final List<Result> results) {
		this.results = results;
	}
	
	public int size() {
		return size;
	}
	
	public void setSize(final int size) {
		this.size = size;
	}
	
	public boolean isEmpty() {
		return results.isEmpty();
	}
	
	public Iterator<Result> iterator() {
		return results.iterator();
	}
}
