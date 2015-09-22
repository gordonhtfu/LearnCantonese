package com.blackberry.common.ui.editablewebview;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.EnumSet;


import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * A WebViewClient which blocks all outgoing network resource requests and
 * enables the JS-native bridge by only allowing connections which originate
 * from custom origin.
 *
 * Additionally, it also notifies the view of custom events originating from
 * the current DOM.
 *
 * @author rratan
 *
 */
public class ResourceRequestFilter extends WebViewClient {

    private EditableWebView mEditableContext;
    private static final String LOG_TAG = "ResourceRequestFilter";

    private enum CustomDOMEvents {
        MICROFOCUS_CHANGE,
        DOCUMENT_DIRTY
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldOverrideUrlLoading (WebView view, String url) {
        return true; // block all outgoing network requests.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (view instanceof EditableWebView) {
            mEditableContext = ((EditableWebView) view);
            mEditableContext.onLoadStarted(url);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageFinished(WebView view, String url) {
        if (view instanceof EditableWebView) {
            mEditableContext = ((EditableWebView) view);
            mEditableContext.onLoadSucceeded(url);
        }
    }

    /**
     * {@inheritDoc}
     * NOTE: This method is called on a thread other than the UI thread
     * so clients should exercise caution when accessing private data or the view system.
     */
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {

        if (view instanceof EditableWebView) {
            mEditableContext = ((EditableWebView) view);
            Uri uri;
            try {
                uri = Uri.parse(url);
            } catch (Exception e) {
                Log.d(LOG_TAG, "Rejecting request for url " + url + " due to " + e);
                return new WebResourceResponse(null, null, null);
            }
            return filterResourceRequest(uri);
        }
        return super.shouldInterceptRequest(view, url);
    }

    protected WebResourceResponse filterResourceRequest(Uri uri) {

        if (uri.getScheme().equals(mEditableContext.getSecurityScheme()) && uri.isHierarchical()) {

            final EnumSet<CustomDOMEvents> events = EnumSet.noneOf(CustomDOMEvents.class);
            // figure out custom DOM events from URI
            if (uri.getLastPathSegment().equals(JavaScriptRepository.URI_SECTION_EVENTS)) {
                if (uri.getQueryParameter(JavaScriptRepository.EVENT_MICROFOCUS_CHANGED) != null)
                    events.add(CustomDOMEvents.MICROFOCUS_CHANGE);
                if (uri.getQueryParameter(JavaScriptRepository.EVENT_DOCUMENT_DIRTY) != null)
                    events.add(CustomDOMEvents.DOCUMENT_DIRTY);
            }
            notifyEvents(events);

            return new WebResourceResponse("text/plain", "utf-8",new ByteArrayInputStream("200".getBytes()));
        }
        Log.d(LOG_TAG, "Rejecting request for uri " + uri.toString());
        return new WebResourceResponse(null, null, null); /**REJECT*/
    }

    // TODO: Manage Runnables from a pool instead of spawning new ones.
    protected void notifyEvents(EnumSet<CustomDOMEvents> events) {
        final ResourceRequestFilter.DOMEventNotifier mUiNotifier = new ResourceRequestFilter.DOMEventNotifier();
        mUiNotifier.setEvents(events);
        if (mEditableContext.getHostActivity() != null) {
            mEditableContext.getHostActivity().runOnUiThread(mUiNotifier);
        } else {
            // slower way to respond to filter requests.
            mEditableContext.post(mUiNotifier);
        }
    }

    private class DOMEventNotifier implements Runnable {

        private final EnumSet<CustomDOMEvents> mEventsToNotify = EnumSet.noneOf(CustomDOMEvents.class);

        @SuppressWarnings("unused")
        public void reset() {
            mEventsToNotify.removeAll(EnumSet.allOf(CustomDOMEvents.class));
        }

        public void setEvents(EnumSet<CustomDOMEvents> currentEvents) {
            mEventsToNotify.addAll(currentEvents);
        }

        @Override
        public void run() {
            if (mEventsToNotify.contains(CustomDOMEvents.MICROFOCUS_CHANGE))
                ResourceRequestFilter.this.mEditableContext.onMicroFocusChanged();
            if (mEventsToNotify.contains(CustomDOMEvents.DOCUMENT_DIRTY))
                ResourceRequestFilter.this.mEditableContext.notifyDocumentDirty();
        }
    }
}
