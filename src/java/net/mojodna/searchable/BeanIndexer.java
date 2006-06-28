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

import org.apache.lucene.document.Document;

/**
 * Visible API for indexing Searchables.
 * 
 * @author Seth Fitzsimmons
 */
public class BeanIndexer extends AbstractBeanIndexer implements Indexer<Searchable> {
    /**
     * Constructor.
     * 
     * @throws IndexException
     */
    public BeanIndexer() throws IndexException {
        super();
    }
    
    /**
     * Add a searchable bean to the index.
     * 
     * @param bean Bean to index.
     * @return Document with this bean added.
     * @throws IndexingException
     */
    public Document add(final Searchable bean) throws IndexException {
        return doAdd( bean );
    }
    
    /**
     * Delete an object from the index.
     * 
     * @param bean Object to delete.
     */
    public void delete(final Searchable bean) throws IndexException {
        doDelete( bean );
    }
}
