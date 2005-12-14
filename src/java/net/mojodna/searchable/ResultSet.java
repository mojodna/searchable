package net.mojodna.searchable;

import java.util.Iterator;
import java.util.List;

public interface ResultSet extends Iterable<Result> {
	public int count();
	public int offset();
	public List<Result> getResults();
	public int size();
	public boolean isEmpty();
	public Iterator<Result> iterator();
}
