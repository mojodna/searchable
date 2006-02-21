package net.mojodna.searchable;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

public class BeanSearcher extends AbstractSearcher {
    public BeanSearcher() throws IndexException {
        super();
    }
    
    public ResultSet<Result> search(final Query query) throws SearchException {
        return doSearch( query );
    }
    
    public ResultSet<Result> search(final Query query, final Integer offset, final Integer count) throws SearchException {
        return doSearch( query, offset, count );
    }
    
    public ResultSet<Result> search(final Query query, final Integer offset, final Integer count, final String sortField) throws SearchException {
        return doSearch( query, offset, count, sortField );
    }

    public ResultSet<Result> search(final Query query, final Integer offset, final Integer count, final String sortField, final boolean reverse) throws SearchException {
        return doSearch( query, offset, count, sortField, reverse );
    }
    
    public ResultSet<Result> search(final String query) throws SearchException {
        return doSearch( query );
    }

    public ResultSet<Result> search(final String query, final String sortField) throws SearchException {
        return doSearch( query, sortField );
    }
    
    public ResultSet<Result> search(final String query, final Sort sort) throws SearchException {
        return doSearch( query, sort );
    }
    
    public ResultSet<Result> search(final String query, final Integer offset, final Integer count)  throws SearchException {
        return doSearch( query, offset, count );
    }
    
    public ResultSet<Result> search(final String query, final Integer offset, final Integer count, final String sortField)  throws SearchException {
        return doSearch( query, offset, count, sortField );
    }
    
    public ResultSet<Result> search(final String query, final Integer offset, final Integer count, final String sortField, final boolean reverse)  throws SearchException {
        return doSearch( query ,offset, count, sortField, reverse );
    }
    
    public ResultSet<Result> search(final String query, final Integer offset, final Integer count, final Sort sort)  throws SearchException {
        return doSearch( query, offset, count, sort );
    }
}
