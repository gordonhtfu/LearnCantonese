/**
 * Copyright (c) 2011, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blackberry.email.ui.browse;

import com.blackberry.common.perf.Timer;
import com.blackberry.common.utils.LogTag;
import com.blackberry.common.utils.LogUtils;
import com.blackberry.email.Account;
import com.blackberry.email.Address;
import com.blackberry.email.ContactInfo;
import com.blackberry.email.ContactInfoSource;
import com.blackberry.email.photomanager.LetterTileProvider;
import com.blackberry.email.provider.UIProvider;
import com.blackberry.email.ui.ImageCanvas;
import com.blackberry.email.ui.browse.ConversationViewAdapter.BorderItem;
import com.blackberry.email.ui.browse.ConversationViewAdapter.MessageHeaderItem;
import com.blackberry.email.utils.UtilsEx;
import com.blackberry.email.utils.VeiledAddressMatcher;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.provider.MessageContract;
import com.blackberry.widgets.tagview.contact.email.EmailTags;
import com.google.common.annotations.VisibleForTesting;

import android.app.FragmentManager;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Map;

public class MessageHeaderView extends LinearLayout implements OnClickListener,
        OnMenuItemClickListener, ConversationContainer.DetachListener {

    /**
     * Cap very long recipient lists during summary construction for efficiency.
     */
    private static final int SUMMARY_MAX_RECIPIENTS = 50;

    private static final int MAX_SNIPPET_LENGTH = 100;

    private static final int SHOW_IMAGE_PROMPT_ONCE = 1;
    private static final int SHOW_IMAGE_PROMPT_ALWAYS = 2;

    private static final String HEADER_RENDER_TAG = "message header render";
    private static final String LAYOUT_TAG = "message header layout";
    private static final String MEASURE_TAG = "message header measure";

    private static final String RECIPIENT_HEADING_DELIMITER = "   ";

    private static final String LOG_TAG = LogTag.getLogTag();

    // This is a debug only feature
    public static final boolean ENABLE_REPORT_RENDERING_PROBLEM = false;

    private MessageHeaderViewCallbacks mCallbacks;

    private ViewGroup mUpperHeaderView;
    private View mSnapHeaderBottomBorder;
    private TextView mSenderNameView;
    private TextView mDateView;
    private TextView mSnippetView;
    private QuickContactBadge mPhotoView;
    private ViewGroup mTitleContainerView;
    private ViewGroup mExtraContentView;
    private View mCollapsedDetailsView;
    private View mExpandedDetailsView;
    private SpamWarningView mSpamWarningView;
    private TextView mImagePromptView;
    private MessageInviteView mInviteView;
    private View mDraftIcon;
    private TextView mAttachmentRecipientCountLabel;
    private final EmailCopyContextMenu mEmailCopyMenu;
    private ViewGroup mExpandableHeaderContent;
    private ImageView mExpandArrow;

    // temporary fields to reference raw data between initial render and details
    // expansion
    private String[] mFrom;
    private String[] mTo;
    private String[] mCc;
    private String[] mBcc;
    private String[] mReplyTo;

    private boolean mIsDraft = false;

    private String mSnippet;

    private Address mSender;

    private ContactInfoSource mContactInfoSource;

    private boolean mPreMeasuring;

    private ConversationAccountController mAccountController;

    private Map<String, Address> mAddressCache;

    private boolean mShowImagePrompt;

    /**
     * End margin of the text when collapsed. When expanded, the margin is 0.
     */
    private int mTitleContainerCollapsedMarginEnd;

    private PopupMenu mPopup;

    private MessageHeaderItem mMessageHeaderItem;
    private ConversationMessage mMessage;

    private final LayoutInflater mInflater;

    private AsyncQueryHandler mQueryHandler;

    private boolean mObservingContactInfo;

    private final DataSetObserver mContactInfoObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            updateContactInfo();
        }
    };

    private boolean mExpandable = true;

    private VeiledAddressMatcher mVeiledMatcher;

    private boolean mIsViewOnlyMode = false;

    private LetterTileProvider mLetterTileProvider;
    private final int mContactPhotoWidth;
    private final int mContactPhotoHeight;

    public interface MessageHeaderViewCallbacks {
        void setMessageSpacerHeight(MessageHeaderItem item, int newSpacerHeight);

        void setMessageExpanded(MessageHeaderItem item, int newSpacerHeight,
                int topBorderHeight, int bottomBorderHeight);

        void setMessageDetailsExpanded(MessageHeaderItem messageHeaderItem, boolean expanded,
                int previousMessageHeaderItemHeight);

        void showExternalResources();

        FragmentManager getFragmentManager();
    }

    public MessageHeaderView(Context context) {
        this(context, null);
    }

    public MessageHeaderView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public MessageHeaderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mEmailCopyMenu = new EmailCopyContextMenu(getContext());
        mInflater = LayoutInflater.from(context);

        final Resources resources = getResources();
        mContactPhotoWidth = resources.getDimensionPixelSize(
                R.dimen.message_header_contact_photo_width);
        mContactPhotoHeight = resources.getDimensionPixelSize(
                R.dimen.message_header_contact_photo_height);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mUpperHeaderView = (ViewGroup) findViewById(R.id.upper_header);
        mSnapHeaderBottomBorder = findViewById(R.id.snap_header_bottom_border);
        mSenderNameView = (TextView) findViewById(R.id.sender_name);
        mDateView = (TextView) findViewById(R.id.send_date);
        mSnippetView = (TextView) findViewById(R.id.email_snippet);
        mPhotoView = (QuickContactBadge) findViewById(R.id.photo);
        mTitleContainerView = (ViewGroup) findViewById(R.id.title_container);
        mDraftIcon = findViewById(R.id.draft);
        mAttachmentRecipientCountLabel = (TextView) findViewById(R.id.attachment_recipient_count_label);
        mExpandableHeaderContent = (ViewGroup) findViewById(R.id.expandable_header_content);
        mExtraContentView = (ViewGroup) findViewById(R.id.header_extra_content);
        mCollapsedDetailsView = (View) findViewById(R.id.inner_header);
        mExpandArrow = (ImageView) findViewById(R.id.expand_header_button);

        final Resources resources = getResources();
        mTitleContainerCollapsedMarginEnd = resources.getDimensionPixelSize(
                R.dimen.message_header_title_container_margin_end_collapsed);

        setExpanded(true);

        registerMessageClickTargets(R.id.reply, R.id.reply_all, R.id.forward, R.id.star,
                R.id.edit_draft, R.id.overflow, R.id.expand_header_summary);

        mUpperHeaderView.setOnCreateContextMenuListener(mEmailCopyMenu);
    }

    private void registerMessageClickTargets(int... ids) {
        for (int id : ids) {
            View v = findViewById(id);
            if (v != null) {
                v.setOnClickListener(this);
            }
        }
    }

    /**
     * Associate the header with a contact info source for later contact
     * presence/photo lookup.
     */
    public void setContactInfoSource(ContactInfoSource contactInfoSource) {
        mContactInfoSource = contactInfoSource;
    }

    public void setCallbacks(MessageHeaderViewCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    public void setVeiledMatcher(VeiledAddressMatcher matcher) {
        mVeiledMatcher = matcher;
    }

    public boolean isExpanded() {
        // (let's just arbitrarily say that unbound views are expanded by default)
        return mMessageHeaderItem == null || mMessageHeaderItem.isExpanded();
    }

    @Override
    public void onDetachedFromParent() {
        unbind();
    }

    /**
     * Headers that are unbound will not match any rendered header (matches()
     * will return false). Unbinding is not guaranteed to *hide* the view's old
     * data, though. To re-bind this header to message data, call render() or
     * renderUpperHeaderFrom().
     */
    public void unbind() {
        mMessageHeaderItem = null;
        mMessage = null;

        if (mObservingContactInfo) {
            mContactInfoSource.unregisterObserver(mContactInfoObserver);
            mObservingContactInfo = false;
        }
    }

    public void initialize(ConversationAccountController accountController,
            Map<String, Address> addressCache) {
        mAccountController = accountController;
        mAddressCache = addressCache;
    }

    private Account getAccount() {
        return mAccountController != null ? mAccountController.getAccount() : null;
    }

    public void bind(MessageHeaderItem headerItem, boolean measureOnly) {
        if (mMessageHeaderItem != null && mMessageHeaderItem == headerItem) {
            return;
        }
        mMessageHeaderItem = headerItem;
        mMessageHeaderItem.detailsExpanded = (mExpandedDetailsView != null && mExpandedDetailsView.getVisibility() == VISIBLE);
        render(measureOnly);
    }

    /**
     * Rebinds the view to its data. This will only update the view
     * if the {@link MessageHeaderItem} sent as a parameter is the
     * same as the view's current {@link MessageHeaderItem} and the
     * view's expanded state differs from the item's expanded state.
     */
    public void rebind(MessageHeaderItem headerItem) {
        if (mMessageHeaderItem == null || mMessageHeaderItem != headerItem ||
                isActivated() == isExpanded()) {
            return;
        }

        render(false /* measureOnly */);
    }

    public void refresh() {
        render(false);
    }

    private void render(boolean measureOnly) {
        if (mMessageHeaderItem == null) {
            return;
        }

        Timer t = new Timer();
        t.start(HEADER_RENDER_TAG);

        mMessage = mMessageHeaderItem.getMessage();
        mShowImagePrompt = mMessage.shouldShowImagePrompt();
        setExpanded(mMessageHeaderItem.isExpanded());

        mFrom = mMessage.getAddresses(MessageContract.MessageContact.FieldType.FROM);
        mTo = mMessage.getAddresses(MessageContract.MessageContact.FieldType.TO);
        mCc = mMessage.getAddresses(MessageContract.MessageContact.FieldType.CC);
        mBcc = mMessage.getAddresses(MessageContract.MessageContact.FieldType.BCC);
        mReplyTo = mMessage.getAddresses(MessageContract.MessageContact.FieldType.REPLY_TO);

        /**
         * Turns draft mode on or off. Draft mode hides message operations other
         * than "edit", hides contact photo, hides presence, and changes the
         * sender name to "Draft".
         */
        mIsDraft = (mMessage.mState & MessageContract.Message.State.DRAFT) != 0;

        // If this was a sent message AND:
        // 1. the account has a custom from, the cursor will populate the
        // selected custom from as the fromAddress when a message is sent but
        // not yet synced.
        // 2. the account has no custom froms, fromAddress will be empty, and we
        // can safely fall back and show the account name as sender since it's
        // the only possible fromAddress.
        String from = mMessage.mSender;
        if (TextUtils.isEmpty(from)) {
            from = getAccount().name;
        }
        mSender = getAddress(from);

        StringBuilder attachRecipCountBuilder = new StringBuilder();
        final int attachCount = mMessage.getAttachments().size();
        if (attachCount > 0) {
            attachRecipCountBuilder.append(getResources().getQuantityString(R.plurals.message_view_attachment_count, attachCount, attachCount));
        }
        int recipCount = mMessage.getContacts(MessageContract.MessageContact.FieldType.TO).size();
        recipCount += mMessage.getContacts(MessageContract.MessageContact.FieldType.CC).size();
        recipCount += mMessage.getContacts(MessageContract.MessageContact.FieldType.BCC).size();
        if (recipCount > 0) {
            attachRecipCountBuilder.append(getResources().getQuantityString(R.plurals.message_view_recipient_count, recipCount, recipCount));
        }
        if (attachRecipCountBuilder.length() > 0) {
            mAttachmentRecipientCountLabel.setText(attachRecipCountBuilder.toString());
        }

        updateChildVisibility();

        if (mIsDraft) {
            mSnippet = makeSnippet(mMessage.mSnippet);
        } else {
            mSnippet = mMessage.mSnippet;
        }

        mSenderNameView.setText(getHeaderTitle());
        mDateView.setText(mMessageHeaderItem.getTimestampLong());
        mSnippetView.setText(mSnippet);
        setAddressOnContextMenu();

        if (measureOnly) {
            // avoid leaving any state around that would interfere with future regular bind() calls
            unbind();
        } else {
            updateContactInfo();
            if (!mObservingContactInfo) {
                mContactInfoSource.registerObserver(mContactInfoObserver);
                mObservingContactInfo = true;
            }
        }

        t.pause(HEADER_RENDER_TAG);
    }

    /**
     * Update context menu's address field for when the user long presses
     * on the message header and attempts to copy/send email.
     */
    private void setAddressOnContextMenu() {
        mEmailCopyMenu.setAddress(mSender.getAddress());
    }

    public boolean isBoundTo(ConversationOverlayItem item) {
        return item == mMessageHeaderItem;
    }

    public Address getAddress(String emailStr) {
        return getAddress(mAddressCache, emailStr);
    }

    public static Address getAddress(Map<String, Address> cache, String emailStr) {
        Address addr = null;
        synchronized (cache) {
            if (cache != null) {
                addr = cache.get(emailStr);
            }
            if (addr == null) {
                addr = Address.getEmailAddress(emailStr);
                if (cache != null) {
                    cache.put(emailStr, addr);
                }
            }
        }
        return addr;
    }

    private void updateSpacerHeight() {
        final int h = measureHeight();

        mMessageHeaderItem.setHeight(h);
        if (mCallbacks != null) {
            mCallbacks.setMessageSpacerHeight(mMessageHeaderItem, h);
        }
    }

    private int measureHeight() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) {
            LogUtils.e(LOG_TAG, new Error(), "Unable to measure height of detached header");
            return getHeight();
        }
        mPreMeasuring = true;
        final int h = UtilsEx.measureViewHeight(this, parent);
        mPreMeasuring = false;
        return h;
    }

    private CharSequence getHeaderTitle() {
        CharSequence title;

        if (mIsDraft) {
            title = getResources().getQuantityText(R.plurals.draft, 1);
        } else {
            title = getSenderName(mSender);
        }

        return title;
    }

    /**
     * Return the name, if known, or just the address.
     */
    private static CharSequence getSenderName(Address sender) {
        final String displayName = sender.getName();
        return TextUtils.isEmpty(displayName) ? sender.getAddress() : displayName;
    }


    private static void setChildVisibility(int visibility, View... children) {
        for (View v : children) {
            if (v != null) {
                v.setVisibility(visibility);
            }
        }
    }

    private void setExpanded(final boolean expanded) {
        // use View's 'activated' flag to store expanded state
        // child view state lists can use this to toggle drawables
        setActivated(expanded);
        if (mMessageHeaderItem != null) {
            mMessageHeaderItem.setExpanded(expanded);
        }
    }

    /**
     * Update the visibility of the many child views based on expanded/collapsed
     * and draft/normal state.
     */
    private void updateChildVisibility() {
        // Too bad this can't be done with an XML state list...

        if (mIsViewOnlyMode) {
            setMessageDetailsVisibility(VISIBLE);
            setChildVisibility(GONE, mSnapHeaderBottomBorder);

            setChildVisibility(GONE, mDraftIcon, mSnippetView);
            setChildVisibility(VISIBLE, mPhotoView, mDateView);

            setChildMarginEnd(mTitleContainerView, 0);
        } else if (isExpanded()) {
            int normalVis, draftVis;

            setMessageDetailsVisibility(VISIBLE);
            setChildVisibility(GONE, mSnapHeaderBottomBorder);

            if (mIsDraft) {
                normalVis = GONE;
                draftVis = VISIBLE;
            } else {
                normalVis = VISIBLE;
                draftVis = GONE;
            }

            setChildVisibility(normalVis, mPhotoView);
            setChildVisibility(draftVis, mDraftIcon);
            setChildVisibility(VISIBLE, mDateView);
            setChildVisibility(GONE, mSnippetView);

            setChildMarginEnd(mTitleContainerView, 0);

            setChildVisibility(VISIBLE, mAttachmentRecipientCountLabel);

        } else {

            setMessageDetailsVisibility(GONE);
            setChildVisibility(GONE, mSnapHeaderBottomBorder);
            setChildVisibility(VISIBLE, mSnippetView);

            setChildVisibility(GONE, mDateView);

            setChildMarginEnd(mTitleContainerView, mTitleContainerCollapsedMarginEnd);

            if (mIsDraft) {

                setChildVisibility(VISIBLE, mDraftIcon);
                setChildVisibility(GONE, mPhotoView);

            } else {

                setChildVisibility(GONE, mDraftIcon);
                setChildVisibility(VISIBLE, mPhotoView);

            }
        }
    }

    private static void setChildMarginEnd(View childView, int marginEnd) {
        MarginLayoutParams mlp = (MarginLayoutParams) childView.getLayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mlp.setMarginEnd(marginEnd);
        } else {
            mlp.rightMargin = marginEnd;
        }
        childView.setLayoutParams(mlp);
    }

    /**
     * Utility class to build a list of recipient lists.
     */
    private static class RecipientListsBuilder {
        private final Context mContext;
        private final String mMe;
        private final String mMyName;
        private final SpannableStringBuilder mBuilder = new SpannableStringBuilder();
        private final CharSequence mComma;
        private final Map<String, Address> mAddressCache;
        private final VeiledAddressMatcher mMatcher;

        int mRecipientCount = 0;
        boolean mFirst = true;

        public RecipientListsBuilder(Context context, String me, String myName,
                Map<String, Address> addressCache, VeiledAddressMatcher matcher) {
            mContext = context;
            mMe = me;
            mMyName = myName;
            mComma = mContext.getText(R.string.enumeration_comma);
            mAddressCache = addressCache;
            mMatcher = matcher;
        }

        public void append(String[] recipients, int headingRes) {
            int addLimit = SUMMARY_MAX_RECIPIENTS - mRecipientCount;
            CharSequence recipientList = getSummaryTextForHeading(headingRes, recipients, addLimit);
            if (recipientList != null) {
                // duplicate TextUtils.join() logic to minimize temporary
                // allocations, and because we need to support spans
                if (mFirst) {
                    mFirst = false;
                } else {
                    mBuilder.append(RECIPIENT_HEADING_DELIMITER);
                }
                mBuilder.append(recipientList);
                mRecipientCount += Math.min(addLimit, recipients.length);
            }
        }

        private CharSequence getSummaryTextForHeading(int headingStrRes, String[] rawAddrs,
                int maxToCopy) {
            if (rawAddrs == null || rawAddrs.length == 0 || maxToCopy == 0) {
                return null;
            }

            SpannableStringBuilder ssb = new SpannableStringBuilder(
                    mContext.getString(headingStrRes));
            ssb.setSpan(new StyleSpan(Typeface.NORMAL), 0, ssb.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            final int len = Math.min(maxToCopy, rawAddrs.length);
            boolean first = true;
            for (int i = 0; i < len; i++) {
                final Address email = getAddress(mAddressCache, rawAddrs[i]);
                final String emailAddress = email.getAddress();
                final String name;
                if (mMatcher != null && mMatcher.isVeiledAddress(emailAddress)) {
                    if (TextUtils.isEmpty(email.getName())) {
                        // Let's write something more readable.
                        name = mContext.getString(VeiledAddressMatcher.VEILED_SUMMARY_UNKNOWN);
                    } else {
                        name = email.getSimplifiedName();
                    }
                } else {
                    // Not a veiled address, show first part of email, or "me".
                    name = mMe.equals(emailAddress) ? mMyName : email.getSimplifiedName();
                }

                // duplicate TextUtils.join() logic to minimize temporary
                // allocations, and because we need to support spans
                if (first) {
                    first = false;
                } else {
                    ssb.append(mComma);
                }
                ssb.append(name);
            }

            return ssb;
        }

        public CharSequence build() {
            return mBuilder;
        }
    }

    @VisibleForTesting
    static CharSequence getRecipientSummaryText(Context context, String me, String myName,
            String[] to, String[] cc, String[] bcc, Map<String, Address> addressCache,
            VeiledAddressMatcher matcher) {

        final RecipientListsBuilder builder =
                new RecipientListsBuilder(context, me, myName, addressCache, matcher);

        builder.append(to, R.string.to_heading);
        builder.append(cc, R.string.cc_heading);
        builder.append(bcc, R.string.bcc_heading);

        return builder.build();
    }

    private void updateContactInfo() {
        if (mContactInfoSource == null || mSender == null) {
            mPhotoView.setImageToDefault();
            mPhotoView.setContentDescription(getResources().getString(
                    R.string.contact_info_string_default));
            return;
        }

        // Set the photo to either a found Bitmap or the default
        // and ensure either the contact URI or email is set so the click
        // handling works
        String contentDesc = getResources().getString(R.string.contact_info_string,
                !TextUtils.isEmpty(mSender.getName()) ? mSender.getName() : mSender.getAddress());
        mPhotoView.setContentDescription(contentDesc);
        boolean photoSet = false;
        final String email = mSender.getAddress();
        final ContactInfo info = mContactInfoSource.getContactInfo(email);
        if (info != null) {
            mPhotoView.assignContactUri(info.contactUri);
            if (info.photo != null) {
                mPhotoView.setImageBitmap(info.photo);
                photoSet = true;
            }
        } else {
            mPhotoView.assignContactFromEmail(email, true /* lazyLookup */);
        }

        if (!photoSet) {
            mPhotoView.setImageBitmap(makeLetterTile(mSender.getName(), email));
        }
    }

    private Bitmap makeLetterTile(
            String displayName, String senderAddress) {
        if (mLetterTileProvider == null) {
            mLetterTileProvider = new LetterTileProvider(getContext());
        }

        final ImageCanvas.Dimensions dimensions = new ImageCanvas.Dimensions(
                mContactPhotoWidth, mContactPhotoHeight, ImageCanvas.Dimensions.SCALE_ONE);
        return mLetterTileProvider.getLetterTile(dimensions, displayName, senderAddress);
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        mPopup.dismiss();
        return onClick(null, item.getItemId());
    }

    @Override
    public void onClick(View v) {
        onClick(v, v.getId());
    }

    /**
     * Handles clicks on either views or menu items. View parameter can be null
     * for menu item clicks.
     */
    public boolean onClick(final View v, final int id) {
        if (mMessage == null) {
            LogUtils.i(LOG_TAG, "ignoring message header tap on unbound view");
            return false;
        }

        boolean handled = true;

        if (id == R.id.overflow) {
            if (mPopup == null) {
                mPopup = new PopupMenu(getContext(), v);
                mPopup.getMenuInflater().inflate(R.menu.message_header_overflow_menu,
                        mPopup.getMenu());
                mPopup.setOnMenuItemClickListener(this);
            }
            final boolean defaultReplyAll = getAccount().settings.replyBehavior
                    == UIProvider.DefaultReplyBehavior.REPLY_ALL;
            final Menu m = mPopup.getMenu();
            m.findItem(R.id.reply).setVisible(defaultReplyAll);
            m.findItem(R.id.reply_all).setVisible(!defaultReplyAll);

            final boolean reportRendering = false;
            m.findItem(R.id.report_rendering_improvement).setVisible(reportRendering);
            m.findItem(R.id.report_rendering_problem).setVisible(reportRendering);

            mPopup.show();
        } else if (id == R.id.expand_header_summary) {
            toggleMessageDetails();
        } else if (id == R.id.upper_header) {
            toggleExpanded();
        } else if (id == R.id.show_pictures_text) {
            handleShowImagePromptClick(v);
        } else {
            LogUtils.i(LOG_TAG, "unrecognized header tap: %d", id);
            handled = false;
        }
        return handled;
    }

    /**
     * Set to true if the user should not be able to perfrom message actions
     * on the message such as reply/reply all/forward/star/etc.
     *
     * Default is false.
     */
    public void setViewOnlyMode(boolean isViewOnlyMode) {
        mIsViewOnlyMode = isViewOnlyMode;
    }

    public void setExpandable(boolean expandable) {
        mExpandable = expandable;
    }

    public void toggleExpanded() {
        if (!mExpandable) {
            return;
        }
        setExpanded(!isExpanded());

        mSenderNameView.setText(getHeaderTitle());
        mDateView.setText(mMessageHeaderItem.getTimestampLong());
        mSnippetView.setText(mSnippet);

        updateChildVisibility();

        final BorderHeights borderHeights = updateBorderExpandedState();

        // Force-measure the new header height so we can set the spacer size and
        // reveal the message div in one pass. Force-measuring makes it unnecessary to set
        // mSizeChanged.
        int h = measureHeight();
        mMessageHeaderItem.setHeight(h);
        if (mCallbacks != null) {
            mCallbacks.setMessageExpanded(mMessageHeaderItem, h,
                    borderHeights.topHeight, borderHeights.bottomHeight);
        }
    }

    /**
     * Checks the neighboring messages to this message and
     * updates the {@link BorderItem}s of the borders of this message
     * in case they should be collapsed or expanded.
     * @return a {@link BorderHeights} object containing
     * the new heights of the top and bottom borders.
     */
    private BorderHeights updateBorderExpandedState() {
        final int position = mMessageHeaderItem.getPosition();
        final boolean isExpanded = mMessageHeaderItem.isExpanded();
        final int abovePosition = position - 2; // position of MessageFooterItem above header
        final int belowPosition = position + 3; // position of next MessageHeaderItem
        final ConversationViewAdapter adapter = mMessageHeaderItem.getAdapter();
        final int size = adapter.getCount();
        final BorderHeights borderHeights = new BorderHeights();

        // if an above message exists, update the border above this message
        if (isValidPosition(abovePosition, size)) {
            final ConversationOverlayItem item = adapter.getItem(abovePosition);
            final int type = item.getType();
            if (type == ConversationViewAdapter.VIEW_TYPE_MESSAGE_FOOTER ||
                    type == ConversationViewAdapter.VIEW_TYPE_SUPER_COLLAPSED_BLOCK) {
                final BorderItem borderItem = (BorderItem) adapter.getItem(abovePosition + 1);
                final boolean borderIsExpanded = isExpanded || item.isExpanded();
                borderItem.setExpanded(borderIsExpanded);
                borderHeights.topHeight = borderIsExpanded ?
                        BorderView.getExpandedHeight() : BorderView.getCollapsedHeight();
                borderItem.setHeight(borderHeights.topHeight);
            }
        }


        // if a below message exists, update the border below this message
        if (isValidPosition(belowPosition, size)) {
            final ConversationOverlayItem item = adapter.getItem(belowPosition);
            if (item.getType() == ConversationViewAdapter.VIEW_TYPE_MESSAGE_HEADER) {
                final BorderItem borderItem = (BorderItem) adapter.getItem(belowPosition - 1);
                final boolean borderIsExpanded = isExpanded || item.isExpanded();
                borderItem.setExpanded(borderIsExpanded);
                borderHeights.bottomHeight = borderIsExpanded ?
                        BorderView.getExpandedHeight() : BorderView.getCollapsedHeight();
                borderItem.setHeight(borderHeights.bottomHeight);
            }
        }

        return borderHeights;
    }

    /**
     * A plain-old-data class used to return the new heights of the top and bottom borders
     * in {@link #updateBorderExpandedState()}.
     * If {@link #topHeight} or {@link #bottomHeight} are -1 after returning,
     * do not update the heights of the spacer for their respective borders
     * as their state has not changed.
     */
    private class BorderHeights {
        public int topHeight = -1;
        public int bottomHeight = -1;
    }

    private boolean isValidPosition(int position, int size) {
        return position >= 0 && position < size;
    }

    private void toggleMessageDetails() {
        int heightBefore = measureHeight();
        final boolean expandDetails;
        expandDetails = mExpandedDetailsView == null || mExpandedDetailsView.getVisibility() == GONE;
        setMessageDetailsExpanded(expandDetails);
        updateSpacerHeight();
        if (mCallbacks != null) {
            mCallbacks.setMessageDetailsExpanded(mMessageHeaderItem, expandDetails, heightBefore);
        }
    }

    private void setMessageDetailsExpanded(boolean expand) {
        if (expand) {
            showExpandedDetails();
            mExpandArrow.setRotation(180);
        } else {
            hideExpandedDetails();
            mExpandArrow.setRotation(0);
        }
        if (mMessageHeaderItem != null) {
            mMessageHeaderItem.detailsExpanded = expand;
        }
    }

    public void setMessageDetailsVisibility(int vis) {
        if (vis == GONE) {
            hideCollapsedDetails();
            hideExpandedDetails();
            hideSpamWarning();
            hideShowImagePrompt();
            hideInvite();
            mUpperHeaderView.setOnCreateContextMenuListener(null);
        } else {
            setMessageDetailsExpanded(mMessageHeaderItem.detailsExpanded);
            hideSpamWarning();
            if (mShowImagePrompt) {
                if (mMessageHeaderItem.getShowImages()) {
                    showImagePromptAlways(true);
                } else {
                    showImagePromptOnce();
                }
            } else {
                hideShowImagePrompt();
            }
            if (mMessage.isFlaggedCalendarInvite()) {
                showInvite();
            } else {
                hideInvite();
            }
            mUpperHeaderView.setOnCreateContextMenuListener(mEmailCopyMenu);
        }
    }

    public void hideMessageDetails() {
        setMessageDetailsVisibility(GONE);
    }

    private void hideCollapsedDetails() {
        if (mCollapsedDetailsView != null) {
            mCollapsedDetailsView.setVisibility(GONE);
        }
    }

    private void hideExpandedDetails() {
        if (mExpandedDetailsView != null) {
            mExpandedDetailsView.setVisibility(GONE);
        }
    }

    private void hideInvite() {
        if (mInviteView != null) {
            mInviteView.setVisibility(GONE);
        }
    }

    private void showInvite() {
        if (mInviteView == null) {
            mInviteView = (MessageInviteView) mInflater.inflate(
                    R.layout.conversation_message_invite, this, false);
            mExtraContentView.addView(mInviteView);
        }
        mInviteView.bind(mMessage);
        mInviteView.setVisibility(VISIBLE);
    }

    private void hideShowImagePrompt() {
        if (mImagePromptView != null) {
            mImagePromptView.setVisibility(GONE);
        }
    }

    private void showImagePromptOnce() {
        if (mImagePromptView == null) {
            mImagePromptView = (TextView) mInflater.inflate(
                    R.layout.conversation_message_show_pics, this, false);
            mExtraContentView.addView(mImagePromptView);
            mImagePromptView.setOnClickListener(this);
        }
        mImagePromptView.setVisibility(VISIBLE);
        mImagePromptView.setText(R.string.show_images);
        mImagePromptView.setTag(SHOW_IMAGE_PROMPT_ONCE);
    }

    /**
     * Shows the "Always show pictures" message
     *
     * @param initialShowing <code>true</code> if this is the first time we are showing the prompt
     *        for "show images", <code>false</code> if we are transitioning from "Show pictures"
     */
    private void showImagePromptAlways(final boolean initialShowing) {
        if (initialShowing) {
            // Initialize the view
            showImagePromptOnce();
        }

        mImagePromptView.setText(R.string.always_show_images);
        mImagePromptView.setTag(SHOW_IMAGE_PROMPT_ALWAYS);

        if (!initialShowing) {
            // the new text's line count may differ, so update the spacer height
            updateSpacerHeight();
        }
    }

    private void hideSpamWarning() {
        if (mSpamWarningView != null) {
            mSpamWarningView.setVisibility(GONE);
        }
    }

    private void handleShowImagePromptClick(View v) {
        Integer state = (Integer) v.getTag();
        if (state == null) {
            return;
        }
        switch (state) {
            case SHOW_IMAGE_PROMPT_ONCE:
                if (mCallbacks != null) {
                    mCallbacks.showExternalResources();
                }
                if (mMessageHeaderItem != null) {
                    mMessageHeaderItem.setShowImages(true);
                }
                if (mIsViewOnlyMode) {
                    hideShowImagePrompt();
                } else {
                    showImagePromptAlways(false);
                }
                break;
            case SHOW_IMAGE_PROMPT_ALWAYS:
                mMessage.markAlwaysShowImages(getQueryHandler(), 0 /* token */, null /* cookie */);

                if (mCallbacks != null) {
                    mCallbacks.showExternalResources();
                }

                mShowImagePrompt = false;
                v.setTag(null);
                v.setVisibility(GONE);
                updateSpacerHeight();
                Toast.makeText(getContext(), R.string.always_show_images_toast, Toast.LENGTH_SHORT)
                        .show();
                break;
        }
    }

    private AsyncQueryHandler getQueryHandler() {
        if (mQueryHandler == null) {
            mQueryHandler = new AsyncQueryHandler(getContext().getContentResolver()) {};
        }
        return mQueryHandler;
    }

    /**
     * Makes expanded details visible. If necessary, will inflate expanded
     * details layout and render using saved-off state (senders, timestamp,
     * etc).
     */
    private void showExpandedDetails() {
        // lazily create expanded details view
        final boolean expandedViewCreated = ensureExpandedDetailsView();
        if (expandedViewCreated) {
            mExpandableHeaderContent.addView(mExpandedDetailsView, 0);
        }
        mExpandedDetailsView.setVisibility(VISIBLE);
    }

    private boolean ensureExpandedDetailsView() {
        boolean viewCreated = false;
        if (mExpandedDetailsView == null) {
            View v = inflateExpandedDetails(mInflater, mExpandableHeaderContent);

            mExpandedDetailsView = (ViewGroup) v;

            renderExpandedDetails(getResources(), mExpandedDetailsView, mAddressCache,
                    getAccount(), mVeiledMatcher, mFrom, mReplyTo, mTo, mCc, mBcc);
            viewCreated = true;
        }
        return viewCreated;
    }

    public static View inflateExpandedDetails(LayoutInflater inflater, ViewGroup rootView) {
            return inflater.inflate(R.layout.conversation_message_details_header_expanded, rootView,
                    false);
    }

    public static void renderExpandedDetails(Resources res, View detailsView,
            Map<String, Address> addressCache, Account account,
            VeiledAddressMatcher veiledMatcher, String[] from, String[] replyTo,
            String[] to, String[] cc, String[] bcc) {
        renderTagView(detailsView, R.id.viewer_to_tagview, to);
        if (cc.length > 0) {
            renderTagView(detailsView, R.id.viewer_cc_tagview, cc);
            final View ccView = detailsView.findViewById(R.id.viewer_cc_tagview);
            ccView.setVisibility(VISIBLE);
        }
        if (bcc.length > 0) {
            renderTagView(detailsView, R.id.viewer_bcc_tagview, bcc);
            final View bccView = detailsView.findViewById(R.id.viewer_bcc_tagview);
            bccView.setVisibility(VISIBLE);
        }
        final String address = account.getEmailAddress();
        final TextView accountName = (TextView)detailsView.findViewById(R.id.account_label);
        accountName.setText(res.getString(R.string.account_label, address));
    }

    private static void renderTagView(View rootView, int tagViewId, String[] addresses) {
        EmailTags tagView = (EmailTags)rootView.findViewById(tagViewId);
        if (tagView != null) {
            tagView.addAllEmailAddresses(Arrays.asList(addresses));
        }
    }

    /**
     * Returns a short plaintext snippet generated from the given HTML message
     * body. Collapses whitespace, ignores '&lt;' and '&gt;' characters and
     * everything in between, and truncates the snippet to no more than 100
     * characters.
     *
     * @return Short plaintext snippet
     */
    @VisibleForTesting
    static String makeSnippet(final String messageBody) {
        if (TextUtils.isEmpty(messageBody)) {
            return null;
        }

        final StringBuilder snippet = new StringBuilder(MAX_SNIPPET_LENGTH);

        final StringReader reader = new StringReader(messageBody);
        try {
            int c;
            while ((c = reader.read()) != -1 && snippet.length() < MAX_SNIPPET_LENGTH) {
                // Collapse whitespace.
                if (Character.isWhitespace(c)) {
                    snippet.append(' ');
                    do {
                        c = reader.read();
                    } while (Character.isWhitespace(c));
                    if (c == -1) {
                        break;
                    }
                }

                if (c == '<') {
                    // Ignore everything up to and including the next '>'
                    // character.
                    while ((c = reader.read()) != -1) {
                        if (c == '>') {
                            break;
                        }
                    }

                    // If we reached the end of the message body, exit.
                    if (c == -1) {
                        break;
                    }
                } else if (c == '&') {
                    // Read HTML entity.
                    StringBuilder sb = new StringBuilder();

                    while ((c = reader.read()) != -1) {
                        if (c == ';') {
                            break;
                        }
                        sb.append((char) c);
                    }

                    String entity = sb.toString();
                    if ("nbsp".equals(entity)) {
                        snippet.append(' ');
                    } else if ("lt".equals(entity)) {
                        snippet.append('<');
                    } else if ("gt".equals(entity)) {
                        snippet.append('>');
                    } else if ("amp".equals(entity)) {
                        snippet.append('&');
                    } else if ("quot".equals(entity)) {
                        snippet.append('"');
                    } else if ("apos".equals(entity) || "#39".equals(entity)) {
                        snippet.append('\'');
                    } else {
                        // Unknown entity; just append the literal string.
                        snippet.append('&').append(entity);
                        if (c == ';') {
                            snippet.append(';');
                        }
                    }

                    // If we reached the end of the message body, exit.
                    if (c == -1) {
                        break;
                    }
                } else {
                    // The current character is a non-whitespace character that
                    // isn't inside some
                    // HTML tag and is not part of an HTML entity.
                    snippet.append((char) c);
                }
            }
        } catch (IOException e) {
            LogUtils.wtf(LOG_TAG, e, "Really? IOException while reading a freaking string?!? ");
        }

        return snippet.toString();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Timer perf = new Timer();
        perf.start(LAYOUT_TAG);
        super.onLayout(changed, l, t, r, b);
        perf.pause(LAYOUT_TAG);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Timer t = new Timer();
        if (Timer.ENABLE_TIMER && !mPreMeasuring) {
            t.count("header measure id=" + mMessage.mId);
            t.start(MEASURE_TAG);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!mPreMeasuring) {
            t.pause(MEASURE_TAG);
        }
    }
}
