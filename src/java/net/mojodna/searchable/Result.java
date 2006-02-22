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

import java.util.Map;

/**
 * An interface for objects that will be returned as part of a ResultSet.
 * 
 * @author Seth Fitzsimmons
 */
public interface Result {
    // TODO add documentId?
    
    /**
     * Gets this result's score.
     * 
     * @return Score.
     */
    public float getScore();
    
    /**
     * Sets this result's score.
     * 
     * @param score Score.
     */
    public void setScore(float score);
    
    /**
     * Gets fields that were stored in the index for this result.
     * 
     * @return Map containing stored field names and values.
     */
    public Map<String,String> getStoredFields();
    
    /**
     * Sets fields that were stored in the index for this result.
     * 
     * @param storedFields Stored field names and values.
     */
    public void setStoredFields(Map<String,String> storedFields);
    
    /**
     * Gets this result's ranking in the resultset.
     * 
     * @return Ranking.
     */
    public int getRanking();
    
    /**
     * Sets this result's ranking in the resultset.
     * 
     * @param ranking Ranking.
     */
    public void setRanking(int ranking);
    
    /**
     * Gets this result's search extract.
     * 
     * @return Highlighted excerpted field.
     */
    public String getSearchExtract();
    
    /**
     * Sets this result's search extract.
     * 
     * @param extract Highlighted excerpted field.
     */
    public void setSearchExtract(String extract);
}
