package net.mojodna.searchable.example;

import net.mojodna.searchable.AbstractResult;
import net.mojodna.searchable.Searchable;

public class NestedSearchableBean extends AbstractResult implements Searchable {
    private String hello = "world";
    private String empty;
    
    @Indexed
    public String getHello() {
        return hello;
    }
    
    @Indexed
    public String getEmpty() {
        return empty;
    }
}
