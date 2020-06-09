package korablique.recipecalculator.ui.mainactivity.history.pages;

import android.content.res.ColorStateList;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.Nutrition;

public class NutritionProgressWrapper {
    private ProgressBar proteinProgress;
    private ProgressBar fatsProgress;
    private ProgressBar carbsProgress;
    private ProgressBar caloriesProgress;

    public NutritionProgressWrapper(ViewGroup layout) {
        proteinProgress = layout.findViewById(R.id.protein_layout).findViewById(R.id.nutrition_progress);
        proteinProgress.setProgressTintList(ColorStateList.valueOf(layout.getResources().getColor(R.color.colorProtein)));

        fatsProgress = layout.findViewById(R.id.fats_layout).findViewById(R.id.nutrition_progress);
        fatsProgress.setProgressTintList(ColorStateList.valueOf(layout.getResources().getColor(R.color.colorFats)));

        carbsProgress = layout.findViewById(R.id.carbs_layout).findViewById(R.id.nutrition_progress);
        carbsProgress.setProgressTintList(ColorStateList.valueOf(layout.getResources().getColor(R.color.colorCarbs)));

        caloriesProgress = layout.findViewById(R.id.calories_layout).findViewById(R.id.nutrition_progress);
        caloriesProgress.setProgressTintList(ColorStateList.valueOf(layout.getResources().getColor(R.color.colorCalories)));
    }

    public void setProgresses(Nutrition nutrition, Nutrition rates) {
        setProgress(proteinProgress, Math.round((float) nutrition.getProtein()), Math.round((float) rates.getProtein()));
        setProgress(fatsProgress, Math.round((float) nutrition.getFats()), Math.round((float) rates.getFats()));
        setProgress(carbsProgress, Math.round((float) nutrition.getCarbs()), Math.round((float) rates.getCarbs()));
        setProgress(caloriesProgress, Math.round((float) nutrition.getCalories()), Math.round((float) rates.getCalories()));
    }

    private void setProgress(ProgressBar progressBar, int progress, int max) {
        progressBar.setMax(max);
        progressBar.setProgress(progress);
    }
}
