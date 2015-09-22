
package com.blackberry.common.ui.list;

/**
 * Interface provides mechanism for supplying section headers for a given instances.
 */
public interface Sectionizer {

    /**
     * Returns the title for the given instance from the data source.
     * 
     * @param instance The instance obtained from the data source of the decorated list adapter.
     * @return section title for the given instance.
     */
    CharSequence getSectionTitle(Object instance);
}
