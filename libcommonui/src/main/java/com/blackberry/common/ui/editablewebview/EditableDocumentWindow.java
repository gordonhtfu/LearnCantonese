package com.blackberry.common.ui.editablewebview;

import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * Used to configure the UI side (chrome) of the associated view.
 * @author rratan
 *
 */
public class EditableDocumentWindow extends WebChromeClient {

    public EditableDocumentWindow() {
        // TODO Auto-generated constructor stub
    }

    public void onRequestFocus(WebView view) {

    }

    public void onProgressChanged(WebView view, int progress) {
        if (view instanceof EditableWebView) {
            ((EditableWebView) view).onProgressChanged(progress);
        }
    }
}
