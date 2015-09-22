
package com.blackberry.common.ui.list;

/**
 * A basic {@link Sectionizer} that uses the capitalized first letter of the toString() of the given
 * object. Spaces, numbers, and symbols are all converted to the character '#'.
 */

public class SimpleSectionizer implements Sectionizer {
    @Override
    public CharSequence getSectionTitle(Object item) {
        if (item != null) {
            String itemString = item.toString();
            if (itemString.length() > 0) {
                char firstChar = Character.toUpperCase(itemString.charAt(0));
                // Check that the first character is a letter
                if (firstChar >= 'A' && firstChar <= 'Z') {
                    return Character.toString(firstChar);
                }
            }
        }
        return "#";
    }
}
