package com.blackberry.email.provider.contract;

/**
 * Valid states for the {@link AttachmentColumns#STATE} column.
 *
 */
public final class AttachmentState {
    /**
     * The full attachment is not present on device. When used as a command,
     * setting this state will tell the provider to cancel a download in
     * progress.
     * <p>
     * Valid next states: {@link #DOWNLOADING}, {@link #PAUSED}
     */
    public static final int NOT_SAVED = 0;
    /**
     * The most recent attachment download attempt failed. The current UI
     * design does not require providers to persist this state, but
     * providers must return this state at least once after a download
     * failure occurs. This state may not be used as a command.
     * <p>
     * Valid next states: {@link #DOWNLOADING}
     */
    public static final int FAILED = 1;
    /**
     * The attachment is currently being downloaded by the provider.
     * {@link AttachmentColumns#DOWNLOADED_SIZE} should reflect the current
     * download progress while in this state. When used as a command,
     * setting this state will tell the provider to initiate a download to
     * the accompanying destination in {@link AttachmentColumns#DESTINATION}
     * .
     * <p>
     * Valid next states: {@link #NOT_SAVED}, {@link #FAILED},
     * {@link #SAVED}
     */
    public static final int DOWNLOADING = 2;
    /**
     * The attachment was successfully downloaded to the destination in
     * {@link AttachmentColumns#DESTINATION}. If a provider later detects
     * that a download is missing, it should reset the state to
     * {@link #NOT_SAVED}. This state may not be used as a command on its
     * own. To move a file from cache to external, update
     * {@link AttachmentColumns#DESTINATION}.
     * <p>
     * Valid next states: {@link #NOT_SAVED}, {@link #PAUSED}
     */
    public static final int SAVED = 3;
    /**
     * This is only used as a command, not as a state. The attachment is
     * currently being redownloaded by the provider.
     * {@link AttachmentColumns#DOWNLOADED_SIZE} should reflect the current
     * download progress while in this state. When used as a command,
     * setting this state will tell the provider to initiate a download to
     * the accompanying destination in {@link AttachmentColumns#DESTINATION}
     * .
     */
    public static final int REDOWNLOADING = 4;
    /**
     * The attachment is either pending or paused in the download manager.
     * {@link AttachmentColumns#DOWNLOADED_SIZE} should reflect the current
     * download progress while in this state. This state may not be used as
     * a command on its own.
     * <p>
     * Valid next states: {@link #DOWNLOADING}, {@link #FAILED}
     */
    public static final int PAUSED = 5;

    private AttachmentState() {}
}