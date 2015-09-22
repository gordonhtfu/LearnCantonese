
package com.blackberry.widgets.tagview;

import android.content.ClipData;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.QwertyKeyListener;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.blackberry.widgets.R;
import com.blackberry.widgets.tagview.BaseTag.BaseTagDimensions;
import com.blackberry.widgets.tagview.BaseTag.IDeletable;
import com.blackberry.widgets.tagview.BaseTag.OnTagDeleteClickListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class TagTextView extends MultiAutoCompleteTextView implements
        AdapterView.OnItemClickListener, TextView.OnEditorActionListener,
        GestureDetector.OnGestureListener, ActionMode.Callback {

    private static final String[] TAG_MIME = {
            "com.blackberry.tag/item"
    };
    private Tokenizer mTokenizer = new CommaTokenizer();
    private Class<? extends BaseTag> mTagClass = BaseTag.class;
    private OnCreateTagDataItemRequest mOnCreateTagDataItemRequest;
    private TagTextWatcher mTextWatcher = new TagTextWatcher();
    private PopupWindow mDetailsView;
    private boolean mReadOnly;
    private BaseTagDimensions mTagDimensions;
    private float mCompletionsLeftOffset;
    private OnTagListChanged<BaseTag> mOnTagListChanged;
    private GestureDetector mGestureDetector;
    private String mDragGroup = "";
    private int mMaxTagsWhenCollapsed = 4;
    private MoreAttributes mMoreAttributes;
    private List<BaseTag> mHiddenTags = new ArrayList<BaseTag>();
    private boolean mIsCollapsed = true;
    private OnTagDeleteClickListener mOnDeleteClickListener = new OnTagDeleteClickListener() {

        @Override
        public void onClick(BaseTag tag) {
            removeTag(tag);
            if (mDetailsView.isShowing()) {
                mDetailsView.dismiss();
            }
        }
    };
    private boolean mKeyboardWasHidden;
    private InputMethodManager mImeManager;
    private Drawable mClearDrawable;
    private boolean mNoChangeNotifications = false;

    /**
     * @param context
     * @param attrs
     */
    public TagTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTokenizer(mTokenizer);
        setThreshold(1);
        setOnItemClickListener(this);
        addTextChangedListener(mTextWatcher);
        setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setOnEditorActionListener(this);
        mImeManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mDetailsView = new PopupWindow(context);
        mDetailsView.setWindowLayoutMode(0,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mDetailsView.setFocusable(true);
        mDetailsView.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {
                // re-show the keyboard if needed.
                if (hasFocus() && mKeyboardWasHidden) {
                    showSoftKeyboard();
                }
            }
        });

        mTagDimensions = new BaseTagDimensions();
        Resources r = context.getResources();
        mTagDimensions.height = r.getDimension(R.dimen.basetag_height);
        mTagDimensions.paddingLeft = r.getDimension(R.dimen.basetag_text_padding_left);
        mTagDimensions.paddingRight = r.getDimension(R.dimen.basetag_text_padding_right);
        mTagDimensions.paddingTop = r.getDimension(R.dimen.basetag_text_padding_top);
        mTagDimensions.paddingBottom = r.getDimension(R.dimen.basetag_text_padding_bottom);
        mTagDimensions.textSize = r.getDimension(R.dimen.basetag_text_size);
        mTagDimensions.spacing = r.getDimension(R.dimen.basetag_spacing);

        mCompletionsLeftOffset = r.getDimension(R.dimen.tagtextview_completions_left_offset);

        mMoreAttributes = new MoreAttributes();
        mMoreAttributes.height = (int) mTagDimensions.height;
        mMoreAttributes.textSize = r.getDimension(R.dimen.tagtextview_more_text_size);
        mMoreAttributes.paddingLeft = (int) r.getDimension(R.dimen.tagtextview_more_padding_left);
        mMoreAttributes.paddingRight = (int) r.getDimension(R.dimen.tagtextview_more_padding_right);
        mMoreAttributes.textColor = r.getColor(R.color.tagtextview_more_text_color);
        mMoreAttributes.text = r.getString(R.string.tagtextview_more_text);

        mGestureDetector = new GestureDetector(context, this);
        setCustomSelectionActionModeCallback(this);

        setCompoundDrawablePadding((int) r.getDimension(R.dimen.tagtextview_compound_padding));
        mClearDrawable = r.getDrawable(R.drawable.ic_cancel_gry_24dp);
        mClearDrawable.setBounds(0, 0, mClearDrawable.getIntrinsicWidth(),
                mClearDrawable.getIntrinsicHeight());

        // There is a small square highlight at the end of a tag when you long
        // press on the right half to drag-n-drop. There is no functional issue
        // but is a small bit of UI ugliness. This makes the highlight color
        // transparent.
        setHighlightColor(0x0);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position < 0) {
            return;
        }
        submitItemAtPosition(position);
    }

    TagImageSpan[] getSortedTagSpans() {
        TagImageSpan[] tags = getText()
                .getSpans(0, getText().length(), TagImageSpan.class);
        ArrayList<TagImageSpan> tagsList = new ArrayList<TagImageSpan>(
                Arrays.asList(tags));
        final Spannable spannable = getText();
        Collections.sort(tagsList, new Comparator<TagImageSpan>() {

            @Override
            public int compare(TagImageSpan first, TagImageSpan second) {
                int firstStart = spannable.getSpanStart(first);
                int secondStart = spannable.getSpanStart(second);
                if (firstStart < secondStart) {
                    return -1;
                } else if (firstStart > secondStart) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        return tagsList.toArray(new TagImageSpan[tagsList.size()]);
    }

    private void sanitizeBetween() {
        TagImageSpan[] tags = getSortedTagSpans();
        if (tags != null && tags.length > 0) {
            for (int i = tags.length - 1; i >= 0; i -= 1) {
                TagImageSpan last = tags[i];
                TagImageSpan beforeLast = null;
                if (i >= 1) {
                    beforeLast = tags[i - 1];
                }
                int startLooking = 0;
                int end = getText().getSpanStart(last) - 1;
                if (beforeLast != null) {
                    startLooking = getText().getSpanEnd(beforeLast);
                } else {
                    startLooking = 0;
                }
                Editable text = getText();
                if (startLooking == -1 || startLooking > text.length() - 1) {
                    return;
                }
                char atPos = text.charAt(startLooking);
                if ((atPos == ' ') && (i > 0)) {
                    startLooking++;
                }

                if (startLooking >= 0 && end >= 0 && startLooking < end) {
                    getText().delete(startLooking, end);
                }
            }
        }
    }

    private void submitItemAtPosition(int position) {
        Object item = getAdapter().getItem(position);
        if (item == null) {
            return;
        }
        submitItem(item);
    }

    private void submitItem(Object item) {
        clearComposingText();

        int end = getSelectionEnd();
        int start = Math.max(0, mTokenizer.findTokenStart(getText(), end - 2));

        Editable editable = getText();
        QwertyKeyListener.markAsReplaced(editable, start, end, "");
        CharSequence tag = createTag(item, false);
        if (tag != null && start >= 0 && end >= 0) {
            editable.replace(start, end, tag);
        }
        sanitizeBetween();
    }

    private float calculateAvailableWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight() - mTagDimensions.spacing
                - getClearDrawableWidth();
    }

    private TagImageSpan createTagSpan(BaseTag item) {
        TextPaint paint = getPaint();
        float defaultSize = paint.getTextSize();
        int defaultColor = paint.getColor();

        paint.setTextSize(mTagDimensions.textSize);
        paint.setColor(getContext().getResources().getColor(android.R.color.black));

        TagImageSpan tagImageSpan = new TagImageSpan(getContext(), (int) calculateAvailableWidth(),
                mTagDimensions, paint, item);

        // Return text to the original size.
        paint.setTextSize(defaultSize);
        paint.setColor(defaultColor);

        return tagImageSpan;
    }

    private BaseTag createBaseTag(Object item) {
        BaseTag tag = null;
        try {
            tag = mTagClass.newInstance();
            tag.setData(item);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return tag;
    }

    private CharSequence createTag(Object item, boolean b) {
        BaseTag tag = createBaseTag(item);
        // leave a blank space between each chip
        CharSequence displayText = mTokenizer.terminateToken(" " + tag.getLabel());
        if (TextUtils.isEmpty(displayText)) {
            return null;
        }
        SpannableString chipText = null;
        // Always leave a blank space at the end of a chip.
        int textLength = displayText.length() - 1;
        chipText = new SpannableString(displayText);
        try {
            // leave the space before the tag
            chipText.setSpan(createTagSpan(tag), 1, textLength,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } catch (NullPointerException e) {
            return null;
        }
        return chipText;
    }

    /**
     * Set this to the class of the BaseTag-derived class which will be used to
     * display each item in the tag list.
     * 
     * @param tagClass The class to use to represent the objects in the list.
     */
    public void setTagClass(Class<? extends BaseTag> tagClass) {
        mTagClass = tagClass;
    }

    private class TagTextWatcher implements TextWatcher {

        private TagImageSpan[] mPreEditSpans;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mPreEditSpans = getSortedTagSpans();
            // Log.d("TagTextView", "beforeTextChanged");
            // printTagsDebugState();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // The user deleted some text OR some text was replaced; check to
            // see if the insertion point is on a space
            // following a chip.
            if (before - count == 1) {
                // If the item deleted is a space, and the thing before the
                // space is a chip, delete the entire span.
                int selStart = getSelectionStart();
                TagImageSpan[] repl = getText().getSpans(selStart, selStart, TagImageSpan.class);
                if (repl.length > 0) {
                    // There is a chip there! Just remove it.
                    Editable editable = getText();
                    // Add the separator token.
                    int tokenStart = mTokenizer.findTokenStart(editable, selStart - 2);
                    int tokenEnd = mTokenizer.findTokenEnd(editable, tokenStart);
                    tokenEnd = tokenEnd + 1;
                    if (tokenEnd > editable.length()) {
                        tokenEnd = editable.length();
                    }
                    // grab the space before the tag and delete it as well or we
                    // will get dangling spaces sanitizeBetween() will have to
                    // clean up.
                    editable.delete(tokenStart - 1, tokenEnd);
                    getText().removeSpan(repl[0]);
                    sanitizeBetween();
                }
            } else if (count > before) {
                // if (mSelectedChip != null
                // && isGeneratedContact(mSelectedChip)) {
                // if (lastCharacterIsCommitCharacter(s)) {
                // commitByCharacter();
                // return;
                // }
                // }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // If the text has been set to null or empty, make sure we remove
            // all the spans we applied.
            if (TextUtils.isEmpty(s)) {
                // Remove all the tag spans.
                Spannable spannable = getText();
                TagImageSpan[] tags = spannable.getSpans(0, getText().length(),
                        TagImageSpan.class);
                for (TagImageSpan tag : tags) {
                    spannable.removeSpan(tag);
                }
                mHiddenTags.clear();
                // if (mMoreChip != null) {
                // spannable.removeSpan(mMoreChip);
                // }
            }
            if (!mNoChangeNotifications && (mOnTagListChanged != null) && (mPreEditSpans != null)) {
                List<TagImageSpan> preEditSpanList = Arrays.asList(mPreEditSpans);
                List<TagImageSpan> postEditSpanList = Arrays.asList(getSortedTagSpans());
                // find added tags
                for (TagImageSpan tagImageSpan : postEditSpanList) {
                    if (!preEditSpanList.contains(tagImageSpan)) {
                        mOnTagListChanged.tagAdded(tagImageSpan.getTag());
                    }
                }
                // find all removed tags
                for (TagImageSpan tagImageSpan : preEditSpanList) {
                    if (!postEditSpanList.contains(tagImageSpan)) {
                        mOnTagListChanged.tagRemoved(tagImageSpan.getTag());
                    }
                }

            }
            mPreEditSpans = null;
            autoShowClearDrawable();
            // Log.d("TagTextView", "afterTextChanged");
            // printTagsDebugState();
        }
    }

    private boolean focusNext() {
        View next = focusSearch(View.FOCUS_DOWN);
        if (next != null) {
            next.requestFocus();
            return true;
        }
        return false;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection connection = super.onCreateInputConnection(outAttrs);
        int imeActions = outAttrs.imeOptions & EditorInfo.IME_MASK_ACTION;
        if ((imeActions & EditorInfo.IME_ACTION_DONE) != 0) {
            // clear the existing action
            outAttrs.imeOptions ^= imeActions;
            // set the DONE action
            outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
        }
        if ((outAttrs.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        }

        outAttrs.actionId = EditorInfo.IME_ACTION_DONE;
        return connection;
    }

    private CharSequence getComposingText() {
        Editable text = getText();
        int end = getSelectionEnd();
        if (end < 0 || mTokenizer == null) {
            return "";
        }

        int start = mTokenizer.findTokenStart(text, end);
        return text.subSequence(start, end);
    }

    private boolean tryToSubmitItem() {
        Adapter adapter = getAdapter();
        if ((adapter != null) && (adapter.getCount() > 0) && enoughToFilter()) {
            submitItemAtPosition(0);
            return true;
        }
        return tryToSubmitComposingText();
    }

    private boolean tryToSubmitComposingText() {
        CharSequence composingText = getComposingText();
        if (!TextUtils.isEmpty(composingText)) {
            if (mOnCreateTagDataItemRequest != null) {
                Object tagDataItem = mOnCreateTagDataItemRequest.createTag(composingText);
                if (tagDataItem != null) {
                    submitItem(tagDataItem);
                }
                clearComposingText();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            // if (mSelectedChip != null) {
            // clearSelectedChip();
            // return true;
            // }
            if (tryToSubmitItem()) {
                return true;
            } else if (TextUtils.isEmpty(getComposingText()) && focusNext()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSelectionChanged(int start, int end) {
        // Log.d("TagTextView", "onSelectionChanged");
        // printTagsDebugState();
        // If the cursor is now "under" a tag move it back to the end of that
        // tag
        // Under means at a position under the tag (NOT the first index it
        // covers!) or the space character after the tag.
        TagImageSpan[] tagsUnderCursor = getText().getSpans(start, end, TagImageSpan.class);
        if (tagsUnderCursor.length > 0) {
            // If we are at the start of a tag, move to before the space prior
            // to the tag. Otherwise move to after the space directly following
            // the tag. This guarantees there is always 1 space between the tags
            // and the cursor in both directions.
            if (getText().getSpanStart(tagsUnderCursor[0]) == start) {
                setSelection(start - 1);
            } else {
                setSelection(Math.min(getText().getSpanEnd(tagsUnderCursor[0]) + 1, getText()
                        .length()));
            }
        }
        super.onSelectionChanged(start, end);
    }

    public void setOnCreateTagDataItemRequest(OnCreateTagDataItemRequest onCreateTagDataItemRequest) {
        mOnCreateTagDataItemRequest = onCreateTagDataItemRequest;
    }

    public interface OnCreateTagDataItemRequest {
        Object createTag(CharSequence inputText);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        autoShowClearDrawable();
        if (!focused) {
            tryToSubmitItem();
            if (mDetailsView.isShowing()) {
                mDetailsView.dismiss();
            }
            collapseControl();
        } else {
            expandControl();
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    /**
     * Adds a tag to the end of the list.
     * 
     * @param dataItem The data item to add.
     */
    public void addTag(Object dataItem) {
        // This is to fix a UI glitch when adding tags
        // before the widget has been sized the first time.
        if (getMeasuredWidth() > 0) {
            doAddTag(dataItem);
        } else {
            final Object finalDataItem = dataItem;
            post(new Runnable() {

                @Override
                public void run() {
                    doAddTag(finalDataItem);
                }
            });
        }
    }

    private void doAddTag(Object dataItem) {
        removeTextChangedListener(mTextWatcher);
        addTagInternal(dataItem);
        addTextChangedListener(mTextWatcher);
    }

    private boolean hasRoomForTag() {
        if (!mIsCollapsed) {
            return true;
        }
        if (getTags().size() < mMaxTagsWhenCollapsed) {
            return true;
        }
        return false;
    }

    /**
     * Adds a tag to the end of the list without removing the text changed
     * listener.
     * 
     * @param dataItem The data item to add.
     */
    private void addTagInternal(Object dataItem) {
        if (hasRoomForTag()) {
            Editable editable = getText();
            editable.append(mTokenizer.terminateToken(dataItem.toString()));
            Selection.setSelection(editable, editable.length());
            submitItem(dataItem);
        } else {
            mHiddenTags.add(createBaseTag(dataItem));
            updateMoreSpan();
        }
    }

    /**
     * Adds a tag to the current cursor position in the list.
     * 
     * @param dataItem The data item to add.
     */
    public void addTagAtCursor(Object dataItem) {
        // This is to fix a UI glitch when adding tags
        // before the widget has been sized the first time.
        if (getMeasuredWidth() > 0) {
            doAddTagAtCursor(dataItem);
        } else {
            final Object finalDataItem = dataItem;
            post(new Runnable() {

                @Override
                public void run() {
                    doAddTagAtCursor(finalDataItem);
                }
            });
        }
    }

    private void doAddTagAtCursor(Object dataItem) {
        int cursorPosition = getSelectionStart();
        if (cursorPosition == -1) {
            addTag(dataItem);
        } else {
            removeTextChangedListener(mTextWatcher);
            Editable editable = getText();
            editable.insert(cursorPosition, mTokenizer.terminateToken(dataItem.toString()));
            // Selection.setSelection(editable, editable.length());
            submitItem(dataItem);
            addTextChangedListener(mTextWatcher);
        }
    }

    private int getClearDrawableWidth() {
        if (getCompoundDrawables()[2] != mClearDrawable) {
            return 0;
        }
        return mClearDrawable.getIntrinsicWidth();
    }

    private boolean isTouchInsideClearDrawable(MotionEvent event) {
        // if the clear drawable is visible
        if (getCompoundDrawables()[2] != mClearDrawable) {
            return false;
        }
        // if the click is not on the right portion of the widget where the
        // clear button is.
        if (event.getX() > getMeasuredWidth() - getPaddingRight()
                - mClearDrawable.getIntrinsicWidth()) {
            // if the click is not on the middle portion (vertically) of the
            // widget where the clear button is.
            int halfClearHeight = mClearDrawable.getIntrinsicHeight() / 2;
            int emptySpace = (getMeasuredHeight() / 2) - halfClearHeight;
            if ((event.getY() > emptySpace) && (event.getY() < getMeasuredHeight() - emptySpace)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if (!isReadOnly()) {
            if (!isFocused()) {
                // let this control become focused the normal way.
                return result;
            }

            mGestureDetector.onTouchEvent(event);
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isTouchInsideClearDrawable(event)) {
                setText("");
            } else {
                int charOffset = getOffsetForPosition(event.getX(), event.getY());
                TagImageSpan[] tags = getText()
                        .getSpans(charOffset, charOffset, TagImageSpan.class);
                if (tags.length > 0) {
                    tags[0].getTag().setReadOnly(mReadOnly);
                    View tagDetailsView = tags[0].getTag().getDetailsView(getContext());
                    mDetailsView.setContentView(tagDetailsView);
                    mDetailsView.setWidth(getWidth() - (int) mCompletionsLeftOffset);
                    mDetailsView.showAsDropDown(this, (int) mCompletionsLeftOffset, -getHeight());
                    hideSoftKeyboard();

                    if (tagDetailsView instanceof IDeletable) {
                        ((IDeletable) tagDetailsView)
                                .setOnDeleteClickListener(mOnDeleteClickListener);
                    }
                } else {
                    // check for the more tag and expand if required.
                    MoreImageSpan[] moreTags = getText().getSpans(charOffset, charOffset,
                            MoreImageSpan.class);
                    if (moreTags.length > 0) {
                        expandControl();
                    }
                }

            }
        }

        return result;
    }

    public boolean isReadOnly() {
        return mReadOnly;
    }

    public void setReadOnly(boolean readOnly) {
        mReadOnly = readOnly;

        setFocusable(!mReadOnly);
        setCursorVisible(!mReadOnly);
        setFocusableInTouchMode(!mReadOnly);
    }

    public List<BaseTag> getTags() {
        TagImageSpan[] tagSpans = getSortedTagSpans();
        ArrayList<BaseTag> result = new ArrayList<BaseTag>(tagSpans.length);
        for (TagImageSpan tagImageSpan : tagSpans) {
            result.add(tagImageSpan.getTag());
        }
        for (BaseTag tag : mHiddenTags) {
            result.add(tag);
        }
        return result;
    }

    /**
     * @return The listener registered
     */
    public OnTagListChanged<BaseTag> getOnTagListChanged() {
        return mOnTagListChanged;
    }

    /**
     * @param onTagListChanged The listener to set.
     */
    public void setOnTagListChanged(OnTagListChanged<BaseTag> onTagListChanged) {
        mOnTagListChanged = onTagListChanged;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        int charOffset = getOffsetForPosition(e.getX(), e.getY());
        TagImageSpan[] tags = getText().getSpans(charOffset, charOffset, TagImageSpan.class);
        if (tags.length > 0) {
            startDrag(tags[0]);
        }
    }

    private void startDrag(TagImageSpan tagImageSpan) {
        // we don't care about clipdata TBH. Wondering if we can just not pass
        // it in.
        ClipData.Item clipItem = new ClipData.Item("");
        ClipData clipData = new ClipData("", TAG_MIME, clipItem);
        int originalOffset = getText().getSpanStart(tagImageSpan);
        removeSpan(tagImageSpan);
        startDrag(clipData, new TagShadow(tagImageSpan), new TagLocalState(this, tagImageSpan,
                originalOffset), 0);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    private static final class TagShadow extends DragShadowBuilder {
        private final TagImageSpan mTagImageSpan;

        public TagShadow(TagImageSpan tagImageSpan) {
            mTagImageSpan = tagImageSpan;
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
            Rect rect = mTagImageSpan.getDrawable().getBounds();
            shadowSize.set(rect.width(), rect.height());
            shadowTouchPoint.set(rect.centerX(), rect.centerY());
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            mTagImageSpan.getDrawable().draw(canvas);
        }
    }

    private static final class TagLocalState {
        public TagTextView dragSource;
        public TagImageSpan tagImageSpan;
        public int originalOffset;

        public TagLocalState(TagTextView dragSource, TagImageSpan tagImageSpan, int originalOffset) {
            this.dragSource = dragSource;
            this.tagImageSpan = tagImageSpan;
            this.originalOffset = originalOffset;
        }
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // Only handle plain text drag and drop.
                return event.getClipDescription().hasMimeType(TAG_MIME[0]);
            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DRAG_LOCATION:
                // this will give us focus and keep the cursor in the proper
                // location
                return super.onDragEvent(event);
            case DragEvent.ACTION_DROP:
                if (event.getLocalState() instanceof TagLocalState) {
                    TagLocalState localState = (TagLocalState) event.getLocalState();
                    if (localState.dragSource.getDragGroup().equals(getDragGroup())) {
                        if (mTagClass.isInstance(localState.tagImageSpan.getTag())) {
                            addTagAtCursor(localState.tagImageSpan.getTag().getData());
                            return true;
                        }
                    }
                }
                return false;
            case DragEvent.ACTION_DRAG_ENDED:
                if (!event.getResult()) {
                    // it was dropped somewhere bad. Put it back!
                    if (event.getLocalState() instanceof TagLocalState) {
                        TagLocalState localState = (TagLocalState) event.getLocalState();
                        if (localState.dragSource == this) {
                            setSelection(Math.min(getText().length(), localState.originalOffset));
                            addTagAtCursor(localState.tagImageSpan.getTag().getData());
                        }
                    }
                }
                return true;
        }
        return false;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    private void removeSpan(Object span) {
        Editable text = getText();
        int spanStart = text.getSpanStart(span);
        int spanEnd = text.getSpanEnd(span);
        int toDelete = spanEnd;

        // Always remove trailing spaces when removing a tag.
        while (toDelete >= 0 && toDelete < text.length() && text.charAt(toDelete) == ' ') {
            toDelete++;
        }
        // Always remove preceding spaces when removing a tag.
        while (spanStart >= 0 && spanStart < text.length() && text.charAt(spanStart) == ' ') {
            spanStart--;
        }
        // by deleting the text the Span wraps around it automatically removes
        // the span
        if (spanStart >= 0 && toDelete > 0) {
            text.delete(spanStart, toDelete);
        }
    }

    /**
     * The drag group is a string which is used to allow drag and drop of tags
     * from different TagTextView controls. The drag group from the source
     * TagTextView must match the drag group from the destination TagTextView or
     * the drop will not be allowed.
     * 
     * @return The drag group for this control.
     * @see #setDragGroup(String)
     */
    public String getDragGroup() {
        return mDragGroup;
    }

    /**
     * Sets the drag group.
     * 
     * @param dragGroup The new drag group to set.
     * @see #getDragGroup()
     */
    public void setDragGroup(String dragGroup) {
        mDragGroup = dragGroup;
    }

    public int getMaxTagsWhenCollapsed() {
        return mMaxTagsWhenCollapsed;
    }

    public void setMaxTagsWhenCollapsed(int maxTagsWhenCollapsed) {
        if (mMaxTagsWhenCollapsed != maxTagsWhenCollapsed) {
            mMaxTagsWhenCollapsed = maxTagsWhenCollapsed;
            if (mMaxTagsWhenCollapsed < 0) {
                expandControl();
            } else if (mIsCollapsed) {
                expandControl();
                collapseControl();
            }
        }
    }

    private static class MoreAttributes {
        public int height;
        private float textSize;
        private int paddingLeft;
        private int paddingRight;
        private int textColor;
        private String text;
    }

    private static class MoreImageSpan extends ImageSpan {
        private MoreImageSpan(Drawable drawable) {
            super(drawable);
        }

        public static MoreImageSpan newInstance(Context context, Paint paint, int count,
                MoreAttributes attributes) {

            String moreText = String.format(attributes.text, count);
            TextPaint morePaint = new TextPaint(paint);
            morePaint.setTextSize(attributes.textSize);
            morePaint.setColor(attributes.textColor);
            Rect textBounds = new Rect();
            morePaint.getTextBounds(moreText, 0, moreText.length(), textBounds);
            int w = textBounds.width() + attributes.paddingLeft + attributes.paddingRight;
            int h = attributes.height;

            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            canvas.drawText(moreText, 0, moreText.length(), 0,
                    h - ((h - textBounds.height()) / 2), morePaint);

            Drawable result = new BitmapDrawable(context.getResources(), b);
            result.setBounds(0, 0, w, h);
            return new MoreImageSpan(result);
        }
    }

    private void collapseControl() {
        // yes we can allow no tags to be shown. Possibly stupid, but available
        // nonetheless.
        if (mIsCollapsed || (mMaxTagsWhenCollapsed < 0)) {
            return;
        }

        // ABOVE the length check or else adding items programattically will
        // keep adding to the list.
        mIsCollapsed = true;

        TagImageSpan[] tagSpans = getSortedTagSpans();
        if (tagSpans.length <= mMaxTagsWhenCollapsed) {
            return;
        }

        // we have too many tags showing. Fix that.
        int hiddenTagCount = tagSpans.length - mMaxTagsWhenCollapsed;

        removeTextChangedListener(mTextWatcher);
        // clear out the current tags
        setText("");
        // add the max number of tags.
        for (int i = 0; i < mMaxTagsWhenCollapsed; i += 1) {
            addTagInternal(tagSpans[i].getTag().getData());
        }
        // keep track of the rest of the data items (spans and BaseTag objects
        // can go away).
        for (int i = mMaxTagsWhenCollapsed; i < tagSpans.length; i += 1) {
            mHiddenTags.add(tagSpans[i].getTag());
        }

        updateMoreSpan();

        addTextChangedListener(mTextWatcher);
    }

    private void updateMoreSpan() {
        Editable text = getText();
        MoreImageSpan[] moreSpans = text.getSpans(0, text.length(), MoreImageSpan.class);
        if (moreSpans.length > 0) {
            removeSpan(moreSpans[0]);
        }

        int hiddenTagCount = mHiddenTags.size();
        if (hiddenTagCount > 0) {
            MoreImageSpan moreSpan = MoreImageSpan.newInstance(getContext(), getPaint(),
                    hiddenTagCount, mMoreAttributes);
            CharSequence textToMoreSpan = mTokenizer.terminateToken("MORE");
            int spanStart = text.length();
            text.append(textToMoreSpan);
            int spanEnd = text.length() - 1;
            SpannableString moreSpanText = new SpannableString(textToMoreSpan);
            moreSpanText.setSpan(moreSpan, 0, moreSpanText.length() - 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.replace(spanStart, spanEnd, moreSpanText);
        }
    }

    private void expandControl() {
        if (!mIsCollapsed) {
            return;
        }
        removeTextChangedListener(mTextWatcher);
        Editable text = getText();
        MoreImageSpan[] moreSpans = text.getSpans(0, text.length(), MoreImageSpan.class);
        if (moreSpans.length > 0) {
            removeSpan(moreSpans[0]);
        }
        mIsCollapsed = false;
        for (BaseTag tag : mHiddenTags) {
            addTagInternal(tag.getData());
        }
        mHiddenTags.clear();
        addTextChangedListener(mTextWatcher);
    }

    private void removeTag(BaseTag tag) {
        Editable text = getText();
        TagImageSpan[] spans = text.getSpans(0, text.length(), TagImageSpan.class);
        for (TagImageSpan tagImageSpan : spans) {
            // yes use reference matching, not equals. I see no reason not to
            // here.
            if (tagImageSpan.getTag() == tag) {
                removeSpan(tagImageSpan);
                return;
            }
        }
    }

    private void hideSoftKeyboard() {
        mKeyboardWasHidden = mImeManager.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    private void showSoftKeyboard() {
        // This only works when posted to the Handler. Why? Not exactly sure.
        // Something weird to do with the PopupWindow's on dismissed callback.
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                mImeManager.showSoftInput(TagTextView.this, 0);
            }
        });
    }

    /**
     * Decide whether or not the clear drawable should be showing. Currently it
     * is shown if and only if we are focused and have non-empty text.
     */
    private void autoShowClearDrawable() {
        setShowClearDrawable(isFocused() && !TextUtils.isEmpty(getText().toString()));
    }

    private void setShowClearDrawable(boolean show) {
        Drawable[] compoundDrawables = getCompoundDrawables();
        if (show) {
            compoundDrawables[2] = mClearDrawable;
        } else {
            compoundDrawables[2] = null;
        }
        setCompoundDrawables(compoundDrawables[0], compoundDrawables[1], compoundDrawables[2],
                compoundDrawables[3]);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        // If we don't explicitly hide it here an exception is thrown to LogCat
        // that we leaked a Window.
        if (mDetailsView.isShowing()) {
            mDetailsView.dismiss();
        }
        return super.onSaveInstanceState();
    }

    private void printTagsDebugState() {
        TagImageSpan[] spans = getSortedTagSpans();
        Log.d("TagTextView", "Cursor: " + getSelectionStart());
        for (int i = 0; i < spans.length; i += 1) {
            int tagStart = getText().getSpanStart(spans[i]);
            int tagEnd = getText().getSpanEnd(spans[i]);
            Log.d("TagTextView", "Span: \"" + spans[i].getTag().getLabel() + "\" Start: "
                    + tagStart + " End: " + tagEnd);
        }
        for (BaseTag tag : mHiddenTags) {
            Log.d("TagTextView", "Hidden: \"" + tag.getLabel() + "\"");
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if ((w > 0) && (w != oldw)) {
            // we have more (or less) space. We need to re-size all of the
            // tags so it is simpler to clear the list and re-add them all.
            // Must be in a Runnable posted to the looper or else it doesn't
            // work in certain cases.
            post(new Runnable() {

                @Override
                public void run() {
                    replaceAllTags();
                }
            });

        }
    }

    /**
     * Remove all tags and re-add them. This is useful to re-size all tags in
     * one swoop. No notifications will be sent to the change listeners.
     */
    private void replaceAllTags() {
        mNoChangeNotifications = true;
        List<BaseTag> tags = getTags();
        setText("");
        for (BaseTag baseTag : tags) {
            addTag(baseTag.getData());
        }
        mNoChangeNotifications = false;
    }
}
