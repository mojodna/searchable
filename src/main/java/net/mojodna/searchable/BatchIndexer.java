/*
 Copyright 2006 Seth Fitzsimmons <seth@mojodna.net>

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

/**
 * Batch indexer interface.
 * 
 * @author Seth Fitzsimmons
 * @param <E> Type of object to index
 */
public interface BatchIndexer<E extends Searchable> extends Indexer<E> {
	/**
	 * Flush any pending deletes or index merges.
	 * 
	 * @throws IndexException
	 */
	void flush() throws IndexException;

	/**
	 * Is this running in batch mode?
	 * 
	 * @return Whether this indexer is running in batch mode.
	 */
	boolean isBatchMode();

	/**
	 * Sets whether this indexer is running in batch mode.
	 * 
	 * @param batchMode Whether this indexer should run in batch mode.
	 */
	void setBatchMode(boolean batchMode);
}
