/**
 * Copyright (c) 2013, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blackberry.email.activity;

import android.text.SpannableStringBuilder;
import android.view.Menu;
import android.view.MenuItem;

import com.blackberry.email.ui.compose.controllers.ComposeActivity;
import com.blackberry.lib.emailprovider.R;

public class ComposeActivityEmail extends ComposeActivity
        implements InsertQuickResponseDialog.Callback {
    static final String insertQuickResponseDialogTag = "insertQuickResponseDialog";
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.email_compose_menu_extras, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.insert_quick_response_menu_item) {
            InsertQuickResponseDialog dialog = InsertQuickResponseDialog.newInstance(null,
                    mReplyFromAccount.account);
            dialog.show(getFragmentManager(), insertQuickResponseDialogTag);
        }
        return super.onOptionsItemSelected(item);
    }

    public void onQuickResponseSelected(CharSequence quickResponse) {
//        final int selEnd = mBodyView.getSelectionEnd();
//        final int selStart = mBodyView.getSelectionStart();
//
//        if (selEnd >= 0 && selStart >= 0) {
//            final SpannableStringBuilder messageBody =
//                    new SpannableStringBuilder(mBodyView.getText());
//            final int replaceStart = selStart < selEnd ? selStart : selEnd;
//            final int replaceEnd = selStart < selEnd ? selEnd : selStart;
//            messageBody.replace(replaceStart, replaceEnd, quickResponse);
//            mBodyView.setText(messageBody);
//            mBodyView.setSelection(replaceStart + quickResponse.length());
//        } else {
//            mBodyView.append(quickResponse);
//            mBodyView.setSelection(mBodyView.getText().length());
//        }
    }
}
