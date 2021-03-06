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

import java.util.Map;

/**
 * An abstract Result class provided as a convenience superclass for classes
 * that implement Result.
 * 
 * @author Seth Fitzsimmons
 */
public abstract class AbstractResult implements Result {
	private String extract;

	private int ranking;

	private float score;

	private Map<String, String> storedFields;

	public int getRanking() {
		return ranking;
	}

	public float getScore() {
		return score;
	}

	public String getSearchExtract() {
		return extract;
	}

	public Map<String, String> getStoredFields() {
		return storedFields;
	}

	public void setRanking(final int ranking) {
		this.ranking = ranking;
	}

	public void setScore(final float score) {
		this.score = score;
	}

	public void setSearchExtract(final String extract) {
		this.extract = extract;
	}

	public void setStoredFields(final Map<String, String> storedFields) {
		this.storedFields = storedFields;
	}
}
