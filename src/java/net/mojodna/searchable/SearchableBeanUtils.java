package net.mojodna.searchable;

import java.beans.PropertyDescriptor;

import org.apache.commons.beanutils.PropertyUtils;

public class SearchableBeanUtils {
    public static final String ID_PROPERTY_NAME = "id";

    public static String getIdPropertyName(final Searchable bean) {
        // look for Searchable.ID annotation
        final PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors( bean );
        for ( final PropertyDescriptor descriptor : pds ) {
            if ( descriptor.getReadMethod().isAnnotationPresent( Searchable.ID.class ) ) {
                return descriptor.getName();
            }
        }
        
        return SearchableBeanUtils.ID_PROPERTY_NAME;
    }
}
