package com.blackberry.email.ui.compose.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.blackberry.email.ui.compose.controllers.ComposeActivity;
import com.blackberry.lib.emailprovider.R;

public class SendConfirmDialogFragment extends DialogFragment {
    // Public no-args constructor needed for fragment re-instantiation
    public SendConfirmDialogFragment() {}

    public static SendConfirmDialogFragment newInstance(final int messageId,
            final boolean save, final boolean showToast) {
        final SendConfirmDialogFragment frag = new SendConfirmDialogFragment();
        final Bundle args = new Bundle(3);
        args.putInt("messageId", messageId);
        args.putBoolean("save", save);
        args.putBoolean("showToast", showToast);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int messageId = getArguments().getInt("messageId");
        final boolean save = getArguments().getBoolean("save");
        final boolean showToast = getArguments().getBoolean("showToast");

        return new AlertDialog.Builder(getActivity())
        .setMessage(messageId)
        .setTitle(R.string.confirm_send_title)
        .setIconAttribute(android.R.attr.alertDialogIcon)
        .setPositiveButton(R.string.send,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
//                ((ComposeActivity)getActivity()).finishSendConfirmDialog(save,
//                        showToast);
            }
        })
        .create();
    }
}