package korablique.recipecalculator.ui.card;

import android.content.Context;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseBottomDialog;
import korablique.recipecalculator.base.prefs.PrefsOwner;
import korablique.recipecalculator.base.prefs.SharedPrefsManager;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.NutritionValuesWrapper;
import korablique.recipecalculator.ui.calckeyboard.CalcEditText;
import korablique.recipecalculator.ui.calckeyboard.CalcKeyboardController;
import korablique.recipecalculator.ui.pluralprogressbar.AnimatedPluralProgressBar;
import korablique.recipecalculator.util.FloatUtils;

import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;

public class Card {
    public interface OnMainButtonClickListener {
        void onClick(
                WeightedFoodstuff foodstuff,
                Foodstuff originalFoodstuff,
                @Nullable Double originalWeight);
    }

    public interface OnMainButtonSimpleClickListener {
        void onClick(WeightedFoodstuff foodstuff);
        default OnMainButtonClickListener convert() {
            return (WeightedFoodstuff foodstuff,
                    Foodstuff originalFoodstuff,
                    @Nullable Double originalWeight) -> onClick(foodstuff);
        }
    }

    public interface OnEditButtonClickListener {
        void onClick(Foodstuff editingFoodstuff);
    }

    public interface OnCloseButtonClickListener {
        void onClick();
    }

    public interface OnDeleteButtonClickListener {
        void onClick(WeightedFoodstuff foodstuff);
    }

    public static final String EDITED_FOODSTUFF = "EDITED_FOODSTUFF";
    private final CalcKeyboardController calcKeyboardController;
    private final SharedPrefsManager prefsManager;
    private ViewGroup cardLayout;
    private Foodstuff receivedFoodstuff;
    @Nullable
    private Double receivedWeight;
    private CalcEditText weightEditText;
    private TextView nameTextView;
    private Button button1;
    private Button button2;
    private View editButton;
    private View closeButton;
    private View deleteButton;

    private AnimatedPluralProgressBar pluralProgressBar;
    private NutritionValuesWrapper nutritionValuesWrapper;

    private boolean disableButton1WhenWeight0 = true;
    private boolean disableButton2WhenWeight0 = true;

    public Card(
            BaseBottomDialog dialog, ViewGroup parent,
            CalcKeyboardController calcKeyboardController, SharedPrefsManager prefsManager) {
        this.calcKeyboardController = calcKeyboardController;
        this.prefsManager = prefsManager;
        Context context = dialog.getContext();
        cardLayout = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.card_layout, parent);
        button1 = cardLayout.findViewById(R.id.button1);
        button2 = cardLayout.findViewById(R.id.button2);
        editButton = cardLayout.findViewById(R.id.frame_layout_button_edit);
        closeButton = cardLayout.findViewById(R.id.button_close);
        deleteButton = cardLayout.findViewById(R.id.frame_layout_button_delete);
        weightEditText = cardLayout.findViewById(R.id.weight_edit_text);
        updateMainButtonsEnability();

        nameTextView = cardLayout.findViewById(R.id.foodstuff_name_text_view);
        nameTextView.setMovementMethod(new ScrollingMovementMethod());
        ConstraintLayout nutritionLayout = cardLayout.findViewById(R.id.nutrition_progress_with_values);
        pluralProgressBar = nutritionLayout.findViewById(R.id.nutrition_progress_bar);
        pluralProgressBar.setAnimationsEnabled(false);
        nutritionValuesWrapper = new NutritionValuesWrapper(nutritionLayout);

        editButton.setVisibility(View.GONE);
        closeButton.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);

        calcKeyboardController.useCalcKeyboardWith(weightEditText, dialog);
    }

    // TODO: test for the 'open recipe' button
    private void updateMainButtonsEnability() {
        Float currentVal = weightEditText.getCurrentCalculatedValue();
        if (currentVal == null
                || FloatUtils.areFloatsEquals(0, currentVal)) {
            if (disableButton1WhenWeight0) {
                button1.setEnabled(false);
            } else {
                button1.setEnabled(true);
            }
            if (disableButton2WhenWeight0) {
                button2.setEnabled(false);
            } else {
                button2.setEnabled(true);
            }
        } else {
            button1.setEnabled(true);
            button2.setEnabled(true);
        }
    }

    public void setFoodstuff(WeightedFoodstuff weightedFoodstuff) {
        setFoodstuffImpl(weightedFoodstuff.withoutWeight(), weightedFoodstuff.getWeight());
        nutritionValuesWrapper.setNutrition(Nutrition.of(weightedFoodstuff));
    }

    public void setFoodstuff(Foodstuff foodstuff) {
        setFoodstuffImpl(foodstuff, null);
        nutritionValuesWrapper.setNutrition(Nutrition.of100gramsOf(foodstuff));
    }

    private void setFoodstuffImpl(Foodstuff foodstuff, @Nullable Double weight) {
        receivedFoodstuff = foodstuff;
        receivedWeight = weight;
        nameTextView.setText(foodstuff.getName());

        setNutritionProgress(Nutrition.of100gramsOf(foodstuff));

        if (weight != null) {
            weightEditText.setText(toDecimalString(weight));
            weightEditText.setSelection(weightEditText.getText().length());
        }
        weightEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                // пользователь отредактировал массу - показываем значения БЖУ на новую массу
                super.afterTextChanged(s);
                updateMainButtonsEnability();
                Float value = weightEditText.getCurrentCalculatedValue();
                double newWeight = 0;
                if (value != null) {
                    newWeight = value;
                }
                WeightedFoodstuff foodstuffWithNewWeight = foodstuff.withWeight(newWeight);
                Nutrition newNutrition = Nutrition.of(foodstuffWithNewWeight);
                nutritionValuesWrapper.setNutrition(newNutrition);
            }
        });
    }

    private void setNutritionProgress(Nutrition nutrition) {
        if (prefsManager.getBool(
                PrefsOwner.NO_OWNER,
                cardLayout.getResources().getString(R.string.preference_key_card_nutrition_extended),
                false)) {
            double sum = nutrition.getProtein()
                    + nutrition.getFats()
                    + nutrition.getCarbs();
            double proteinPercent;
            double fatsPercent;
            double carbsPercent;
            if (FloatUtils.areFloatsEquals(sum, 0)) {
                proteinPercent = 0;
                fatsPercent = 0;
                carbsPercent = 0;
            } else {
                proteinPercent = nutrition.getProtein() / sum;
                fatsPercent = nutrition.getFats() / sum;
                carbsPercent = nutrition.getCarbs() / sum;
            }
            pluralProgressBar.setProgress(
                    (float) proteinPercent * 100,
                    (float) fatsPercent * 100,
                    (float) carbsPercent * 100);
        } else {
            pluralProgressBar.setProgress(
                    (float) nutrition.getProtein(),
                    (float) nutrition.getFats(),
                    (float) nutrition.getCarbs());
        }
    }

    ViewGroup getCardLayout() {
        return cardLayout;
    }

    public void setUpButton1(OnMainButtonClickListener listener, @StringRes int buttonTextRes) {
        setUpMainButton(button1, listener, buttonTextRes);
    }

    public void setUpButton2(OnMainButtonClickListener listener, @StringRes int buttonTextRes) {
        setUpMainButton(button2, listener, buttonTextRes);
    }

    public void setUpButton1(OnMainButtonSimpleClickListener listener, @StringRes int buttonTextRes) {
        setUpMainButton(
                button1,
                (foodstuff, unused1, unused2) -> listener.onClick(foodstuff),
                buttonTextRes);
    }

    public void setUpButton2(OnMainButtonSimpleClickListener listener, @StringRes int buttonTextRes) {
        setUpMainButton(
                button2,
                (foodstuff, unused1, unused2) -> listener.onClick(foodstuff),
                buttonTextRes);
    }

    private void setUpMainButton(Button button, OnMainButtonClickListener listener, @StringRes int buttonTextRes) {
        button.setText(buttonTextRes);
        button.setOnClickListener(v -> {
            WeightedFoodstuff clickedFoodstuff = extractWeightedFoodstuff();
            listener.onClick(clickedFoodstuff, receivedFoodstuff, receivedWeight);
        });
        button.setVisibility(View.VISIBLE);
    }

    public void deinitButton1() {
        deinitMainButton(button1);
    }

    public void deinitButton2() {
        deinitMainButton(button2);
    }

    private void deinitMainButton(Button button) {
        button.setOnClickListener(null);
        button.setVisibility(View.GONE);
    }

    private WeightedFoodstuff extractWeightedFoodstuff() {
        Float weight = weightEditText.getCurrentCalculatedValue();
        if (weight == null) {
            weight = 0f;
        }
        return Foodstuff
                .withId(receivedFoodstuff.getId())
                .withName(nameTextView.getText().toString())
                .withNutrition(receivedFoodstuff.getProtein(),
                        receivedFoodstuff.getFats(),
                        receivedFoodstuff.getCarbs(),
                        receivedFoodstuff.getCalories())
                .withWeight(weight);
    }

    public Foodstuff extractFoodstuff() {
        return extractWeightedFoodstuff().withoutWeight();
    }

    void setOnEditButtonClickListener(@Nullable OnEditButtonClickListener listener) {
        if (listener != null) {
            editButton.setVisibility(View.VISIBLE);
        } else {
            editButton.setVisibility(View.GONE);
        }
        editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(receivedFoodstuff);
            }
        });
    }

    void setOnCloseButtonClickListener(OnCloseButtonClickListener listener) {
        if (listener != null) {
            closeButton.setVisibility(View.VISIBLE);
        } else {
            closeButton.setVisibility(View.GONE);
        }
        closeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick();
            }
        });
    }

    void setOnDeleteButtonClickListener(OnDeleteButtonClickListener listener) {
        if (listener != null) {
            deleteButton.setVisibility(View.VISIBLE);
        } else {
            deleteButton.setVisibility(View.GONE);
        }
        deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                WeightedFoodstuff clickedFoodstuff = extractWeightedFoodstuff();
                listener.onClick(clickedFoodstuff);
            }
        });
    }

    void focusOnEditing() {
        weightEditText.requestFocus();
    }

    void setDisableButton1WhenWeight0(boolean value) {
        disableButton1WhenWeight0 = value;
        updateMainButtonsEnability();
    }

    void setDisableButton2WhenWeight0(boolean value) {
        disableButton2WhenWeight0 = value;
        updateMainButtonsEnability();
    }
}
