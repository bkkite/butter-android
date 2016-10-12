package butter.droid.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChooserOptionDialogFragment extends DialogFragment {

    public static final String TITLE = "title";
    public static final String ITEM_LIST = "options";
    public static final String NEG_BUT = "neg_but";

    private Listener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final List<String> list_options = getArguments().getStringArrayList(ITEM_LIST);
        final CharSequence[] charSec_options = list_options.toArray(new CharSequence[list_options.size()]);

        return new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getString(TITLE))
                .setItems(charSec_options,  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onItemSelected(which);
                    }
                })
                .setNegativeButton(getArguments().getString(NEG_BUT), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null)
                            mListener.onSelectionNegative();
                        dialog.dismiss();
                    }
                })
                .create();
    }

    public static void show(FragmentManager fm, String title, ArrayList<String> options, String positiveButton, String negativeButton, Listener listener) {
        try {
            ChooserOptionDialogFragment dialogFragment = new ChooserOptionDialogFragment();
            Bundle args = new Bundle();
            args.putString(TITLE, title);
            args.putStringArrayList(ITEM_LIST, options);
            args.putString(NEG_BUT, negativeButton);
            dialogFragment.setListener(listener);
            dialogFragment.setArguments(args);
            dialogFragment.show(fm, "overlay_fragment");
        } catch (IllegalStateException e) {
            // Eat exception
        }
    }

    public static void show(Context context, FragmentManager fm, int titleRes, ArrayList<String> options, int posButtonRes, int negButtonRes, Listener listener) {

        show(fm, context.getString(titleRes), options, context.getString(posButtonRes), context.getString(negButtonRes), listener);
    }

    private void setListener(Listener listener) {
        mListener = listener;
    }

    public interface Listener {
        public void onSelectionNegative();
        public void onItemSelected(int item);
    }

}
