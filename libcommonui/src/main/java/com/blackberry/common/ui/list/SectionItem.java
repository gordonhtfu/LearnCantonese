
package com.blackberry.common.ui.list;

/**
 * Class that represents and item in a sectioned list.
 */
public final class SectionItem {

    public static final int ITEM = 0;
    public static final int HEADER = 1;

    private final int mType;
    private final CharSequence mText;

    // The position of the section this item belongs to
    private int mSectionPosition;
    // The position of this item in the listview
    private int mListPosition;
    // The position of this item in its adapter
    private int mSourcePosition;

    public SectionItem(int type, CharSequence text, int sectionPosition, int listPosition,
            int sourcePosition) {
        mType = type;
        mText = text;
        mSectionPosition = sectionPosition;
        mListPosition = listPosition;
        mSourcePosition = sourcePosition;
    }

    public int sectionPosition() {
        return mSectionPosition;
    }

    public int listPosition() {
        return mListPosition;
    }

    public int sourcePosition() {
        return mSourcePosition;
    }

    public int itemType() {
        return mType;
    }

    public boolean isHeader() {
        return mType == HEADER;
    }

    @Override
    public String toString() {
        return mText.toString();
    }

    public static SectionItem createItem(int sectionPosition, int listPosition,
            int sourcePosition) {
        String itemText = String.format("sectionPosition: %d listPosition: %d sourcePosition %d",
                sectionPosition, listPosition, sourcePosition);
        SectionItem item = new SectionItem(SectionItem.ITEM,
                itemText,
                sectionPosition,
                listPosition,
                sourcePosition);
        return item;
    }

    public static SectionItem createHeader(CharSequence text, int sectionPosition,
            int listPosition,
            int sourcePosition) {
        SectionItem item = new SectionItem(SectionItem.HEADER,
                text,
                sectionPosition,
                listPosition,
                sourcePosition);
        return item;
    }
}
