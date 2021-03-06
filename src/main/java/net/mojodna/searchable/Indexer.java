/*
 Copyright 2005-2006 Seth Fitzsimmons <seth@mojodna.net>

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
 * Barebones indexer interface.
 * 
 * @author Seth Fitzsimmons
 * @param <E> 
 */
public interface Indexer<E extends Searchable> {
	/**
	 * Adds an object to the index.
	 * 
	 * @param object Object to index.
	 * @return Document
	 * @throws IndexException
	 */
	Document add(E object) throws IndexException;

	/**
	 * Deletes an object from the index.
	 * 
	 * @param object Object to delete.
	 * @throws IndexException
	 */
	void delete(E object) throws IndexException;

	/**
	 * Optimize the index.
	 *
	 * @throws IndexException
	 */
	void optimize() throws IndexException;
}
