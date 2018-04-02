package korablique.recipecalculator.ui.mainscreen;


import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.Foodstuff;

public class SelectedFoodstuffsSnackbar {
    private static long DURATION = 250L;
    private static final String IS_SNACKBAR_SHOWN = "IS_SNACKBAR_SHOWN";
    private static final String SELECTED_FOODSTUFFS = "SELECTED_FOODSTUFFS";
    private boolean isSnackbarShown;
    private ViewGroup snackbarLayout;
    private TextView selectedFoodstuffsCounter;
    private List<Foodstuff> selectedFoodstuffs = new ArrayList<>();

    public SelectedFoodstuffsSnackbar(Activity activity) {
        snackbarLayout = activity.findViewById(R.id.snackbar);
        selectedFoodstuffsCounter = activity.findViewById(R.id.selected_foodstuffs_counter);
    }

    public SelectedFoodstuffsSnackbar(Activity activity, Bundle savedInstanceState) {
        snackbarLayout = activity.findViewById(R.id.snackbar);
        selectedFoodstuffsCounter = activity.findViewById(R.id.selected_foodstuffs_counter);

        onRestoreInstanceState(savedInstanceState);
    }

    public void show() {
        snackbarLayout.setVisibility(View.VISIBLE);
        snackbarLayout.bringToFront();
        float startValue = snackbarLayout.getHeight();
        float endValue = 0;
        if (!isSnackbarShown) {
            animateSnackbar(startValue, endValue);
        }
        isSnackbarShown = true;
    }

    public void hide() {
        float startValue = snackbarLayout.getTranslationY();
        float endValue = getParentHeight();
        animateSnackbar(startValue, endValue);
    }

    private void animateSnackbar(float startValue, float endValue) {
        ValueAnimator animator = ValueAnimator.ofFloat(startValue, endValue);
        animator.setDuration(DURATION);
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            snackbarLayout.setTranslationY(animatedValue);
        });
        animator.start();
    }

    public void addFoodstuff(Foodstuff foodstuff) {
        selectedFoodstuffs.add(foodstuff);
        updateSelectedFoodstuffsCounter();
    }

    private void updateSelectedFoodstuffsCounter() {
        selectedFoodstuffsCounter.setText(String.valueOf(selectedFoodstuffs.size()));
    }

    private int getParentHeight() {
        View snackbarParent = (View) snackbarLayout.getParent();
        return snackbarParent.getHeight();
    }

    public void setOnBasketClickRunnable(Runnable runnable) {
        snackbarLayout.findViewById(R.id.basket).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runnable.run();
            }
        });
    }

    public List<Foodstuff> getSelectedFoodstuffs() {
        return selectedFoodstuffs;
    }

    public void setSelectedFoodstuffs(List<Foodstuff> selectedFoodstuffs) {
        this.selectedFoodstuffs = selectedFoodstuffs;
        updateSelectedFoodstuffsCounter();
    }

    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(IS_SNACKBAR_SHOWN, isSnackbarShown);
        out.putParcelableArrayList(SELECTED_FOODSTUFFS, new ArrayList<>(selectedFoodstuffs));
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(IS_SNACKBAR_SHOWN)) {
            setSelectedFoodstuffs(savedInstanceState.getParcelableArrayList(SELECTED_FOODSTUFFS));
            show();
        }
    }
}
