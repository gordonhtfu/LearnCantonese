package com.blackberry.email.ui.compose.controllers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.blackberry.lib.emailprovider.R;

public class RecipientErrorDialogFragment extends DialogFragment {
    // Public no-args constructor needed for fragment re-instantiation
    public RecipientErrorDialogFragment() {}

    public static RecipientErrorDialogFragment newInstance(final String message) {
        final RecipientErrorDialogFragment frag = new RecipientErrorDialogFragment();
        final Bundle args = new Bundle(1);
        args.putString("message", message);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String message = getArguments().getString("message");
        return new AlertDialog.Builder(getActivity()).setMessage(message).setTitle(
                R.string.recipient_error_dialog_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(
                        R.string.ok, new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                ((ComposeActivity)getActivity()).finishRecipientErrorDialog();
                            }
                        }).create();
    }
}
