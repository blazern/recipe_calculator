package korablique.recipecalculator;

import android.app.Activity;
import android.graphics.Point;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class Card {
    private Activity activity;
    private Window rootWindow;
    private View rootView;
    private View cardLayout;
    private LinearLayout editedRow; //или какой тут тип будет?
    private EditText nameEditText;
    private EditText weightEditText;
    private EditText proteinEditText;
    private EditText fatsEditText;
    private EditText carbsEditText;
    private EditText caloriesEditText;
    private Button buttonOk;
    private Button buttonDelete;
    private Button buttonSave;
    private TextView weightTextview;

    public Card(Activity activity, ViewGroup parentLayout) {
        this.activity = activity;
        cardLayout = LayoutInflater.from(activity).inflate(R.layout.card_layout, null);
        parentLayout.addView(cardLayout);

        // NOTE: тут происходит какая-то чёрная магия
        // (Когда карточку добавили в parentView, карточка внезапно становится высотой match_parent,
        // а если parent'а карточки передать в inflate, то происходит какая-то непонятная фигня).
        ViewGroup.LayoutParams params = cardLayout.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        cardLayout.setLayoutParams(params);

        rootWindow = activity.getWindow();
        rootView = rootWindow.getDecorView().findViewById(android.R.id.content);

        nameEditText = (EditText) cardLayout.findViewById(R.id.name_edit_text);
        weightEditText = (EditText) cardLayout.findViewById(R.id.weight_edit_text);
        proteinEditText = (EditText) cardLayout.findViewById(R.id.protein_edit_text);
        fatsEditText = (EditText) cardLayout.findViewById(R.id.fats_edit_text);
        carbsEditText = (EditText) cardLayout.findViewById(R.id.carbs_edit_text);
        caloriesEditText = (EditText) cardLayout.findViewById(R.id.calories_edit_text);
        buttonOk = (Button) cardLayout.findViewById(R.id.button_ok);
        buttonDelete = (Button) cardLayout.findViewById(R.id.button_delete);
        buttonSave = (Button) cardLayout.findViewById(R.id.button_save);
        weightTextview = (TextView) cardLayout.findViewById(R.id.weight_text_view);
    }

    public void displayEmpty() {
        cardLayout.setVisibility(View.VISIBLE);
        cardLayout.bringToFront();
        cardLayout.setY(getVisibleParentHeight() - cardLayout.getHeight());
        editedRow = null;
    }

    public void displayForRow(LinearLayout row) {
        displayEmpty();
        nameEditText.setText(((TextView) row.findViewById(R.id.name)).getText().toString());
        weightEditText.setText(((TextView) row.findViewById(R.id.weight)).getText().toString());
        proteinEditText.setText(((TextView) row.findViewById(R.id.protein)).getText().toString());
        fatsEditText.setText(((TextView) row.findViewById(R.id.fats)).getText().toString());
        carbsEditText.setText(((TextView) row.findViewById(R.id.carbs)).getText().toString());
        caloriesEditText.setText(((TextView) row.findViewById(R.id.calories)).getText().toString());
        editedRow = row;
    }

    public void hide() {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int displayHeight = size.y;
        cardLayout.setY(cardLayout.getHeight() + displayHeight);
        cardLayout.setVisibility(View.INVISIBLE);
        editedRow = null;
    }

    public void addOnGlobalLayoutListener() {
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        cardLayout.setY(getVisibleParentHeight()
                                + ((View)cardLayout.getParent()).getY()
                                - cardLayout.getHeight());
                    }
                });
    }

    private int getVisibleParentHeight() {
        View cardParent = (View)cardLayout.getParent();
        return cardParent.getHeight();
    }

    public void clear() {
        nameEditText.setText("");
        weightEditText.setText("");
        proteinEditText.setText("");
        fatsEditText.setText("");
        carbsEditText.setText("");
        caloriesEditText.setText("");
    }

    public boolean areAllEditTextsFull() {
        if (weightEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (proteinEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (fatsEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (carbsEditText.getText().toString().isEmpty()) {
            return false;
        }
        if (caloriesEditText.getText().toString().isEmpty()) {
            return false;
        }
        return true;
    }

    public LinearLayout getEditedRow() {
        return editedRow;
    }

    public Button getButtonOk() {
        return buttonOk;
    }

    public Button getButtonDelete() {
        return buttonDelete;
    }

    public Button getButtonSave() {
        return buttonSave;
    }

    public EditText getNameEditText() {
        return nameEditText;
    }

    public EditText getProteinEditText() {
        return proteinEditText;
    }

    public EditText getFatsEditText() {
        return fatsEditText;
    }

    public EditText getCarbsEditText() {
        return carbsEditText;
    }

    public EditText getCaloriesEditText() {
        return caloriesEditText;
    }

    public EditText getWeightEditText() {
        return weightEditText;
    }

    public TextView getWeightTextView() {
        return weightTextview;
    }
}
