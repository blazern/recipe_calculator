package korablique.recipecalculator.ui.mainactivity.history.pages;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.ui.NutritionValuesWrapper;

public class HistoryNutritionValuesWrapper extends NutritionValuesWrapper {
    private ViewGroup layout;

    public HistoryNutritionValuesWrapper(ConstraintLayout layout) {
        super(layout);
        this.layout = layout;
    }

    public void setNutrition(Nutrition nutrition, Nutrition rates) {
        super.setNutrition(nutrition);
        Context context = layout.getContext();

        TextView proteinRateView = layout.findViewById(R.id.protein_layout).findViewById(R.id.of_n_grams);
        TextView fatsRateView = layout.findViewById(R.id.fats_layout).findViewById(R.id.of_n_grams);
        TextView carbsRateView = layout.findViewById(R.id.carbs_layout).findViewById(R.id.of_n_grams);
        TextView caloriesRateView = layout.findViewById(R.id.calories_layout).findViewById(R.id.of_n_grams);

        proteinRateView.setText(context.getString(R.string.of_n_grams, Math.round(rates.getProtein())));
        fatsRateView.setText(context.getString(R.string.of_n_grams, Math.round(rates.getFats())));
        carbsRateView.setText(context.getString(R.string.of_n_grams, Math.round(rates.getCarbs())));
        caloriesRateView.setText(context.getString(R.string.of_n_calories, Math.round(rates.getCalories())));
    }
}
