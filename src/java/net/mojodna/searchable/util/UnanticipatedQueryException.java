package net.mojodna.searchable.util;

import net.mojodna.searchable.SearchException;

import org.apache.lucene.search.Query;

/**
 * @author seth
 */
public class UnanticipatedQueryException extends SearchException {
    public UnanticipatedQueryException(final Query query) {
        super("Unanticipated query type: " + query.getClass().getName() );
    }
}
