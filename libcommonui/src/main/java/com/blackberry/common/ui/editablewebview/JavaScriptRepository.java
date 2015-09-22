package com.blackberry.common.ui.editablewebview;

import android.net.Uri;

/**
 * Repository class providing common javascripts which might be helpful in
 * editable context.
 * 
 * @author rratan
 * 
 */
/* package*/ final class JavaScriptRepository {

    private JavaScriptRepository() {
        // Utilities should not have their default ctors as public.
    }

    static final String URI_SECTION_EVENTS = "events";

    static final String EVENT_DOCUMENT_DIRTY = "setDirty";

    static final String EVENT_MICROFOCUS_CHANGED = "microFocusChanged";

    static final String SPECIFY_VIEWPORT_AND_SECURITY_CONFIG = " (function () { "
            + "    var documentHeadMissing = false;  "
            + "    var documentHead = document.head; "
            + "    if (!documentHead) { "
            + "        documentHeadMissing = true; "
            + "        documentHead = document.createElement('head'); "
            + "    } "
            + "    var cspConfig = document.createElement('meta'); "
            + "    cspConfig.httpEquiv = 'Content-Security-Policy'; "
            + "    cspConfig.content = \"script-src \'self\'; img-src *;\";"
            + "    var viewportConfig = document.createElement('meta'); "
            + "    viewportConfig.name = 'viewport'; "
            + "    viewportConfig.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0'; "
            + "    documentHead.insertBefore(viewportConfig,documentHead.firstChild); "
            + "    documentHead.insertBefore(cspConfig,documentHead.firstChild); "
            + "    if (documentHeadMissing) { "
            + "        document.documentElement.insertBefore(documentHead, document.documentElement.firstChild); "
            + "    } " + " })();";

    static String getCursorCallbackScript(String forScheme) {
        sUriBuilder.clearQuery();
        String callbackParameters = sUriBuilder.appendQueryParameter(
                EVENT_MICROFOCUS_CHANGED,
                Integer.toString((int) (Math.random() * 10000 + 1))).toString();
        return String.format(sCursorCallbackScript, callbackParameters);
    }

    static String getSyncSchemeWithDOMScript(String newScheme) {
        sUriBuilder.clearQuery();
        sUriBuilder.scheme(newScheme);
        return String.format("_postRequestURI = '%s';", sUriBuilder.toString());
    }

    static final String SETUP_SCRIPT_ENVIRONMENT = String.format(
              "var _postRequestURI = '%s';", EditableWebView.DEFAULT_SCHEME)

            + " DOM_OBSERVER_DICTIONARY =              "
            + "     {                                  "
            + "          subtree: true,                " // setup observer dictionary
            + "          childList: true,              "
            + "          attributes: true,             "
            + "          characterData: true,          "
            + "          attributeOldValue: true,      "
            + "          characterDataOldValue: true   "
            + "     };                                 "

            + " function postEventRequest(eventName , eventData) {                       "
            + "     var xmlHttpRequest = new XMLHttpRequest();                           "
            + "     var requestURL = _postRequestURI + '?' + eventName + '=' + eventData;"
            + "     xmlHttpRequest.open('GET', requestURL, true);                        "
            + "     xmlHttpRequest.send();                                               "
            + " }                                                                        "

            + " ( function() {                         "
            + "    _documentObserver = new WebKitMutationObserver(function(mutationRecordQueue) {"
            + "          if (mutationRecordQueue.length>0) {                                     "
            + "              postEventRequest( '" + EVENT_DOCUMENT_DIRTY + "', 'true' );         "
            + "              _documentObserver.disconnect();                                     "
            + "          }                                                                       "
            + "         _documentObserver.takeRecords();                                         "
            + "    });                                                                           "
            + " })();";

    static final String MARK_CONTENT_EDTIABLE =
              " (function(setEditable) {                                           "
            + "     setEditable ? document.body.contentEditable=\'true\'           "
            + "                 : document.body.removeAttribute('contentEditable');"
            + " })(%s);                                                            ";

    static final String OBSERVE_CONTENT_CHANGES =
              " _documentObserver.observe(document.documentElement, DOM_OBSERVER_DICTIONARY );";

    static String FETCH_DOCUMENT_CONTENTS =
              " (%s === true) ? document.documentElement.outerHTML  "
            + "               : document.documentElement.outerText; ";

    private static String sCursorCallbackScript =
              " document.addEventListener('selectionchange',function(eventObj) {"
            + " if (eventObj.eventPhase == Event.AT_TARGET)  {"
            + "     eventObj.preventDefault();"
            + "     document.removeEventListener('selectionchange',arguments.callee);"
            + "     var xmlHttpRequest = new XMLHttpRequest();"
            + "     var microFocusChangeRequest = '%s';"
            + "     xmlHttpRequest.open('GET',microFocusChangeRequest,true);"
            + "     xmlHttpRequest.send();"
            + " }"
            + " });";

    private static Uri.Builder sUriBuilder = new Uri.Builder().scheme(
            EditableWebView.DEFAULT_SCHEME).appendPath(URI_SECTION_EVENTS);
}
