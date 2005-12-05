package net.mojodna.searchable.example;

import net.mojodna.searchable.Searchable.Indexed;

public interface Bean extends SuperBean {
    @Indexed
    public String getGreen();
}
