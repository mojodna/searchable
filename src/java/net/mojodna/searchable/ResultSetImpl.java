package net.mojodna.searchable;

import java.util.Collection;
import java.util.Iterator;

public class ResultSetImpl implements ResultSet {
	private int count;
	private int offset;
	private Collection<Result> results;
	private int size;
	
	ResultSetImpl() {
		super();
	}
	
	ResultSetImpl(final int size) {
		super();
		setSize( size );
	}
	
	public int count() {
		return count;
	}
	
	void setCount(final int count) {
		this.count = count;
	}
	
	public int offset() {
		return offset;
	}
	
	void setOffset(final int offset) {
		this.offset = offset;
	}
	
	public Collection<Result> getResults() {
		return results;
	}
	
	void setResults(final Collection<Result> results) {
		this.results = results;
	}
	
	public int size() {
		return size;
	}
	
	void setSize(final int size) {
		this.size = size;
	}
	
	public boolean isEmpty() {
		return results.isEmpty();
	}
	
	public Iterator<Result> iterator() {
		return results.iterator();
	}
}
