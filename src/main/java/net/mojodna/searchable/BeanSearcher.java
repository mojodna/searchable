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

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * Searcher implementation that searches an index for Searchables.
 * 
 * @author Seth Fitzsimmons
 */
public class BeanSearcher extends AbstractSearcher implements
		Searcher<Searchable> {
	/**
	 * Constructor.
	 */
	public BeanSearcher() {
		super();
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final Query query)
			throws IndexException {
		return doSearch(query);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param filter Filter to use.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final Query query,
			final Filter filter) throws IndexException {
		return doSearch(query, filter);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param filter Filter to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final Query query,
			final Filter filter, final Integer offset, final Integer count)
			throws IndexException {
		return doSearch(query, filter, offset, count);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param filter Filter to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @param sortField Field to sort by.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final Query query,
			final Filter filter, final Integer offset, final Integer count,
			final String sortField) throws IndexException {
		return doSearch(query, filter, offset, count, sortField);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param filter Filter to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @param sortField Field to sort by.
	 * @param reverse Whether to reverse the resultset. 
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final Query query,
			final Filter filter, final Integer offset, final Integer count,
			final String sortField, final boolean reverse)
			throws IndexException {
		return doSearch(query, filter, offset, count, sortField, reverse);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final Query query,
			final Integer offset, final Integer count) throws IndexException {
		return doSearch(query, offset, count);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @param sortField Field to sort by.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final Query query,
			final Integer offset, final Integer count, final String sortField)
			throws IndexException {
		return doSearch(query, offset, count, sortField);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @param sortField Field to sort by.
	 * @param reverse Whether to reverse the resultset. 
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final Query query,
			final Integer offset, final Integer count, final String sortField,
			final boolean reverse) throws IndexException {
		return doSearch(query, offset, count, sortField, reverse);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<Searchable> search(final String query)
			throws IndexException {
		return doSearch(query);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param filter Filter to use.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final String query,
			final Filter filter) throws IndexException {
		return doSearch(query, filter);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param filter Filter to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final String query,
			final Filter filter, final Integer offset, final Integer count)
			throws IndexException {
		return doSearch(query, filter, offset, count);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param filter Filter to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @param sort Sort to use.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final String query,
			final Filter filter, final Integer offset, final Integer count,
			final Sort sort) throws IndexException {
		return doSearch(query, filter, offset, count, sort);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param filter Filter to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @param sortField Field to sort by.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final String query,
			final Filter filter, final Integer offset, final Integer count,
			final String sortField) throws IndexException {
		return doSearch(query, filter, offset, count, sortField);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param filter Filter to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @param sortField Field to sort by.
	 * @param reverse Whether to reverse the resultset. 
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final String query,
			final Filter filter, final Integer offset, final Integer count,
			final String sortField, final boolean reverse)
			throws IndexException {
		return doSearch(query, filter, offset, count, sortField, reverse);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param filter Filter to use.
	 * @param sort Sort to use.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final String query,
			final Filter filter, final Sort sort) throws IndexException {
		return doSearch(query, filter, sort);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param filter Filter to use.
	 * @param sortField Field to sort by.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final String query,
			final Filter filter, final String sortField) throws IndexException {
		return doSearch(query, filter, sortField);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<Searchable> search(final String query,
			final Integer offset, final Integer count) throws IndexException {
		return doSearch(query, offset, count);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @param sort Sort to use.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final String query,
			final Integer offset, final Integer count, final Sort sort)
			throws IndexException {
		return doSearch(query, offset, count, sort);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @param sortField Field to sort by.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final String query,
			final Integer offset, final Integer count, final String sortField)
			throws IndexException {
		return doSearch(query, offset, count, sortField);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param offset Offset to begin result set at.
	 * @param count Number of results to return.
	 * @param sortField Field to sort by.
	 * @param reverse Whether to reverse the resultset. 
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final String query,
			final Integer offset, final Integer count, final String sortField,
			final boolean reverse) throws IndexException {
		return doSearch(query, offset, count, sortField, reverse);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param sort Sort to use.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final String query,
			final Sort sort) throws IndexException {
		return doSearch(query, sort);
	}

	/**
	 * Search the index with the specified query.
	 * 
	 * @param query Query to use.
	 * @param sortField Field to sort by.
	 * @return ResultSet containing results.
	 * @throws IndexException
	 */
	public ResultSet<? extends Searchable> search(final String query,
			final String sortField) throws IndexException {
		return doSearch(query, sortField);
	}
}
