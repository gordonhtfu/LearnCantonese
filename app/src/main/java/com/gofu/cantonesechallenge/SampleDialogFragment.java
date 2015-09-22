package com.gofu.cantonesechallenge;

/**
 * Created by gofu on 2014-09-14.
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class SampleDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener {

    /**
     * Class to provide a dialog to user to rename the file when they recorded a new audio
     * */

    private View form=null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Create the layout of the dialog
        form=getActivity().getLayoutInflater().inflate(R.layout.dialog, null);

        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());

        return(builder.setTitle(R.string.dlg_title).setView(form)
                .setPositiveButton(android.R.string.ok, this).create());
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        String template=getActivity().getString(R.string.toast);
        EditText name=(EditText)form.findViewById(R.id.title);
        String msg=
                String.format(template, name.getText().toString().toString());
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDismiss(DialogInterface unused) {
        super.onDismiss(unused);

        Log.d(getClass().getSimpleName(), "Goodbye!");
    }

    @Override
    public void onCancel(DialogInterface unused) {
        super.onCancel(unused);

        Toast.makeText(getActivity(), R.string.back, Toast.LENGTH_LONG).show();
    }
}