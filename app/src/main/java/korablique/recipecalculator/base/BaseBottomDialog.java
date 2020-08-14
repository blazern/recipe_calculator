package korablique.recipecalculator.base;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.prefs.PrefsOwner;
import korablique.recipecalculator.base.prefs.SharedPrefsManager;
import korablique.recipecalculator.dagger.InjectorHolder;

public class BaseBottomDialog extends DialogFragment {
    private final List<OnBackPressObserver> onBackPressObservers = new CopyOnWriteArrayList<>();

    @Inject
    SharedPrefsManager prefsManager;

    public interface OnBackPressObserver {
        /**
         * @return true if event is consumed. If the event is consumed, the dialog won't close on back press.
         */
        boolean onBackPressed();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InjectorHolder.getInjector().inject(this);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog1 = super.onCreateDialog(savedInstanceState);
        dialog1.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Drawable background = getContext().getResources().getDrawable(R.drawable.bottom_dialog_transparent_background);
        dialog1.setOnShowListener(dialog -> {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog1.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.BOTTOM;
            dialog1.getWindow().setAttributes(layoutParams);
            dialog1.getWindow().setBackgroundDrawable(background);
        });

        if (showKeyboardOnStart()) {
            dialog1.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog1.setOnKeyListener(this::onKey);
        return dialog1;
    }

    private boolean showKeyboardOnStart() {
        boolean calcKeyboardEnabled =
                prefsManager.getBool(PrefsOwner.NO_OWNER,
                        getString(R.string.preference_key_calc_keyboard_enabled),
                        true);
        return !calcKeyboardEnabled || !calcKeyboardExpectedOnStart();
    }

    protected boolean calcKeyboardExpectedOnStart() {
        return true;
    }

    private boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN
                || keyCode != KeyEvent.KEYCODE_BACK) {
            // Not interesting events
            return false;
        }

        // Notify observers in reversed order, so that the most recently added observers would
        // receive the event first.
        // What this is for - usually new UI elements which appear on top of the UI want to react to
        // back presses before any other UI elements. For example, if a Dialog is shown, user expects
        // that a back press would close it immediately, so the dialog needs to receive the back press
        // event first.
        ListIterator<OnBackPressObserver> reversedIterator =
                onBackPressObservers.listIterator(onBackPressObservers.size());
        while (reversedIterator.hasPrevious()) {
            OnBackPressObserver observer = reversedIterator.previous();
            boolean eventConsumed = observer.onBackPressed();
            if (eventConsumed) {
                // Event consumed by current observer - other observers must not receive it.
                return true;
            }
        }
        return false;
    }

    public void addOnBackPressObserver(OnBackPressObserver observer) {
        onBackPressObservers.add(observer);
    }

    public void removeOnBackPressObserver(OnBackPressObserver observer) {
        onBackPressObservers.remove(observer);
    }
}
