package net.mojodna.searchable.example;

import net.mojodna.searchable.Searchable.Indexed;

public interface SuperBean {
    @Indexed
    public String getWax();
}
