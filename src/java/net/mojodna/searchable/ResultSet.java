package net.mojodna.searchable;

import java.util.Collection;
import java.util.Iterator;

public interface ResultSet extends Iterable<Result> {
	public int count();
	public int offset();
	public Collection<Result> getResults();
	public int size();
	public boolean isEmpty();
	public Iterator<Result> iterator();
}
