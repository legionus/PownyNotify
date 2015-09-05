package com.yandex.pownynotify;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class DeleteDialogFragment extends DialogFragment {
    interface Callbacks {
        void onConfirmDelete();
        void onCancelDelete();
    }

    private Callbacks mCallbacks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.confirm_deletion_message)
                .setPositiveButton(R.string.confirm_deletion_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mCallbacks != null) {
                            mCallbacks.onConfirmDelete();
                        }
                    }
                })
                .setNegativeButton(R.string.confirm_deletion_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mCallbacks != null) {
                            mCallbacks.onCancelDelete();
                        }
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
