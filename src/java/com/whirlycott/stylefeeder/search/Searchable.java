/*
 * Created on Nov 27, 2005 by phil
 *
 */
package com.whirlycott.stylefeeder.search;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Searchable {

    public static final int KEYWORD = 1;
    
    public static final int UNINDEXED = 2;
    
    public static final int UNSTORED = 3;
    
    public static final int TEXT = 4;
    
    int value();
    
}

