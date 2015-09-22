
package com.blackberry.provider;

public final class MessageFolderListItemValue {

    /**
     * Bit position for Sync_State
     */
    private static final int SYNCSTATE_BIT_POSITION = 0;

    /**
     * Number of bits for Sync_State
     */
    private static final int SYNCSTATE_BIT_NUMBERS = 8;

    /**
     * Bit position for capabilities
     */
    private static final int CAPABILITIES_BIT_POSITION = SYNCSTATE_BIT_POSITION
            + SYNCSTATE_BIT_NUMBERS;

    /**
     * Number of bits for capabilities
     */
    private static final int CAPABILITIES_BIT_NUMBERS = 16;

    /**
     * Bit position for Type
     */
    private static final int TYPE_BIT_POSITION = CAPABILITIES_BIT_POSITION
            + CAPABILITIES_BIT_NUMBERS;

    /**
     * Number of bits for Type
     */
    private static final int TYPE_BIT_NUMBERS = 8;

    /**
     * Set Type
     * 
     * @param parameter where the type should go to
     */
    public static long setType(long param, long type) {
        return (param | (type << TYPE_BIT_POSITION));
    }

    /**
     * Set Sync State
     * 
     * @param parameter where the syncState should go to
     */
    public static long setSyncState(long param, long syncState) {
        return (param | (syncState << SYNCSTATE_BIT_POSITION));
    }

    /**
     * Get Type
     * 
     * @param Get type given the parameter
     */
    public static long getType(long param) {
        return ((param >> TYPE_BIT_POSITION) & ((1 << TYPE_BIT_NUMBERS) - 1));
    }

    /**
     * Get SyncState
     * 
     * @param Get SyncState given the parameter
     */
    public static long getSyncState(long param) {
        return ((param >> SYNCSTATE_BIT_POSITION) & ((1 << SYNCSTATE_BIT_NUMBERS) - 1));
    }
    
    /**
     * Set Capabilities
     * 
     * @param parameter where the capabilities should go to
     */
    public static long setCapabilities(long param, long type) {
        return (param | (type << CAPABILITIES_BIT_POSITION));
    }
    
    /**
     * Get Capabilities
     * 
     * @param Get capabilities given the parameter
     */
    public static long getCapabilities(long param) {
        return ((param >> CAPABILITIES_BIT_POSITION) & ((1 << CAPABILITIES_BIT_NUMBERS) - 1));
    }
}
