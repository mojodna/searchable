package net.mojodna.searchable.converter;

import java.util.UUID;

import org.apache.commons.beanutils.Converter;

public class UUIDConverter implements Converter {
    public Object convert(final Class clazz, final Object value) {
        return UUID.fromString( value.toString() );
    }
}
