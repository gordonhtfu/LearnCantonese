
package com.blackberry.provider;

public final class MessageConversationListItemValue {

    /**
     * Bit position for Sync_State
     */
    private static final int STATE_BIT_POSITION = 0;

    /**
     * Number of bits for Sync_State
     */
    private static final int STATE_BIT_NUMBERS = 16;

    /**
     * Bit position for unread count
     */
    private static final int UNREADCOUNT_BIT_POSITION = STATE_BIT_POSITION + STATE_BIT_NUMBERS;

    /**
     * Number of bits for unread count
     */
    private static final int UNREADCOUNT_BIT_NUMBERS = 8;

    /**
     * Bit position for Type
     */
    private static final int TOTALCOUNT_BIT_POSITION = UNREADCOUNT_BIT_POSITION
            + UNREADCOUNT_BIT_NUMBERS;

    /**
     * Number of bits for Type
     */
    private static final int TOTALCOUNT_BIT_NUMBERS = 8;

    /**
     * Set Total count
     * 
     * @param parameter where the total count should go to
     */
    public static long setTotalCount(long param, long type) {
        return (param | (type << TOTALCOUNT_BIT_POSITION));
    }

    /**
     * Set State
     * 
     * @param parameter where the State should go to
     */
    public static long setState(long param, long state) {
        return (param | (state << STATE_BIT_POSITION));
    }

    /**
     * Get Total Count
     * 
     * @param Get total count given the parameter
     */
    public static long getTotalCount(long param) {
        return ((param >> TOTALCOUNT_BIT_POSITION) & ((1 << TOTALCOUNT_BIT_NUMBERS) - 1));
    }

    /**
     * Get State
     * 
     * @param Get State given the parameter
     */
    public static long getState(long param) {
        return ((param >> STATE_BIT_POSITION) & ((1 << STATE_BIT_NUMBERS) - 1));
    }

    /**
     * Set UnreadCount
     * 
     * @param parameter where the unread count should go to
     */
    public static long setUnreadCount(long param, long type) {
        return (param | (type << UNREADCOUNT_BIT_POSITION));
    }

    /**
     * Get UnreadCount
     * 
     * @param Get unread count given the parameter
     */
    public static long getUnreadCount(long param) {
        return ((param >> UNREADCOUNT_BIT_POSITION) & ((1 << UNREADCOUNT_BIT_NUMBERS) - 1));
    }
}
