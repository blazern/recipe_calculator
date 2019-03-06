package korablique.recipecalculator.ui;


import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;

import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;

public class NutritionValuesWrapper {
    private ViewGroup layout;
    private Context context;
    private TextView proteinTextView;
    private TextView fatsTextView;
    private TextView carbsTextView;
    private TextView caloriesTextView;
    private Foodstuff foodstuff;

    public NutritionValuesWrapper(Context context, ViewGroup layout) {
        this.layout = layout;
        this.context = context;

        proteinTextView = layout.findViewById(R.id.protein_layout).findViewById(R.id.nutrition_text_view);
        fatsTextView = layout.findViewById(R.id.fats_layout).findViewById(R.id.nutrition_text_view);
        carbsTextView = layout.findViewById(R.id.carbs_layout).findViewById(R.id.nutrition_text_view);

        View caloriesView = layout.findViewById(R.id.calories_layout);
        if (caloriesView != null) {
            caloriesTextView = caloriesView.findViewById(R.id.nutrition_text_view);
        }

        setNutritionTable(R.id.protein_layout, R.string.protein, R.drawable.new_card_protein_icon);
        setNutritionTable(R.id.fats_layout, R.string.fats, R.drawable.new_card_fats_icon);
        setNutritionTable(R.id.carbs_layout, R.string.carbs, R.drawable.new_card_carbs_icon);
        if (caloriesView != null) {
            setNutritionTable(R.id.calories_layout, R.string.calories, R.drawable.invisible_drawable);
        }
    }

    // только задает цвета кружкам и названия в шапке
    private void setNutritionTable(@IdRes int nutritionLayout, @StringRes int nutritionName, @DrawableRes int drawable) {
        ViewGroup layout = this.layout.findViewById(nutritionLayout);
        TextView header = layout.findViewById(R.id.nutrition_name);
        header.setText(nutritionName);
        View coloredCircle = layout.findViewById(R.id.colored_circle);
        if (coloredCircle != null) {
            coloredCircle.setBackground(this.layout.getResources().getDrawable(drawable));
        }
    }

    public void setNutrition(Nutrition nutrition) {
        setNutritionValue(proteinTextView, nutrition.getProtein());
        setNutritionValue(fatsTextView, nutrition.getFats());
        setNutritionValue(carbsTextView, nutrition.getCarbs());
        if (caloriesTextView != null) {
            setNutritionValue(caloriesTextView, nutrition.getCalories());
        }
    }

    private void setNutritionValue(TextView nutritionTextView, double nutritionValue) {
        nutritionTextView.setText(toDecimalString(nutritionValue));
    }

    public double getProteinValue() {
        return Double.valueOf(proteinTextView.getText().toString());
    }

    public double getFatsValue() {
        return Double.valueOf(fatsTextView.getText().toString());
    }

    public double getCarbsValue() {
        return Double.valueOf(carbsTextView.getText().toString());
    }

    public double getCaloriesValue() {
        return Double.valueOf(caloriesTextView.getText().toString());
    }

    public Foodstuff getFoodstuff() {
        return foodstuff;
    }

    public void setFoodstuff(Foodstuff foodstuff) {
        this.foodstuff = foodstuff;
    }
}
