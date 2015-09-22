package com.blackberry.email.provider.contract;

/**
 * Values for the result of the last attempted sync of a Folder/Account
 */
public final class LastSyncResult {
    /** The sync completed successfully */
    public static final int SUCCESS = 0;
    /** The sync wasn't completed due to a connection error */
    public static final int CONNECTION_ERROR = 1;
    /** The sync wasn't completed due to an authentication error */
    public static final int AUTH_ERROR = 2;
    /** The sync wasn't completed due to a security error */
    public static final int SECURITY_ERROR = 3;
    /** The sync wasn't completed due to a low memory condition */
    public static final int STORAGE_ERROR = 4;
    /** The sync wasn't completed due to an internal error/exception */
    public static final int INTERNAL_ERROR = 5;
}
