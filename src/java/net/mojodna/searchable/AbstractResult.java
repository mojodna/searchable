package net.mojodna.searchable;

import java.util.Map;

public abstract class AbstractResult implements Result {
    private int ranking;
    private Map storedFields;
    private float score;
    
    public int getRanking() {
        return ranking;
    }
    
    public void setRanking(final int ranking) {
        this.ranking = ranking;
    }
    
    public Map getStoredFields() {
        return storedFields;
    }
    
    public void setStoredFields(final Map storedFields) {
        this.storedFields = storedFields;
    }
    
    public float getScore() {
        return score;
    }
    
    public void setScore(final float score) {
        this.score = score;
    }
}
