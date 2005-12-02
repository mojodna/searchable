package net.mojodna.searchable.example;

import java.util.Date;
import java.util.UUID;

import net.mojodna.searchable.AbstractSearchable;
import net.mojodna.searchable.BeanIndexer;
import net.mojodna.searchable.Searchable;
import net.mojodna.searchable.SearchableException;

public class SearchableBean extends AbstractSearchable implements Searchable {
    private UUID uuid;
    private String name;
    private String bio;
    private String secretCode;
    private int number = 42;
    private float value = 7.2F;
    private boolean bool = true;
    private Date now = new Date();
    
    /**
     * As there is no "id" property, an alternative must be specified. Store
     * it in the index as "bean-id".
     */
    @ID
    @Stored(name="bean-id")
    public UUID getUUID() {
        return uuid;
    }
    
    public void setUUID(final UUID uuid) {
        this.uuid = uuid;
    }
    
    /**
     * Index this property as "name" (default), don't tokenize it (it'll act
     * like a Keyword field), and set a boost value of 2.
     */
    @Indexed(boost=2F, tokenized=false)
    public String getName() {
        return name;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
    
    /**
     * Index this property and store a term vector.
     */
    @Indexed(storeTermVector=true)
    public String getBio() {
        return bio;
    }
    
    public void setBio(final String bio) {
        this.bio = bio;
    }
    
    /**
     * Store this property in the index as "secret".
     */
    @Stored(name="secret")
    public String getSecretCode() {
        return secretCode;
    }
    
    public void setSecretCode(final String secretCode) {
        this.secretCode = secretCode;
    }
    
    @Indexed
    public int getNumber() {
        return number;
    }
    
    @Indexed
    public boolean getBool() {
        return bool;
    }
    
    @Indexed
    public float getValue() {
        return value;
    }
    
    @Indexed(name="date", boost=4F)
    public Date getNow() {
        return now;
    }
    
    public static void main(final String[] args) throws SearchableException {
        final SearchableBean bean = new SearchableBean();
        bean.setName("Seth Fitzsimmons");
        bean.setBio("Seth likes to kayak a lot.");
        bean.setSecretCode("grothfuss");
        bean.setUUID( UUID.randomUUID() );
        
        final BeanIndexer bi = new BeanIndexer();
        bi.initialize( true );
        bi.add( bean );
        bi.close();
    }
}
