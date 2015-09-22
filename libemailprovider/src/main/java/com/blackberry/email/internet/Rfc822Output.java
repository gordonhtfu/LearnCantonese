/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.blackberry.email.internet;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Base64OutputStream;

import com.blackberry.common.utils.LogUtils;
import com.blackberry.email.mail.Address;
import com.blackberry.email.mail.MessagingException;
import com.blackberry.email.utils.EmailMessageContactUtilities;
import com.blackberry.message.service.MessageAttachmentValue;
import com.blackberry.message.service.MessageBodyValue;
import com.blackberry.message.service.MessageValue;
import com.blackberry.provider.MessageContract.MessageBody;
import com.blackberry.provider.MessageContract.MessageContact;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to output RFC 822 messages from provider email messages
 */
public class Rfc822Output {
    private static final String TAG = "Email";

    // In MIME, en_US-like date format should be used. In other words "MMM"
    // should be encoded to
    // "Jan", not the other localized format like "Ene" (meaning January in
    // locale es).
    public static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    /** A less-than-perfect pattern to pull out <body> content */
    public static final Pattern BODY_PATTERN = Pattern.compile(
            "(?:<\\s*body[^>]*>)(.*)(?:<\\s*/\\s*body\\s*>)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /** Match group in {@code BODY_PATTERN} for the body HTML */
    public static final int BODY_PATTERN_GROUP = 1;
    /** Index of the plain text version of the message body */
    public final static int INDEX_BODY_TEXT = 0;
    /** Index of the HTML version of the message body */
    public final static int INDEX_BODY_HTML = 1;
    /** Single digit [0-9] to ensure uniqueness of the MIME boundary */
    /* package */static byte sBoundaryDigit;

    /**
     * Returns just the content between the <body></body> tags. This is not
     * perfect and breaks with malformed HTML or if there happens to be special
     * characters in the attributes of the <body> tag (e.g. a '>' in a java
     * script block).
     */
    public static String getHtmlBody(String html) {
        Matcher match = BODY_PATTERN.matcher(html);
        if (match.find()) {
            return match.group(BODY_PATTERN_GROUP); // Found body; return
        } else {
            return html; // Body not found; return the full HTML and hope for
                         // the best
        }
    }

    /**
     * Gets both the plain text and HTML versions of the message body.
     */
    public static String buildBodyText(MessageBodyValue body, boolean useSmartReply) {
        if (body == null) {
            return new String();
        }

        String messageBody = new String(body.getBytesAsString());
        //Quoted text will be stored in SyncData2
        final int pos = body.mSyncData2 != null ? Integer.parseInt(body.mSyncData2) : 0;
        if (useSmartReply && pos > 0) {
            if (messageBody != null) {
                if (pos < messageBody.length()) {
                    messageBody = messageBody.substring(0, pos);
                }
            }
        }
        return messageBody;
    }

    /**
     * Write the entire message to an output stream. This method provides
     * buffering, so it is not necessary to pass in a buffered output stream
     * here.
     * 
     * @param context system context for accessing the provider
     * @param message the message to write out
     * @param out the output stream to write the message to
     * @param useSmartReply whether or not quoted text is appended to a
     *            reply/forward
     * @param sendBcc Whether to add the bcc header
     * @param attachments list of attachments to send (or null if retrieved from
     *            the message itself)
     */
    public static void writeTo(Context context, MessageValue message, OutputStream out,
            boolean useSmartReply, boolean sendBcc, List<MessageAttachmentValue> attachments)
            throws IOException, MessagingException {
        if (message == null) {
            // throw something?
            return;
        }

        OutputStream stream = new BufferedOutputStream(out, 1024);
        Writer writer = new OutputStreamWriter(stream);

        // Write the fixed headers. Ordering is arbitrary (the legacy code
        // iterated through a
        // hashmap here).

        String date = DATE_FORMAT.format(new Date(message.mTimeStamp));
        writeHeader(writer, "Date", date);

        writeEncodedHeader(writer, "Subject", message.mSubject);

        // @TODO Find place for this IMAP NEEDS IT WILL SAY it lives in
        // SYNC_DATA5 FOR NOW, MAY NEED COLUMN
        writeHeader(writer, "Message-ID", message.mSyncData5);
        writeAddressHeader(writer, "From",
                getContactsByFieldType(message, MessageContact.FieldType.FROM));
        writeAddressHeader(writer, "To",
                getContactsByFieldType(message, MessageContact.FieldType.TO));
        writeAddressHeader(writer, "Cc",
                getContactsByFieldType(message, MessageContact.FieldType.CC));
        // Address fields. Note that we skip bcc unless the sendBcc argument is
        // true
        // SMTP should NOT send bcc headers, but EAS must send it!
        if (sendBcc) {
            writeAddressHeader(writer, "Bcc",
                    getContactsByFieldType(message, MessageContact.FieldType.BCC));
        }
        writeAddressHeader(writer, "Reply-To",
                getContactsByFieldType(message, MessageContact.FieldType.REPLY_TO));
        writeHeader(writer, "MIME-Version", "1.0");

        // Analyze message and determine if we have multiparts
        MessageBodyValue body = MessageBodyValue.restoreBodyWithMessageId(context, message.mId);
        String bodyText = buildBodyText(body, useSmartReply);

        // If a list of attachments hasn't been passed in, build one from the
        // message
        if (attachments == null) {
            attachments =
                    Arrays.asList(MessageAttachmentValue.restoreAttachmentsWithMessageId(context,
                            message.mId));
        }

        boolean multipart = attachments.size() > 0;
        String multipartBoundary = null;
        String multipartType = "mixed";

        // Simplified case for no multipart - just emit text and be done.
        if (!multipart) {
            writeTextWithHeaders(writer, stream, bodyText, body.mType);
        } else {
            // continue with multipart headers, then into multipart body
            multipartBoundary = getNextBoundary();

            // Move to the first attachment; this must succeed because multipart
            // is true
            if (attachments.size() == 1) {
                // If we've got one attachment and it's an ics "attachment", we
                // want to send
                // this as multipart/alternative instead of multipart/mixed
                int flags = attachments.get(0).mFlags;
                if ((flags & MessageAttachmentValue.FLAG_ICS_ALTERNATIVE_PART) != 0) {
                    multipartType = "alternative";
                }
            }

            writeHeader(writer, "Content-Type",
                    "multipart/" + multipartType + "; boundary=\"" + multipartBoundary + "\"");
            // Finish headers and prepare for body section(s)
            writer.write("\r\n");

            // first multipart element is the body
            if (bodyText != null) {
                writeBoundary(writer, multipartBoundary, false);
                writeTextWithHeaders(writer, stream, bodyText, body.mType);
            }

            // Write out the attachments until we run out
            for (MessageAttachmentValue att : attachments) {
                writeBoundary(writer, multipartBoundary, false);
                writeOneAttachment(context, writer, stream, att);
                writer.write("\r\n");
            }

            // end of multipart section
            writeBoundary(writer, multipartBoundary, true);
        }

        writer.flush();
        out.flush();
    }

    private static String getContactsByFieldType(MessageValue message, int fieldType) {
        return EmailMessageContactUtilities.convertMessageContract(message.getContacts(fieldType));
    }

    /**
     * Write a single attachment and its payload
     */
    public static void writeOneAttachment(Context context, Writer writer, OutputStream out,
            MessageAttachmentValue attachment) throws IOException, MessagingException {
        writeHeader(writer, "Content-Type",
                attachment.mMimeType + ";\n name=\"" + attachment.mFileName + "\"");
        writeHeader(writer, "Content-Transfer-Encoding", "base64");
        // Most attachments (real files) will send Content-Disposition. The
        // suppression option
        // is used when sending calendar invites.
        if ((attachment.mFlags & MessageAttachmentValue.FLAG_ICS_ALTERNATIVE_PART) == 0) {
            writeHeader(writer, "Content-Disposition",
                    "attachment;"
                            + "\n filename=\"" + attachment.mFileName + "\";"
                            + "\n size=" + Long.toString(attachment.mSize));
        }
        if (attachment.mContentId != null) {
            writeHeader(writer, "Content-ID", attachment.mContentId);
        }
        writer.append("\r\n");

        // Set up input stream and write it out via base64
        InputStream inStream = null;
        try {
            // Use content, if provided; otherwise, use the contentUri
            if (attachment.mContentBytes != null) {
                inStream = new ByteArrayInputStream(attachment.mContentBytes);
            } else {
                // First try the cached file
                final String cachedFile = attachment.getCachedFileUri();
                if (!TextUtils.isEmpty(cachedFile)) {
                    final Uri cachedFileUri = Uri.parse(cachedFile);
                    try {
                        inStream = context.getContentResolver().openInputStream(cachedFileUri);
                    } catch (FileNotFoundException e) {
                        // Couldn't open the cached file, fall back to the
                        // original content uri
                        inStream = null;

                        LogUtils.d(TAG, "Rfc822Output#writeOneAttachment(), failed to load" +
                                "cached file, falling back to: %s", attachment.getContentUri());
                    }
                }

                if (inStream == null) {
                    // try to open the file
                    final Uri fileUri = Uri.parse(attachment.getContentUri());
                    inStream = context.getContentResolver().openInputStream(fileUri);
                }
            }
            // switch to output stream for base64 text output
            writer.flush();
            Base64OutputStream base64Out = new Base64OutputStream(
                    out, Base64.CRLF | Base64.NO_CLOSE);
            // copy base64 data and close up
            IOUtils.copy(inStream, base64Out);
            base64Out.close();

            // The old Base64OutputStream wrote an extra CRLF after
            // the output. It's not required by the base-64 spec; not
            // sure if it's required by RFC 822 or not.
            out.write('\r');
            out.write('\n');
            out.flush();
        } catch (FileNotFoundException fnfe) {
            // Ignore this - empty file is OK
            LogUtils.e(TAG, fnfe, "Rfc822Output#writeOneAttachment(), FileNotFoundException" +
                    "when sending attachment");
        } catch (IOException ioe) {
            LogUtils.e(TAG, ioe, "Rfc822Output#writeOneAttachment(), IOException" +
                    "when sending attachment");
            throw new MessagingException("Invalid attachment.", ioe);
        }
    }

    /**
     * Write a single header with no wrapping or encoding
     * 
     * @param writer the output writer
     * @param name the header name
     * @param value the header value
     */
    public static void writeHeader(Writer writer, String name, String value) throws IOException {
        if (value != null && value.length() > 0) {
            writer.append(name);
            writer.append(": ");
            writer.append(value);
            writer.append("\r\n");
        }
    }

    /**
     * Write a single header using appropriate folding & encoding
     * 
     * @param writer the output writer
     * @param name the header name
     * @param value the header value
     */
    public static void writeEncodedHeader(Writer writer, String name, String value)
            throws IOException {
        if (value != null && value.length() > 0) {
            writer.append(name);
            writer.append(": ");
            writer.append(MimeUtility.foldAndEncode2(value, name.length() + 2));
            writer.append("\r\n");
        }
    }

    /**
     * Unpack, encode, and fold address(es) into a header
     * 
     * @param writer the output writer
     * @param name the header name
     * @param value the header value (a packed list of addresses)
     */
    public static void writeAddressHeader(Writer writer, String name, String value)
            throws IOException {
        if (value != null && value.length() > 0) {
            writer.append(name);
            writer.append(": ");
            writer.append(MimeUtility.fold(Address.packedToHeader(value), name.length() + 2));
            writer.append("\r\n");
        }
    }

    /**
     * Write a multipart boundary
     * 
     * @param writer the output writer
     * @param boundary the boundary string
     * @param end false if inner boundary, true if final boundary
     */
    public static void writeBoundary(Writer writer, String boundary, boolean end)
            throws IOException {
        writer.append("--");
        writer.append(boundary);
        if (end) {
            writer.append("--");
        }
        writer.append("\r\n");
    }

    /**
     * Write the body text. Note this always uses base64, even when not
     * required. Slightly less efficient for US-ASCII text, but handles all
     * formats even when non-ascii chars are involved. A small optimization
     * might be to prescan the string for safety and send raw if possible.
     * 
     * @param writer the output writer
     * @param out the output stream inside the writer (used for byte[] access)
     * @param bodyText Plain text and HTML versions of the original text of the
     *            message
     */
    public static void writeTextWithHeaders(Writer writer, OutputStream out, String bodyText,
            int type)
            throws IOException {
        boolean html = type == MessageBody.Type.HTML ? true : false;
        String text = bodyText;

        if (text == null) {
            writer.write("\r\n"); // a truly empty message
        } else {
            // first multipart element is the body
            String mimeType = "text/" + (html ? "html" : "plain");
            writeHeader(writer, "Content-Type", mimeType + "; charset=utf-8");
            writeHeader(writer, "Content-Transfer-Encoding", "base64");
            writer.write("\r\n");
            byte[] textBytes = text.getBytes("UTF-8");
            writer.flush();
            out.write(Base64.encode(textBytes, Base64.CRLF));
        }
    }

    /**
     * Returns a unique boundary string.
     */
    public static String getNextBoundary() {
        StringBuilder boundary = new StringBuilder();
        boundary.append("--_com.android.email_").append(System.nanoTime());
        synchronized (Rfc822Output.class) {
            boundary = boundary.append(sBoundaryDigit);
            sBoundaryDigit = (byte) ((sBoundaryDigit + 1) % 10);
        }
        return boundary.toString();
    }
}
