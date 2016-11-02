package butter.droid.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import butter.droid.R;
import butterknife.Bind;
import butterknife.ButterKnife;

public class OptionDeleteMovieDialogFragment extends DialogFragment {

    private Listener mListener;

    @Bind(R.id.check_delete_files)
    CheckBox checkDeleteFiles;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final View view = View.inflate(getActivity(), R.layout.fragment_delete_movie, null);
        ButterKnife.bind(this, view);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null) {
                            mListener.onSelectionPositive(checkDeleteFiles.isChecked());
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null)
                            mListener.onSelectionNegative();
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    public static void show(FragmentManager fm, Listener listener) {
        try {
            OptionDeleteMovieDialogFragment dialogFragment = new OptionDeleteMovieDialogFragment();
            dialogFragment.setListener(listener);
            dialogFragment.show(fm, "overlay_fragment");
        } catch (IllegalStateException e) {
            // Eat exception
        }
    }

    private void setListener(Listener listener) {
        mListener = listener;
    }

    public interface Listener {
        public void onSelectionPositive(boolean delete_files);

        public void onSelectionNegative();
    }

}
