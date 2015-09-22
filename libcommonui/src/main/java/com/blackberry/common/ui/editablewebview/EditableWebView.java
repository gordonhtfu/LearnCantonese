package com.blackberry.common.ui.editablewebview;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.blackberry.common.ui.R;

/**
 * Provides a base implementation for a webview which is securely editable, and
 * provides other features like cursorcallbacks, maintaining a ROI, default
 * configurations suitable for editable web content etc.
 * 
 * @author rratan
 * 
 *         Usage: Most custom features like cursorcallbaks, ROI and others, work
 *         only as long as the secureScheme is maintained throughout the
 *         lifetime of an html document.
 * 
 *         If a security scheme is not provided, the view assumes one during
 *         initialization. Security scheme can be provided either within layout
 *         xmls or using {@link EditableWebView#setSecurityScheme(String)}
 * 
 *         <com.blackberry.common.ui.editablewebview.EditableWebView ...
 *         custom:securityScheme="customScheme" ... />
 * 
 *         The recommended approaches to loading secure editable content are
 *         {@link EditableWebView#loadSecureData(String)} OR
 *         {@link EditableWebView#loadSecureDataWithScheme(String, String)} OR
 *         {@link EditableWebView#loadDataWithBaseURL(String, String, String, String, String)}
 * 
 *         You can listen for events either by registering for callbacks
 *         {@link EditableWebView#registerForEventCallbacks(EventCallbacks)} OR
 *         by extending the event methods in this control.
 * 
 */
public class EditableWebView extends WebView {

    /**
     * Callbacks for events generated by EditableWebView.
     * Objects interested in listening to these events must register with
     * {@link EditableWebView#registerForEventCallbacks(EventCallbacks)}
     */
    public interface EventCallbacks {
        /**
         * Notification callback for event
         * {@link EditableWebView#onMicroFocusChanged()}
         */
        void onMicroFocusChanged();

        /**
         * Notification callback for event
         * {@link EditableWebView#onLoadStarted(String)}
         * 
         * @param url
         *            , identifies the location of document.
         */
        void onLoadStarted(String url);

        /**
         * Notification callback for event
         * {@link EditableWebView#onLoadSucceeded(String)}
         * 
         * @param url
         *            , identifies the location of document.
         */
        void onLoadSucceeded(String url);

        /**
         * This callback multiplexes the results of all scripts injected
         * successfully using {@link EditableWebView#evaluateJavascript(String)}
         * 
         * @param scriptID
         *            Unique ID assigned to the script when it was queued for
         *            execution. Clients can use this ID to find the results of
         *            the earlier script. from here
         * @param scriptResult
         *            Results of the script identified by scriptID, bundled into
         *            a JSON object.
         */
        void processJSResult(int scriptID, String scriptResult);

        /**
         * Callback notifying clients that the result from
         * {@link EditableWebView#fetchDocumentContents(boolean)} is available.
         * 
         * @param resultDocument
         *            String containing the contents of DOM when
         *            {@link EditableWebView#fetchDocumentContents(boolean)}
         *            request was issued.
         * @param isHtml
         *            identifies the type of the result document, html or
         *            plaintext.
         */
        void documentAvailable(String resultDocument, boolean isHtml);

        /**
         * Notification for the event that contents of the loaded document has
         * changed. This source of the change could either be programmatic or
         * user driven.
         */
        void setDirty(boolean dirty);
    }

    // Identifiers for predefined scripts used internally
    private enum InternalScriptID {
        ANONYMOUS_INTERNAL, FETCH_HTML_DOCUMENT, FETCH_PLAIN_TEXT_DOCUMENT;
    }

    private static final int SCRIPT_FAILURE = -1;
    static final String DEFAULT_SCHEME = "defaultcspscheme";
    private int mScriptUID = SCRIPT_FAILURE;
    private boolean mEditable;
    private boolean mLoadSucceeded;
    private Activity mHostActivity;
    private String mSecurityScheme = DEFAULT_SCHEME;

    private static final String LOG_TAG = EditableWebView.class.getSimpleName();

    private Vector<EventCallbacks> mRegisteredCallbackListeners = new Vector<EventCallbacks>();

    /**
     * {@inheritDoc}
     */
    public EditableWebView(Context context) {
        this(context, null, 0);
    }

    /**
     * {@inheritDoc}
     */
    public EditableWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * {@inheritDoc}
     */
    public EditableWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs,
            int defStyleAttr) {
        applyDefaultConfigurations();
        setupUIHost(context);
        applyCustomStyledAttributes(context, attrs, defStyleAttr);
    }

    private void applyCustomStyledAttributes(Context context,
            AttributeSet attrs, int defStyleAttr) {
        TypedArray styledAttributes = context.obtainStyledAttributes(attrs,
                R.styleable.EditableWebViewAttrs);
        setSecurityScheme(styledAttributes
                .getString(R.styleable.EditableWebViewAttrs_securityScheme));
        styledAttributes.recycle();
    }

    private void setupUIHost(Context context) {
        if (context instanceof Activity) {
            mHostActivity = (Activity) context;
        }
    }

    /**
     * Sets up the default behavior preferred for the editable content. These
     * can be overridden if client chooses to.
     */
    private void applyDefaultConfigurations() {
        // Any post-initialize auto zoom behavior on editable content will
        // interfere with the control's ROI.
        getSettings().setSupportZoom(false);
        getSettings().setSaveFormData(false);
        getSettings().setJavaScriptEnabled(false);
        getSettings().setNeedInitialFocus(true);
        getSettings().setBlockNetworkLoads(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setLoadsImagesAutomatically(false);
        getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        setWebChromeClient(new EditableDocumentWindow());
        setWebViewClient(new ResourceRequestFilter());

        setFocusable(true);
        setFocusableInTouchMode(true);
        // getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm);
    }

    /**
     * Registers the provided object with this control as a listener for events.
     * 
     * @param callbackListener
     *            listener object which will be notified of events.
     * @return true if the registration was successful.
     */
    public boolean registerForEventCallbacks(EventCallbacks callbackListener) {
        return mRegisteredCallbackListeners.add(callbackListener);
    }

    /**
     * Unregisters the provided object with this control as a listener for
     * events.
     * 
     * @param callbackListener
     *            listener object which was registered earlier using
     *            {@link EditableWebView#registerForEventCallbacks(EventCallbacks)}
     *            .
     * @return true if the listener was successfully removed.
     */
    public boolean unregisterForEventCallbacks(EventCallbacks callbackListener) {
        Vector<EventCallbacks> uniqueEntry = new Vector<EventCallbacks>();
        uniqueEntry.add(callbackListener);
        return mRegisteredCallbackListeners.removeAll(uniqueEntry);
    }

    /**
     * Uses
     * {@link EditableWebView#loadDataWithBaseURL(String, String, String, String, String)}
     * with the current security scheme, "text/html" as mimetype, UTF-8 as
     * encoding and no history url.
     * 
     * This loads the provided data in a way that guarantees that, the document
     * once loaded will be modified in a secure environment, where script
     * injections, cross site scripting cannot occur.
     * 
     * @param data
     *            Html data to be loaded in editable mode.
     */
    public void loadSecureData(String data) {
        loadDataWithBaseURL(getSecurityScheme() + ":/", data,
                "text/html; charset=utf-8", "UTF-8", null);
    }

    /**
     * Overloaded version of {@link EditableWebView#loadSecureData(String)},
     * facilitating the change for securityScheme to be used from this point on.
     * 
     * @param securityScheme
     *            new securityScheme
     * @param data
     *            Html data to be loaded in editable mode.
     */
    public void loadSecureDataWithScheme(String securityScheme, String data) {
        setSchemeIfValid(securityScheme);
        loadSecureData(data);
    }

    /**
     * Overloads WebView#loadDataWithBaseURL(String, String, String, String,
     * String) Additionally, the baseUrl provided will be used as the security
     * scheme from this point forward, for this control.
     */
    public void loadDataWithBaseURL(String baseUrl, String data,
            String mimeType, String encoding, String historyUrl) {
        // ensure that scripts can run only after document load is complete.
        getSettings().setJavaScriptEnabled(false);
        try {
            URI uri = URI.create(baseUrl);
            setSchemeIfValid(uri.getScheme());
        } catch (IllegalArgumentException e) {
            Log.d(LOG_TAG, "Failed to parse baseUrl" + e);
        }
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    /**
     * Overloaded evaluateJavaScript(String script, ValueCallback<String>
     * resultCallback) which also assigns a unique ID to the script which can be
     * used to track the result of the script at a later point.
     * 
     * @param script
     *            the JavaScript to execute.
     * @return generated unique ID for the injected script. A return value of
     *         {@link EditableWebView#SCRIPT_FAILURE} indicates that the control
     *         isn't in a state to execute scripts, and the result of the script
     *         are not guaranteed.
     */
    public int evaluateJavascript(String script) {
        ScriptValueCallback<String> scriptCallback = new ScriptValueCallback<String>();
        evaluateJavascript(script, scriptCallback);
        return mLoadSucceeded ? scriptCallback.getCallbackID() : SCRIPT_FAILURE;
    }

    /**
     * Returns the current contents as a string. Fetches the content of DOM
     * asynchronously, and UI is notified with
     * {@link EditableWebView#documentAvailable(String, boolean)}
     * 
     * Note: This request only fetches the contents of the <body> element only,
     * excluding any <head>, <script> and other such markups.
     * 
     * @param html
     *            Returned document will be stripped of CSS styles and html markup
     *            if this is false.
     */
    public void fetchDocumentContents(boolean html) {
        setEditable(false);
        InternalScriptID scriptID = html ? InternalScriptID.FETCH_HTML_DOCUMENT
                                         : InternalScriptID.FETCH_PLAIN_TEXT_DOCUMENT;
        String script = String.format(JavaScriptRepository.FETCH_DOCUMENT_CONTENTS, Boolean.toString(html));
        injectInternalScript(script, scriptID);
        setEditable(true);
    }

    /**
     * Configures the control to notify once it has been modified after this request.
     * Listeners will be notified by {@link EventCallbacks#setDirty(boolean)}.
     * The content change request will be automatically removed, after the first
     * change has been notified. In other words, each request generates exactly one
     * {@link EventCallbacks#setDirty(boolean)} notification.
     * Content change request can however be used any number of times.
     *  
     * Note: Typical usage is to track changes on the DOM once the document load is
     * complete.
     * 
     * @return true if the request was successful, false otherwise.
     */
    public boolean observeContentChanges() {
        injectInternalScript(JavaScriptRepository.OBSERVE_CONTENT_CHANGES , null);
        return mLoadSucceeded;
    }

    /**
     * Toggles editable behavior on this control.
     * 
     * @param editable If true, enables the entire document for editing. Drops
     * the editable behavior from the document otherwise. This is enabled by default.
     * Note: Elements nested within the <body> sub-tree, if editable, are not affected.
     */
    public void setEditable(boolean editable) {
        if (mEditable != editable) {
            mEditable = editable;
            injectInternalScript(String.format(
                    JavaScriptRepository.MARK_CONTENT_EDTIABLE, editable), null);
        }
    }

    /**
     * Uses the provided scheme for securely loading and interacting with the
     * document. Security scheme can also be set using the custom attribute from
     * xml. Once set, this will be used to apply CSP based policies and can only
     * be changed using either EditableWebView#loadSecureDataWithScheme() or
     * EditableWebView#loadDataWithBaseURL()
     * 
     * @param secureScheme
     *            custom scheme used for CSP based default security
     *            implementation.
     */
    public void setSecurityScheme(String secureScheme) {
        if (mSecurityScheme.equals(DEFAULT_SCHEME)) {
            setSchemeIfValid(secureScheme);
        }
    }

    /**
     * Getter for the current security scheme in use.
     * 
     * @return Current security scheme in use by the control.
     */
    public String getSecurityScheme() {
        return mSecurityScheme;
    }

    /**
     * Returns true if DOM is currently editable, false otherwise.
     * @return true if DOM is currently editable, false otherwise.
     */
    public boolean isEditable() {
        return mEditable;
    }

    /**
     * Provides the context of this control only if its an Activity. This is
     * similar to {@link View#getContext()} only, this guarantees it to be an
     * activity.
     * 
     * @return Activity context if present.
     */
    public Activity getHostActivity() {
        return mHostActivity;
    }

    protected void processJSResult(int callbackID, String value) {
        for (EventCallbacks callbackListener : mRegisteredCallbackListeners) {
            callbackListener.processJSResult(callbackID, value);
        }
    }

    /**
     * Notification for the event when mainframe of the document started
     * loading.
     * 
     * @param url
     *            identifies the current location of the document as seen by
     *            webkit.
     */
    protected void onLoadStarted(String url) {
        // ensure that scripts can run only after document load is complete.
        getSettings().setJavaScriptEnabled(false);
        mLoadSucceeded = false;
        for (EventCallbacks callbackListener : mRegisteredCallbackListeners) {
            callbackListener.onLoadStarted(url);
        }
    }

    /**
     * Notification for the event when mainframe of the document completed
     * loading.
     * 
     * @param url identifies the current location of the document as seen by
     *            webkit.
     */
    @SuppressLint("SetJavaScriptEnabled")
    protected void onLoadSucceeded(String url) {
        mLoadSucceeded = true;
        getSettings().setJavaScriptEnabled(true);
        injectInternalScript(JavaScriptRepository.SETUP_SCRIPT_ENVIRONMENT , null);
        injectInternalScript(JavaScriptRepository.getSyncSchemeWithDOMScript(getSecurityScheme()), null);
        injectInternalScript(JavaScriptRepository.SPECIFY_VIEWPORT_AND_SECURITY_CONFIG, null);
        injectInternalScript(String.format(JavaScriptRepository.MARK_CONTENT_EDTIABLE, true), null);
        injectInternalScript(JavaScriptRepository.getCursorCallbackScript(getSecurityScheme()), null);
        // TODO: verify editability from DOM before setting this flag.
        setEditable(true);
        for (EventCallbacks callbackListener : mRegisteredCallbackListeners) {
            callbackListener.onLoadSucceeded(url);
        }
    }

    /**
     * Callback notification for cursor position change. This is fired every
     * time the caret position moves during edits.
     */
    protected void onMicroFocusChanged() {
        injectInternalScript(JavaScriptRepository
                .getCursorCallbackScript(getSecurityScheme()), null);
        for (EventCallbacks callbackListener : mRegisteredCallbackListeners) {
            callbackListener.onMicroFocusChanged();
        }
    }

    protected void notifyDocumentDirty() {
        for (EventCallbacks callbackListener : mRegisteredCallbackListeners) {
            callbackListener.setDirty(true);
        }
    }

    protected void onProgressChanged(int progress) {
        // Do nothing for now.
    }

    private void setSchemeIfValid(String newScheme) {
        if (newScheme != null && !newScheme.isEmpty()) {
            mSecurityScheme = newScheme;
        } else {
            Log.e(LOG_TAG, "Ignored null or empty scheme");
        }
    }

    private void documentAvailable(String document, boolean isHtml) {
        for (EventCallbacks callbackListener : mRegisteredCallbackListeners) {
            callbackListener.documentAvailable(document, isHtml);
        }
    }

    private void injectInternalScript(String script, InternalScriptID internalScriptID) {
        ScriptValueCallback<String> scriptCallback = new ScriptValueCallback<String>();
        internalScriptID = (internalScriptID == null) ? InternalScriptID.ANONYMOUS_INTERNAL
                                                      : internalScriptID;
        scriptCallback.setScriptID(internalScriptID);
        evaluateJavascript(script, scriptCallback);
    }

    private void processInternalJSResult(InternalScriptID mInternalScriptID,
            int mCallbackID, String scriptResult) {
        if (mInternalScriptID == null) {
            processJSResult(mCallbackID, scriptResult);
            return; // no one should hear about notifications for script we use
                    // internally.
        }
        if (scriptResult != null) {
            if (mInternalScriptID.compareTo(InternalScriptID.FETCH_HTML_DOCUMENT) == 0
                    || mInternalScriptID.compareTo(InternalScriptID.FETCH_PLAIN_TEXT_DOCUMENT) == 0) {
                try {
                    JsonReader reader = new JsonReader(new StringReader(scriptResult));
                    reader.setLenient(true);
                    if ((reader.peek() != JsonToken.NULL)
                            && (reader.peek() == JsonToken.STRING)) {
                        documentAvailable(reader.nextString(), mInternalScriptID.compareTo(InternalScriptID.FETCH_HTML_DOCUMENT) == 0);
                    }
                    reader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, " Failed to read script results: ");
                    e.printStackTrace();
                }
            }
        } else {
            Log.e(LOG_TAG, "Script failed most likely.");
        }
    }

    private class ScriptValueCallback<T> implements ValueCallback<T> {

        private int mCallbackID = 0;
        private InternalScriptID mInternalScriptID;
        public ScriptValueCallback() {
            mCallbackID = ++mScriptUID;
        }

        @Override
        public void onReceiveValue(T value) {
            if (value instanceof String)
                processInternalJSResult(mInternalScriptID, mCallbackID, (String) value);
            else
                Log.e(LOG_TAG, "ScriptValueCallback - unexpected value in callback");
        }

        public void setScriptID(InternalScriptID internalScriptID) {
            mInternalScriptID = internalScriptID;
        }

        public int getCallbackID() {
            return mCallbackID;
        }

    }
}
