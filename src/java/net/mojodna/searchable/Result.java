package net.mojodna.searchable;

import java.util.Map;

public interface Result {
    public float getScore();
    public void setScore(float score);
    public Map getStoredFields();
    public void setStoredFields(Map storedFields);
    public int getRanking();
    public void setRanking(int ranking);
}
