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

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * A container for generic results.  This is used when an object of the
 * correct type cannot be instantiated.
 * 
 * @author Seth Fitzsimmons
 */
public class GenericResult extends AbstractResult implements Result {
	private String id;

	private String type;

	/**
	 * Gets the id of the object corresponding to this result.
	 * 
	 * @return Object id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Gets the type of the object corresponding to this result.
	 * 
	 * @return Object type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the id of the object corresponding to this result.
	 * 
	 * @param id
	 */
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * Sets the type of the object corresponding to this result.
	 * 
	 * @param type Object type.
	 */
	public void setType(final String type) {
		this.type = type;
	}

	@Override
    public String toString() {
        return new ToStringBuilder(this).append("id", this.id).append("score", this.getScore()).append("type", this.type).append("storedFields",
                this.getStoredFields()).append("searchExtract", this.getSearchExtract()).append("ranking", this.getRanking()).toString();
    }
}
