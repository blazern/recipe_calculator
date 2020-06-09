package korablique.recipecalculator.ui.numbersediting;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

public class SimpleTextWatcher <T extends TextView> implements TextWatcher {
    private final OnTextChangedListener<T> onTextChangedListener;
    private final T textView;

    public interface OnTextChangedListener  <T extends TextView> {
        void onTextChanged(T textView);
    }

    public SimpleTextWatcher(T textView, OnTextChangedListener<T> listener) {
        this.onTextChangedListener = listener;
        this.textView = textView;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        onTextChangedListener.onTextChanged(textView);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void afterTextChanged(Editable s) {}
}
