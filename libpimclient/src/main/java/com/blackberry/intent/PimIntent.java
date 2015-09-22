package com.blackberry.intent;

public class PimIntent {

    /**
     * Broadcast Action: Some content providers have parts of their namespace
     * where they publish new events or items that clients (such as analytics)
     * may be especially interested in. For these things, they may broadcast
     * this action when the set of interesting items change.
     * <p>
     * For example, DomainMessageProvider sends this notification when a new
     * message is inserted.
     * <p>
     * The data URI of the intent identifies which part of which provider
     * changed. When queried through the content resolver, the data URI will
     * return the data set in question.
     */
    public static final String PIM_PROVIDER_CHANGED = "com.blackberry.intent.action.PIM_PROVIDER_CHANGED";

    /**
     * Broadcast Action when a change occurs in the AccountProvider
     * 
     */
    public static final String PIM_ACCOUNT_PROVIDER_CHANGED = "com.blackberry.intent.action.PIM_ACCOUNT_PROVIDER_CHANGED";

    /**
     * Fired by lists to delete an item. For example, Message Hub may fire this
     * intent in order to delete an email message.
     */
    public static final String PIM_ITEM_ACTION_DELETE = "com.blackberry.intent.action.PIM_ITEM_ACTION_DELETE";

    /**
     * Fired by lists to mark a message unread. For example, Message Hub may
     * fire this intent in order to mark unread an email message.
     */
    public static final String PIM_MESSAGE_ACTION_MARK_UNREAD = "com.blackberry.intent.action.PIM_MESSAGE_ACTION_MARK_UNREAD";

    /**
     * Fired by lists to mark a message read. For example, Message Hub may fire
     * this intent in order to mark read an email message.
     */
    public static final String PIM_MESSAGE_ACTION_MARK_READ = "com.blackberry.intent.action.PIM_MESSAGE_ACTION_MARK_READ";

    /**
     * Fired by lists to add priority to a message. For example, Message Hub may
     * fire this intent in order to add priority to an email message.
     */
    public static final String PIM_MESSAGE_ACTION_ADD_PRIORITY = "com.blackberry.intent.action.PIM_MESSAGE_ACTION_ADD_PRIORITY";

    /**
     * Fired by lists to remove priority from a message. For example, Message
     * Hub may fire this intent in order to remove priority from an email
     * message.
     */
    public static final String PIM_MESSAGE_ACTION_REMOVE_PRIORITY = "com.blackberry.intent.action.PIM_MESSAGE_ACTION_REMOVE_PRIORITY";

    /**
     * Fired by lists to flag a message. For example, Message Hub may fire this
     * intent in order to flag an email message.
     */
    public static final String PIM_MESSAGE_ACTION_FLAG = "com.blackberry.intent.action.PIM_MESSAGE_ACTION_FLAG";

    /**
     * Fired by lists to clear a flag on message. For example, Message Hub may
     * fire this intent in order to clear the flag from an email message.
     */
    public static final String PIM_MESSAGE_ACTION_CLEAR_FLAG = "com.blackberry.intent.action.PIM_MESSAGE_ACTION_CLEAR_FLAG";

    /**
     * Fired by lists to file a message. For example, Message Hub may fire this
     * intent in order to file an email message.
     */
    public static final String PIM_MESSAGE_ACTION_FILE = "com.blackberry.intent.action.PIM_MESSAGE_ACTION_FILE";

    /**
     * Fired by lists to reply to a message. For example, Message Hub may fire
     * this intent in order to start composing a reply to an email message.
     */
    public static final String PIM_MESSAGE_ACTION_REPLY = "com.blackberry.intent.action.PIM_MESSAGE_ACTION_REPLY";

    /**
     * Fired by lists to reply all to a message. For example, Message Hub may
     * fire this intent in order to start composing a reply all to an email
     * message.
     */
    public static final String PIM_MESSAGE_ACTION_REPLY_ALL = "com.blackberry.intent.action.PIM_MESSAGE_ACTION_REPLY_ALL";

    /**
     * Fired by lists to forward to a message. For example, Message Hub may fire
     * this intent in order to start composing forwarding an email message.
     */
    public static final String PIM_MESSAGE_ACTION_FORWARD = "com.blackberry.intent.action.PIM_MESSAGE_ACTION_FORWARD";

    /**
     * Fired by lists to place a call.  For example, Hub may fire this intent in order to
     * place a call on the selected item.
     */
    public static final String PIM_ACTION_PLACE_CALL = "com.blackberry.intent.action.PIM_ACTION_PLACE_CALL";

    private PimIntent() {
        // private constructor, not meant to be instantiated
    }
}
