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
package net.mojodna.searchable.util;

import net.mojodna.searchable.SearchException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

/**
 * Simulates AND operator for MultiFieldQueryParser.
 * 
 * This is unnecessary as of Lucene 1.9
 * 
 * @author seth
 */
public abstract class MultiFieldQueryPreparer {
    private static final Logger log = Logger.getLogger( MultiFieldQueryPreparer.class ); 
    private static final String JUNK_FIELD = "org.prx.search.AbstractSearcher.junk";
    
    /**
     * Prepare a query as if it were going through the MultiFieldQueryParser in
     * AND mode.
     * 
     * @param query Query to prepare.
     * @param defaultFields List of default fields.
     * @param analyzer Analyzer to use.
     * @return Parsed query.
     * @throws SearchException
     */
    public static Query prepareQuery(final String query, final String[] defaultFields, final Analyzer analyzer) throws SearchException {
        final QueryParser qp = new QueryParser( JUNK_FIELD, analyzer );
        qp.setOperator( QueryParser.DEFAULT_OPERATOR_AND );
        
        try {
            // do a first-round parse to set proper ANDness and to pick up explicitly specified fields
            Query q = qp.parse( query );
            // subsequent rounds to point at all desired fields
            q = resolveQuery(q, defaultFields, analyzer );
            return q;
        }
        catch (final ParseException e) {
            throw new SearchException(e);
        }
    }
    
    private static Query resolveQuery(Query query, final String[] defaultFields, final Analyzer analyzer) throws ParseException, UnanticipatedQueryException {
        if ( query instanceof TermQuery ) {
            // simple, single-term query

            final TermQuery tq = (TermQuery) query;
            if ( tq.getTerm().field().equals( MultiFieldQueryPreparer.JUNK_FIELD ) ) {
                // no field specified; rewrite
                query = MultiFieldQueryParser.parse( tq.getTerm().text(), defaultFields, analyzer );
                query.setBoost( tq.getBoost() );
            }
        } else if ( query instanceof BooleanQuery ) {
            query = resolveBooleanQuery( (BooleanQuery) query, defaultFields, analyzer );
        } else if ( query instanceof PrefixQuery ) {
            final PrefixQuery pq = (PrefixQuery) query;
            if ( pq.getPrefix().field().equals( MultiFieldQueryPreparer.JUNK_FIELD ) ) {
                // no field specified; rewrite (include * to mark this as a PrefixQuery)
                query = MultiFieldQueryParser.parse( pq.getPrefix().text() + "*", defaultFields, analyzer );
                query.setBoost( pq.getBoost() );
            }
        } else if ( query instanceof FuzzyQuery ) {
            final FuzzyQuery fq = (FuzzyQuery) query;
            if ( fq.getTerm().field().equals( MultiFieldQueryPreparer.JUNK_FIELD ) ) {
                query = MultiFieldQueryParser.parse( fq.getTerm().text() + "~", defaultFields, analyzer );
                query.setBoost( fq.getBoost() );
            }
        } else if ( query instanceof WildcardQuery ) {
            final WildcardQuery wq = (WildcardQuery) query;
            if ( wq.getTerm().field().equals( MultiFieldQueryPreparer.JUNK_FIELD ) ) {
                query = MultiFieldQueryParser.parse( wq.getTerm().text(), defaultFields, analyzer );
                query.setBoost( wq.getBoost() );
            }
        } else if ( query instanceof PhraseQuery ) {
            query = resolvePhraseQuery( (PhraseQuery) query, defaultFields, analyzer );
        } else if ( query instanceof RangeQuery ) {
            query = resolveRangeQuery( (RangeQuery) query, defaultFields, analyzer );
        } else {
            throw new UnanticipatedQueryException( query );
        }

        return query;
    }
    
    private static Query resolveBooleanQuery(final BooleanQuery query, final String[] defaultFields, final Analyzer analyzer) throws ParseException, UnanticipatedQueryException {
        final BooleanQuery newQuery = new BooleanQuery();
        final BooleanClause[] clauses = query.getClauses();
        for ( int i=0; i < clauses.length; i++ ) {
            final BooleanClause clause = clauses[i];
            if ( clause.query instanceof TermQuery ) {
                final TermQuery tq = (TermQuery) clause.query;
                if ( tq.getTerm().field().equals( MultiFieldQueryPreparer.JUNK_FIELD ) ) {
                    // rewrite this subQuery to search all desired fields
                    final Query q = MultiFieldQueryParser.parse( tq.getTerm().text(), defaultFields, analyzer );
                    newQuery.add(q, clause.required, clause.prohibited);
                } else {
                    // use this query as-is because a non-junk field was specified
                    newQuery.add(tq, clause.required, clause.prohibited);
                }
                newQuery.setBoost( tq.getBoost() );
            } else if ( clause.query instanceof BooleanQuery ) {
                // nested BooleanQuery, recur
                final Query q = resolveBooleanQuery( (BooleanQuery) clause.query, defaultFields, analyzer );
                newQuery.add( q, clause.required, clause.prohibited );
                newQuery.setBoost( clause.query.getBoost() );
            } else {
                final Query q = resolveQuery( clause.query, defaultFields, analyzer );
                newQuery.add( q, clause.required, clause.prohibited );
                newQuery.setBoost( clause.query.getBoost() );
            }
        }
        return newQuery;
    }
    
    private static Query resolvePhraseQuery(final PhraseQuery query, final String[] defaultFields, final Analyzer analyzer) throws ParseException, UnanticipatedQueryException {
        final Term[] terms = query.getTerms();
        
        // if a field was specified, don't rewrite it
        if ( terms.length > 0 && !terms[0].field().equals( MultiFieldQueryPreparer.JUNK_FIELD ) ) {
            return query;
        }
        
        final StringBuffer str = new StringBuffer();
        str.append("\"");
        for ( int i = 0; i < terms.length; i++ ) {
            if ( i > 0 )
                str.append(" ");
            str.append( terms[i].text() );
        }
        str.append("\"");
        
        log.debug("Reconstituted string: " + str.toString() );

        final Query q = MultiFieldQueryParser.parse( str.toString(), defaultFields, analyzer );
        if ( q instanceof BooleanQuery ) {
            final BooleanClause[] clauses = ((BooleanQuery) q).getClauses();
            for ( int i = 0; i < clauses.length; i++ ) {
                if ( clauses[i].query instanceof PhraseQuery ) {
                    final PhraseQuery pq = (PhraseQuery) clauses[i].query;
                    pq.setBoost( query.getBoost() );
                    pq.setSlop( query.getSlop() );
                } else {
                    throw new UnanticipatedQueryException( clauses[i].query );
                }
            }
        } else {
            throw new UnanticipatedQueryException( q );
        }

        return q;
    }
    
    private static Query resolveRangeQuery(final RangeQuery query, final String[] defaultFields, final Analyzer analyzer) throws ParseException, UnanticipatedQueryException {
        Query q = null;
        if ( query.getField().equals( MultiFieldQueryPreparer.JUNK_FIELD ) ) {
            final String range = query.getLowerTerm().text() + " TO " + query.getUpperTerm();
            final StringBuffer str = new StringBuffer();
            if ( query.isInclusive() ) {
                str.append("[")
                	.append( range )
                	.append("]");
                
            } else {
                str.append("{")
                	.append( range )
                	.append("}");
            }
            
            log.debug("Reconstituted query: " + str.toString() );
            
            q = MultiFieldQueryParser.parse( str.toString(), defaultFields, analyzer );
            if ( q instanceof BooleanQuery ) {
                q.setBoost( query.getBoost() );
            } else {
                throw new UnanticipatedQueryException( q );
            }
        } else {
            q = query;
        }
        return q;
    }
}
