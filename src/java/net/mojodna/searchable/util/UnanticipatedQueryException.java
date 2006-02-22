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

import org.apache.lucene.search.Query;

/**
 * Thrown by MultiFieldQueryPreparer when it encounters a Query type that is
 * not recognized.
 * 
 * @author Seth Fitzsimmons
 */
public class UnanticipatedQueryException extends SearchException {
    /**
     * Constructor with a Query.
     * 
     * @param query Query that caused the problem.
     */
    public UnanticipatedQueryException(final Query query) {
        super("Unanticipated query type: " + query.getClass().getName() );
    }
}