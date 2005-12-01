package net.mojodna.searchable;

import java.util.Map;

public class GenericResult implements Result {
    private String id;
    private String type;
    
    private int ranking;
    private Map storedFields;
    private float score;
    
    public String getId() {
        return id;
    }
    
    void setId(final String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    void setType(final String type) {
        this.type = type;
    }
    
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
