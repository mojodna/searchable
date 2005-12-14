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
