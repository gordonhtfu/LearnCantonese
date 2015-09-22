/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blackberry.email.utils;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.blackberry.email.provider.UIProvider;
import com.blackberry.email.provider.contract.Account;
import com.blackberry.email.provider.contract.EmailContent.Attachment;
import com.blackberry.email.provider.contract.EmailContent.AttachmentColumns;
import com.blackberry.lib.emailprovider.R;
import com.blackberry.message.service.MessageAttachmentValue;
import com.blackberry.message.service.MessageBodyValue;
import com.blackberry.message.service.MessageValue;
import com.blackberry.message.utilities.MessagingProviderUtilities;
import com.blackberry.provider.MessageContract;
import com.blackberry.provider.MessageContract.MessageAttachment;
import com.blackberry.provider.MessageContract.MessageBody;
import com.blackberry.common.Logging;
import com.blackberry.common.utils.LogTag;
import com.blackberry.common.utils.LogUtils;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class AttachmentUtilities {

    public static final String FORMAT_RAW = "RAW";
    public static final String FORMAT_THUMBNAIL = "THUMBNAIL";

    public static class Columns {
        public static final String _ID = "_id";
        public static final String DATA = "_data";
        public static final String DISPLAY_NAME = "_display_name";
        public static final String SIZE = "_size";
    }

    private static final String[] ATTACHMENT_CACHED_FILE_PROJECTION = new String[] {
            AttachmentColumns.CACHED_FILE
    };

    /**
     * The MIME type(s) of attachments we're willing to send via attachments.
     *
     * Any attachments may be added via Intents with Intent.ACTION_SEND or ACTION_SEND_MULTIPLE.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_SEND_INTENT_TYPES = new String[] {
        "*/*",
    };
    /**
     * The MIME type(s) of attachments we're willing to send from the internal UI.
     *
     * NOTE:  At the moment it is not possible to open a chooser with a list of filter types, so
     * the chooser is only opened with the first item in the list.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_SEND_UI_TYPES = new String[] {
        "image/*",
        "video/*",
    };
    /**
     * The MIME type(s) of attachments we're willing to view.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_VIEW_TYPES = new String[] {
        "*/*",
    };
    /**
     * The MIME type(s) of attachments we're not willing to view.
     */
    public static final String[] UNACCEPTABLE_ATTACHMENT_VIEW_TYPES = new String[] {
    };
    /**
     * The MIME type(s) of attachments we're willing to download to SD.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES = new String[] {
        "*/*",
    };
    /**
     * The MIME type(s) of attachments we're not willing to download to SD.
     */
    public static final String[] UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES = new String[] {
    };
    /**
     * Filename extensions of attachments we're never willing to download (potential malware).
     * Entries in this list are compared to the end of the lower-cased filename, so they must
     * be lower case, and should not include a "."
     */
    public static final String[] UNACCEPTABLE_ATTACHMENT_EXTENSIONS = new String[] {
        // File types that contain malware
        "ade", "adp", "bat", "chm", "cmd", "com", "cpl", "dll", "exe",
        "hta", "ins", "isp", "jse", "lib", "mde", "msc", "msp",
        "mst", "pif", "scr", "sct", "shb", "sys", "vb", "vbe",
        "vbs", "vxd", "wsc", "wsf", "wsh",
        // File types of common compression/container formats (again, to avoid malware)
        "zip", "gz", "z", "tar", "tgz", "bz2",
    };
    /**
     * Filename extensions of attachments that can be installed.
     * Entries in this list are compared to the end of the lower-cased filename, so they must
     * be lower case, and should not include a "."
     */
    public static final String[] INSTALLABLE_ATTACHMENT_EXTENSIONS = new String[] {
        "apk",
    };
    /**
     * The maximum size of an attachment we're willing to download (either View or Save)
     * Attachments that are base64 encoded (most) will be about 1.375x their actual size
     * so we should probably factor that in. A 5MB attachment will generally be around
     * 6.8MB downloaded but only 5MB saved.
     */
    public static final int MAX_ATTACHMENT_DOWNLOAD_SIZE = (5 * 1024 * 1024);
    /**
     * The maximum size of an attachment we're willing to upload (measured as stored on disk).
     * Attachments that are base64 encoded (most) will be about 1.375x their actual size
     * so we should probably factor that in. A 5MB attachment will generally be around
     * 6.8MB uploaded.
     */
    public static final int MAX_ATTACHMENT_UPLOAD_SIZE = (5 * 1024 * 1024);

    private static Uri sUri;
    public static Uri getAttachmentUri(long accountId, long id) {
        if (sUri == null) {
            sUri = Uri.parse(Attachment.ATTACHMENT_PROVIDER_URI_PREFIX);
        }
        return sUri.buildUpon()
                .appendPath(Long.toString(accountId))
                .appendPath(Long.toString(id))
                .appendPath(FORMAT_RAW)
                .build();
    }

    /**
     * Return the filename for a given attachment.  This should be used by any code that is
     * going to *write* attachments.
     *
     * This does not create or write the file, or even the directories.  It simply builds
     * the filename that should be used.
     */
    public static File getAttachmentFilename(Context context, long accountId, long attachmentId) {
        return new File(getAttachmentDirectory(context, accountId), Long.toString(attachmentId));
    }

    /**
     * Return the directory for a given attachment.  This should be used by any code that is
     * going to *write* attachments.
     *
     * This does not create or write the directory.  It simply builds the pathname that should be
     * used.
     */
    public static File getAttachmentDirectory(Context context, long accountId) {
        return context.getDatabasePath(accountId + ".db_att");
    }

    /**
     * Helper to convert unknown or unmapped attachments to something useful based on filename
     * extensions. The mime type is inferred based upon the table below. It's not perfect, but
     * it helps.
     *
     * <pre>
     *                   |---------------------------------------------------------|
     *                   |                  E X T E N S I O N                      |
     *                   |---------------------------------------------------------|
     *                   | .eml        | known(.png) | unknown(.abc) | none        |
     * | M |-----------------------------------------------------------------------|
     * | I | none        | msg/rfc822  | image/png   | app/abc       | app/oct-str |
     * | M |-------------| (always     |             |               |             |
     * | E | app/oct-str |  overrides  |             |               |             |
     * | T |-------------|             |             |-----------------------------|
     * | Y | text/plain  |             |             | text/plain                  |
     * | P |-------------|             |-------------------------------------------|
     * | E | any/type    |             | any/type                                  |
     * |---|-----------------------------------------------------------------------|
     * </pre>
     *
     * NOTE: Since mime types on Android are case-*sensitive*, return values are always in
     * lower case.
     *
     * @param fileName The given filename
     * @param mimeType The given mime type
     * @return A likely mime type for the attachment
     */
    public static String inferMimeType(final String fileName, final String mimeType) {
        String resultType = null;
        String fileExtension = getFilenameExtension(fileName);
        boolean isTextPlain = "text/plain".equalsIgnoreCase(mimeType);

        if ("eml".equals(fileExtension)) {
            resultType = "message/rfc822";
        } else {
            boolean isGenericType =
                    isTextPlain || "application/octet-stream".equalsIgnoreCase(mimeType);
            // If the given mime type is non-empty and non-generic, return it
            if (isGenericType || TextUtils.isEmpty(mimeType)) {
                if (!TextUtils.isEmpty(fileExtension)) {
                    // Otherwise, try to find a mime type based upon the file extension
                    resultType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
                    if (TextUtils.isEmpty(resultType)) {
                        // Finally, if original mimetype is text/plain, use it; otherwise synthesize
                        resultType = isTextPlain ? mimeType : "application/" + fileExtension;
                    }
                }
            } else {
                resultType = mimeType;
            }
        }

        // No good guess could be made; use an appropriate generic type
        if (TextUtils.isEmpty(resultType)) {
            resultType = isTextPlain ? "text/plain" : "application/octet-stream";
        }
        return resultType.toLowerCase();
    }

    /**
     * Extract and return filename's extension, converted to lower case, and not including the "."
     *
     * @return extension, or null if not found (or null/empty filename)
     */
    public static String getFilenameExtension(String fileName) {
        String extension = null;
        if (!TextUtils.isEmpty(fileName)) {
            int lastDot = fileName.lastIndexOf('.');
            if ((lastDot > 0) && (lastDot < fileName.length() - 1)) {
                extension = fileName.substring(lastDot + 1).toLowerCase();
            }
        }
        return extension;
    }

    /**
     * Resolve attachment id to content URI.  Returns the resolved content URI (from the attachment
     * DB) or, if not found, simply returns the incoming value.
     *
     * @param attachmentUri
     * @return resolved content URI
     *
     * TODO:  Throws an SQLite exception on a missing DB file (e.g. unknown URI) instead of just
     * returning the incoming uri, as it should.
     */
    public static Uri resolveAttachmentIdToContentUri(ContentResolver resolver, Uri attachmentUri) {
        Cursor c = resolver.query(attachmentUri,
                new String[] { Columns.DATA },
                null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    final String strUri = c.getString(0);
                    if (strUri != null) {
                        return Uri.parse(strUri);
                    }
                }
            } finally {
                c.close();
            }
        }
        return attachmentUri;
    }

    /**
     * In support of deleting a message, find all attachments and delete associated attachment
     * files.
     * @param context
     * @param accountId the account for the message
     * @param messageId the message
     */
    public static void deleteAllAttachmentFiles(Context context, long accountId, long messageId) {
        Uri uri = ContentUris.withAppendedId(Attachment.MESSAGE_ID_URI, messageId);
        Cursor c = context.getContentResolver().query(uri, Attachment.ID_PROJECTION,
                null, null, null);
        try {
            while (c.moveToNext()) {
                long attachmentId = c.getLong(Attachment.ID_PROJECTION_COLUMN);
                File attachmentFile = getAttachmentFilename(context, accountId, attachmentId);
                // Note, delete() throws no exceptions for basic FS errors (e.g. file not found)
                // it just returns false, which we ignore, and proceed to the next file.
                // This entire loop is best-effort only.
                attachmentFile.delete();
            }
        } finally {
            c.close();
        }
    }

    /**
     * In support of deleting a message, find all attachments and delete associated cached
     * attachment files.
     * @param context
     * @param accountId the account for the message
     * @param messageId the message
     */
    public static void deleteAllCachedAttachmentFiles(Context context, long accountId,
            long messageId) {
        final Uri uri = ContentUris.withAppendedId(Attachment.MESSAGE_ID_URI, messageId);
        final Cursor c = context.getContentResolver().query(uri, ATTACHMENT_CACHED_FILE_PROJECTION,
                null, null, null);
        try {
            while (c.moveToNext()) {
                final String fileName = c.getString(0);
                if (!TextUtils.isEmpty(fileName)) {
                    final File cachedFile = new File(fileName);
                    // Note, delete() throws no exceptions for basic FS errors (e.g. file not found)
                    // it just returns false, which we ignore, and proceed to the next file.
                    // This entire loop is best-effort only.
                    cachedFile.delete();
                }
            }
        } finally {
            c.close();
        }
    }

    /**
     * In support of deleting a mailbox, find all messages and delete their attachments.
     *
     * @param context
     * @param accountId the account for the mailbox
     * @param mailboxId the mailbox for the messages
     */
    public static void deleteAllMailboxAttachmentFiles(Context context, long accountId,
            long mailboxId) {
        Cursor c = context.getContentResolver().query(MessageContract.Message.CONTENT_URI,
                new String[]{MessageContract.Message._ID}, MessageContract.Message.FOLDER_ID + "=?",
                new String[] { Long.toString(mailboxId) }, null);
        try {
            while (c.moveToNext()) {
                long messageId = c.getLong(0);
                deleteAllAttachmentFiles(context, accountId, messageId);
            }
        } finally {
            c.close();
        }
    }

    /**
     * In support of deleting or wiping an account, delete all related attachments.
     *
     * @param context
     * @param accountId the account to scrub
     */
    public static void deleteAllAccountAttachmentFiles(Context context, long accountId) {
        File[] files = getAttachmentDirectory(context, accountId).listFiles();
        if (files == null) return;
        for (File file : files) {
            boolean result = file.delete();
            if (!result) {
                LogUtils.e(Logging.LOG_TAG, "Failed to delete attachment file " + file.getName());
            }
        }
    }

    private static long copyFile(InputStream in, OutputStream out) throws IOException {
        long size = IOUtils.copy(in, out);
        in.close();
        out.flush();
        out.close();
        return size;
    }

    /**
     * Save the attachment to its final resting place (cache or sd card)
     */
    public static void saveAttachment(Context context, InputStream in, MessageAttachmentValue attachment) {
        Uri uri = ContentUris.withAppendedId(MessageAttachment.CONTENT_URI, attachment.mId);
        ContentValues cv = new ContentValues();
        long attachmentId = attachment.mId;
        long accountId = attachment.mAccountId;
        String contentUri = null;
        long size;
        try {
            ContentResolver resolver = context.getContentResolver();
            if (attachment.mUiDestination == UIProvider.AttachmentDestination.CACHE) {
                Uri attUri = getAttachmentUri(accountId, attachmentId);
                size = copyFile(in, resolver.openOutputStream(attUri));
                contentUri = attUri.toString();
            } else if (Utility.isExternalStorageMounted()) {
                if (attachment.mFileName == null) {
                    // TODO: This will prevent a crash but does not surface the underlying problem
                    // to the user correctly.
                    LogUtils.w(Logging.LOG_TAG, "Trying to save an attachment with no name: %d",
                            attachmentId);
                    throw new IOException("Can't save an attachment with no name");
                }
                File downloads = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                downloads.mkdirs();
                File file = Utility.createUniqueFile(downloads, attachment.mFileName);
                size = copyFile(in, new FileOutputStream(file));
                String absolutePath = file.getAbsolutePath();

                // Although the download manager can scan media files, scanning only happens
                // after the user clicks on the item in the Downloads app. So, we run the
                // attachment through the media scanner ourselves so it gets added to
                // gallery / music immediately.
                MediaScannerConnection.scanFile(context, new String[] {absolutePath},
                        null, null);

                DownloadManager dm =
                        (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                long id = dm.addCompletedDownload(attachment.mFileName, attachment.mFileName,
                        false /* do not use media scanner */,
                        attachment.mMimeType, absolutePath, size,
                        true /* show notification */);
                contentUri = dm.getUriForDownloadedFile(id).toString();

            } else {
                LogUtils.w(Logging.LOG_TAG,
                        "Trying to save an attachment without external storage?");
                throw new IOException();
            }

            // Update the attachment
            cv.put(MessageAttachment.SIZE, size);
            cv.put(MessageAttachment.URI, contentUri);
            cv.put(MessageAttachment.STATE, UIProvider.AttachmentState.SAVED);
        } catch (IOException e) {
            // Handle failures here...
            cv.put(MessageAttachment.STATE, UIProvider.AttachmentState.FAILED);
        }
        context.getContentResolver().update(uri, cv, null, null);

        // If this is an inline attachment, update the body
        if (contentUri != null && attachment.mContentId != null) {
            MessageBodyValue body = MessageBodyValue.restoreBodyWithMessageId(context, attachment.mMessageId);
            if (body != null && body.mType == MessageBody.Type.HTML) {
                cv.clear();
                String html = new String(body.mContentBytes);
                String contentIdRe =
                        "\\s+(?i)src=\"cid(?-i):\\Q" + attachment.mContentId + "\\E\"";
                String srcContentUri = " src=\"" + contentUri + "\"";
                html = html.replaceAll(contentIdRe, srcContentUri);
                cv.put(MessageBody.DATA, html);
                context.getContentResolver().update(
                        ContentUris.withAppendedId(MessageBody.CONTENT_URI, body.mId), cv, null, null);
            }
        }
    }

    /**
     * Save the attachment to its final resting place (cache or sd card)
     */
    public static void saveMessageAttachment(Context context, InputStream in, MessageAttachmentValue attachment) {
        Uri uri = ContentUris.withAppendedId(MessageAttachment.CONTENT_URI, attachment.mId);
        ContentValues cv = new ContentValues();
        long attachmentId = attachment.mId;
        long accountId = attachment.mAccountId;
        String contentUri = null;
        long size;
        try {
            ContentResolver resolver = context.getContentResolver();
            if (attachment.mUiDestination == UIProvider.AttachmentDestination.CACHE) {
                Uri attUri = getAttachmentUri(accountId, attachmentId);
                size = copyFile(in, resolver.openOutputStream(attUri));
                contentUri = attUri.toString();
            } else if (Utility.isExternalStorageMounted()) {
                if (attachment.mFileName == null) {
                    // TODO: This will prevent a crash but does not surface the underlying problem
                    // to the user correctly.
                    LogUtils.w(Logging.LOG_TAG, "Trying to save an attachment with no name: %d",
                            attachmentId);
                    throw new IOException("Can't save an attachment with no name");
                }
                File downloads = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                downloads.mkdirs();
                File file = Utility.createUniqueFile(downloads, attachment.mFileName);
                size = copyFile(in, new FileOutputStream(file));
                String absolutePath = file.getAbsolutePath();

                // Although the download manager can scan media files, scanning only happens
                // after the user clicks on the item in the Downloads app. So, we run the
                // attachment through the media scanner ourselves so it gets added to
                // gallery / music immediately.
                MediaScannerConnection.scanFile(context, new String[] {absolutePath},
                        null, null);

                DownloadManager dm =
                        (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                long id = dm.addCompletedDownload(attachment.mFileName, attachment.mFileName,
                        false /* do not use media scanner */,
                        attachment.mMimeType, absolutePath, size,
                        true /* show notification */);
                contentUri = dm.getUriForDownloadedFile(id).toString();

            } else {
                LogUtils.w(Logging.LOG_TAG,
                        "Trying to save an attachment without external storage?");
                throw new IOException();
            }

            // Update the attachment
            cv.put(MessageAttachment.SIZE, size);
            cv.put(MessageAttachment.URI, contentUri);
            cv.put(MessageAttachment.STATE, UIProvider.AttachmentState.SAVED);
        } catch (IOException e) {
            // Handle failures here...
            cv.put(MessageAttachment.STATE, UIProvider.AttachmentState.FAILED);
        }
        context.getContentResolver().update(uri, cv, null, null);

        // If this is an inline attachment, update the body
        if (contentUri != null && attachment.mContentId != null) {
            MessageBodyValue body = MessageBodyValue.restoreBodyWithMessageId(context, attachment.mMessageId);
            if (body != null && body.mContentBytes != null && body.mType == MessageBody.Type.HTML) {
                cv.clear();
                String html = new String(body.mContentBytes);
                String contentIdRe =
                        "\\s+(?i)src=\"cid(?-i):\\Q" + attachment.mContentId + "\\E\"";
                String srcContentUri = " src=\"" + contentUri + "\"";
                html = html.replaceAll(contentIdRe, srcContentUri);
                cv.put(MessageBody.DATA, html);
                context.getContentResolver().update(
                        ContentUris.withAppendedId(MessageBody.CONTENT_URI, body.mId), cv, null, null);
            }
        }
    }
    
    //Section was from old AttachmentUtils - just collapsed into single class
    private static final String LOG_TAG = LogTag.getLogTag();
    private static final int KILO = 1024;
    private static final int MEGA = KILO * KILO;
    /** Any IO reads should be limited to this timeout */
    private static final long READ_TIMEOUT = 3600 * 1000;
    private static final float MIN_CACHE_THRESHOLD = 0.25f;
    private static final int MIN_CACHE_AVAILABLE_SPACE_BYTES = 100 * 1024 * 1024;
    /**
     * Singleton map of MIME->friendly description
     * @see #getMimeTypeDisplayName(Context, String)
     */
    private static Map<String, String> sDisplayNameMap;

    /**
     * @return A string suitable for display in bytes, kilobytes or megabytes
     *         depending on its size.
     */
    public static String convertToHumanReadableSize(Context context, long size) {
        final String count;
        if (size == 0) {
            return "";
        } else if (size < KILO) {
            count = String.valueOf(size);
            return context.getString(R.string.bytes, count);
        } else if (size < MEGA) {
            count = String.valueOf(size / KILO);
            return context.getString(R.string.kilobytes, count);
        } else {
            DecimalFormat onePlace = new DecimalFormat("0.#");
            count = onePlace.format((float) size / (float) MEGA);
            return context.getString(R.string.megabytes, count);
        }
    }

    /**
     * Return a friendly localized file type for this attachment, or the empty string if
     * unknown.
     * @param context a Context to do resource lookup against
     * @return friendly file type or empty string
     */
    public static String getDisplayType(final Context context, final com.blackberry.email.Attachment attachment) {
        if ((attachment.flags & MessageAttachmentValue.FLAG_DUMMY_ATTACHMENT) != 0) {
            // This is a dummy attachment, display blank for type.
            return null;
        }

        // try to get a friendly name for the exact mime type
        // then try to show a friendly name for the mime family
        // finally, give up and just show the file extension
        final String contentType = attachment.getContentType();
        String displayType = getMimeTypeDisplayName(context, contentType);
        int index = !TextUtils.isEmpty(contentType) ? contentType.indexOf('/') : -1;
        if (displayType == null && index > 0) {
            displayType = getMimeTypeDisplayName(context, contentType.substring(0, index));
        }
        if (displayType == null) {
            String extension = Utility.getFileExtension(attachment.getName());
            // show '$EXTENSION File' for unknown file types
            if (extension != null && extension.length() > 1 && extension.indexOf('.') == 0) {
                displayType = context.getString(R.string.attachment_unknown,
                        extension.substring(1).toUpperCase());
            }
        }
        if (displayType == null) {
            // no extension to display, but the map doesn't accept null entries
            displayType = "";
        }
        return displayType;
    }

    /**
     * Returns a user-friendly localized description of either a complete a MIME type or a
     * MIME family.
     * @param context used to look up localized strings
     * @param type complete MIME type or just MIME family
     * @return localized description text, or null if not recognized
     */
    public static synchronized String getMimeTypeDisplayName(final Context context,
            String type) {
        if (sDisplayNameMap == null) {
            String docName = context.getString(R.string.attachment_application_msword);
            String presoName = context.getString(R.string.attachment_application_vnd_ms_powerpoint);
            String sheetName = context.getString(R.string.attachment_application_vnd_ms_excel);

            sDisplayNameMap = new ImmutableMap.Builder<String, String>()
                .put("image", context.getString(R.string.attachment_image))
                .put("audio", context.getString(R.string.attachment_audio))
                .put("video", context.getString(R.string.attachment_video))
                .put("text", context.getString(R.string.attachment_text))
                .put("application/pdf", context.getString(R.string.attachment_application_pdf))

                // Documents
                .put("application/msword", docName)
                .put("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        docName)

                // Presentations
                .put("application/vnd.ms-powerpoint",
                        presoName)
                .put("application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        presoName)

                // Spreadsheets
                .put("application/vnd.ms-excel", sheetName)
                .put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        sheetName)

                .build();
        }
        return sDisplayNameMap.get(type);
    }

    /**
     * Cache the file specified by the given attachment.  This will attempt to use any
     * {@link ParcelFileDescriptor} in the Bundle parameter
     * @param context
     * @param attachment  Attachment to be cached
     * @param attachmentFds optional {@link Bundle} containing {@link ParcelFileDescriptor} if the
     *        caller has opened the files
     * @return String file path for the cached attachment
     */
    // TODO(pwestbro): Once the attachment has a field for the cached path, this method should be
    // changed to update the attachment, and return a boolean indicating that the attachment has
    // been cached.
    public static String cacheAttachmentUri(Context context, com.blackberry.email.Attachment attachment,
            Bundle attachmentFds) {
        final File cacheDir = context.getCacheDir();

        final long totalSpace = cacheDir.getTotalSpace();
        if (attachment.size > 0) {
            final long usableSpace = cacheDir.getUsableSpace() - attachment.size;
            if (isLowSpace(totalSpace, usableSpace)) {
                LogUtils.w(LOG_TAG, "Low memory (%d/%d). Can't cache attachment %s",
                        usableSpace, totalSpace, attachment);
                return null;
            }
        }
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        File file = null;
        try {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-kk:mm:ss");
            file = File.createTempFile(dateFormat.format(new Date()), ".attachment", cacheDir);
            final ParcelFileDescriptor fileDescriptor = attachmentFds != null
                    && attachment.contentUri != null ? (ParcelFileDescriptor) attachmentFds
                    .getParcelable(attachment.contentUri.toString())
                    : null;
            if (fileDescriptor != null) {
                // Get the input stream from the file descriptor
                inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            } else {
                if (attachment.contentUri == null) {
                    // The contentUri of the attachment is null.  This can happen when sending a
                    // message that has been previously saved, and the attachments had been
                    // uploaded.
                    LogUtils.d(LOG_TAG, "contentUri is null in attachment: %s", attachment);
                    throw new FileNotFoundException("Missing contentUri in attachment");
                }
                // Attempt to open the file
                inputStream = context.getContentResolver().openInputStream(attachment.contentUri);
            }
            outputStream = new FileOutputStream(file);
            final long now = SystemClock.elapsedRealtime();
            final byte[] bytes = new byte[1024];
            while (true) {
                int len = inputStream.read(bytes);
                if (len <= 0) {
                    break;
                }
                outputStream.write(bytes, 0, len);
                if (SystemClock.elapsedRealtime() - now > READ_TIMEOUT) {
                    throw new IOException("Timed out reading attachment data");
                }
            }
            outputStream.flush();
            String cachedFileUri = file.getAbsolutePath();
            LogUtils.d(LOG_TAG, "Cached %s to %s", attachment.contentUri, cachedFileUri);

            final long usableSpace = cacheDir.getUsableSpace();
            if (isLowSpace(totalSpace, usableSpace)) {
                file.delete();
                LogUtils.w(LOG_TAG, "Low memory (%d/%d). Can't cache attachment %s",
                        usableSpace, totalSpace, attachment);
                cachedFileUri = null;
            }

            return cachedFileUri;
        } catch (IOException e) {
            // Catch any exception here to allow for unexpected failures during caching se we don't
            // leave app in inconsistent state as we call this method outside of a transaction for
            // performance reasons.
            LogUtils.e(LOG_TAG, e, "Failed to cache attachment %s", attachment);
            if (file != null) {
                file.delete();
            }
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                LogUtils.w(LOG_TAG, e, "Failed to close stream");
            }
        }
    }

    private static boolean isLowSpace(long totalSpace, long usableSpace) {
        // For caching attachments we want to enable caching if there is
        // more than 100MB available, or if 25% of total space is free on devices
        // where the cache partition is < 400MB.
        return usableSpace <
                Math.min(totalSpace * MIN_CACHE_THRESHOLD, MIN_CACHE_AVAILABLE_SPACE_BYTES);
    }

    /**
     * Checks if the attachment can be downloaded with the current network
     * connection.
     *
     * @param attachment the attachment to be checked
     * @return true if the attachment can be downloaded.
     */
    public static boolean canDownloadAttachment(Context context, com.blackberry.email.Attachment attachment) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info == null) {
            return false;
        } else if (info.isConnected()) {
            if (info.getType() != ConnectivityManager.TYPE_MOBILE) {
                // not mobile network
                return true;
            } else {
                // mobile network
                Long maxBytes = DownloadManager.getMaxBytesOverMobile(context);
                return maxBytes == null || attachment == null || attachment.size <= maxBytes;
            }
        } else {
            return false;
        }
    }
    
    public static boolean attachmentExists(Context context, MessageAttachmentValue attachment) {
        if (attachment == null) {
            return false;
        } else if (attachment.mContentBytes != null) {
            return true;
        } else {
            final String cachedFile = attachment.getCachedFileUri();
            // Try the cached file first
            if (!TextUtils.isEmpty(cachedFile)) {
                final Uri cachedFileUri = Uri.parse(cachedFile);
                try {
                    final InputStream inStream =
                            context.getContentResolver().openInputStream(cachedFileUri);
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        // Nothing to be done if can't close the stream
                    }
                    return true;
                } catch (FileNotFoundException e) {
                    // We weren't able to open the file, try the content uri below
                    LogUtils.e(Logging.LOG_TAG, e, "not able to open cached file");
                }
            }
            final String contentUri = attachment.getContentUri();
            if (TextUtils.isEmpty(contentUri)) {
                return false;
            }
            try {
                final Uri fileUri = Uri.parse(contentUri);
                try {
                    final InputStream inStream =
                            context.getContentResolver().openInputStream(fileUri);
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        // Nothing to be done if can't close the stream
                    }
                    return true;
                } catch (FileNotFoundException e) {
                    return false;
                }
            } catch (RuntimeException re) {
                LogUtils.w(Logging.LOG_TAG, "attachmentExists RuntimeException=" + re);
                return false;
            }
        }
    }

    /**
     * Check whether the message with a given id has unloaded attachments.  If the message is
     * a forwarded message, we look instead at the messages's source for the attachments.  If the
     * message or forward source can't be found, we return false
     * @param context the caller's context
     * @param messageId the id of the message
     * @return whether or not the message has unloaded attachments
     */
    public static boolean hasUnloadedAttachments(Context context, long messageId) {
        MessageValue msg = MessageValue.restoreMessageWithId(context, messageId);
        if (msg == null) return false;
        MessageAttachmentValue[] atts = MessageAttachmentValue.restoreAttachmentsWithMessageId(context, messageId);
        for (MessageAttachmentValue att: atts) {
            if (!attachmentExists(context, att)) {
                // If the attachment doesn't exist and isn't marked for download, we're in trouble
                // since the outbound message will be stuck indefinitely in the Outbox.  Instead,
                // we'll just delete the attachment and continue; this is far better than the
                // alternative.  In theory, this situation shouldn't be possible.
                if ((att.mFlags & (MessageAttachmentValue.FLAG_DOWNLOAD_FORWARD |
                        MessageAttachmentValue.FLAG_DOWNLOAD_USER_REQUEST)) == 0) {
                    LogUtils.d(Logging.LOG_TAG, "Unloaded attachment isn't marked for download: " +
                            att.mFileName + ", #" + att.mId);
                    Account acct = Account.restoreAccountWithId(context, msg.mAccountId);
                    if (acct == null) return true;
                    // If smart forward is set and the message is a forward, we'll act as though
                    // the attachment has been loaded
                    // In Email1 this test wasn't necessary, as the UI handled it...
                    if ((msg.mState & MessageContract.Message.State.TYPE_FORWARD) != 0) {
                        if ((acct.mFlags & Account.FLAGS_SUPPORTS_SMART_FORWARD) != 0) {
                            continue;
                        }
                    }
                    context.getContentResolver().delete(
                            MessagingProviderUtilities.buildUri(MessageAttachment.CONTENT_URI, att.mId, true),null,null) ;
                } else if (att.getContentUri() != null) {
                    // In this case, the attachment file is gone from the cache; let's clear the
                    // contentUri; this should be a very unusual case
                    ContentValues cv = new ContentValues();
                    //@TODO REVIST WHEN WE FINISH ATTACHMENTS MFL
                   // cv.putNull(MessageAttachment.CONTENT_URI);
                   /// Attachment.update(context, Attachment.CONTENT_URI, att.mId, cv);
                }
                return true;
            }
        }
        return false;
    }

}
