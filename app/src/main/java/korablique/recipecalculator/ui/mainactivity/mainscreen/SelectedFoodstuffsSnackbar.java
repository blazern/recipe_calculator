package korablique.recipecalculator.ui.mainactivity.mainscreen;


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.behavior.SwipeDismissBehavior;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.model.WeightedFoodstuff;

import static korablique.recipecalculator.util.FloatUtils.areFloatsEquals;

public class SelectedFoodstuffsSnackbar {
    private static final long DURATION = 250L;
    private static final String IS_SNACKBAR_SHOWN = "IS_SNACKBAR_SHOWN";
    private static final String SELECTED_FOODSTUFFS = "SELECTED_FOODSTUFFS";
    private boolean isShown;
    private ViewGroup snackbarLayout;
    private TextView selectedFoodstuffsCounter;
    private List<WeightedFoodstuff> selectedFoodstuffs = new ArrayList<>();

    @Nullable
    private Runnable onDismissListener;

    public SelectedFoodstuffsSnackbar(View fragmentView) {
        snackbarLayout = fragmentView.findViewById(R.id.snackbar);
        selectedFoodstuffsCounter = fragmentView.findViewById(R.id.selected_foodstuffs_counter);

        // Настраиваем "высвайпываемость" снекбара
        SwipeDismissBehavior<View> swipeDismissBehavior = new SwipeDismissBehavior<>();
        swipeDismissBehavior.setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY);

        // Задаём behaviour в часть снекбара, которая позволяет себя "высвайпывать" (swipeable_snackbar_part)
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                snackbarLayout.findViewById(R.id.swipeable_snackbar_part).getLayoutParams();
        params.setBehavior(swipeDismissBehavior);

        // Задаём слушателя "высвайпывания"
        swipeDismissBehavior.setListener(new SwipeDismissBehaviourListener() {
            @Override
            public void onDismiss(View view) {
                // Сбрасываем состояние "высвайпности" (https://stackoverflow.com/a/40193547)
                CoordinatorLayout.LayoutParams swipedParams =
                        (CoordinatorLayout.LayoutParams) view.getLayoutParams();
                swipedParams.setMargins(0, 0, 0, 0);
                view.requestLayout();
                view.setAlpha(1.0f);
                // Прячем основной леяут снекбара так же, как это делается в методе hide
                // (чтобы снекбар потом нормально показался через вызов метода show)
                snackbarLayout.setVisibility(View.INVISIBLE);
                snackbarLayout.setTranslationY(getParentHeight());
                isShown = false;

                // Уведомляем о "высвайпывании"
                if (onDismissListener != null) {
                    onDismissListener.run();
                }
            }
        });
    }

    public void show() {
        snackbarLayout.setVisibility(View.VISIBLE);
        float startValue = snackbarLayout.getHeight();
        float endValue = 0;
        if (!isShown) {
            animateSnackbar(startValue, endValue);
        }
        isShown = true;
    }

    public void hide() {
        float startValue = snackbarLayout.getTranslationY();
        float endValue = getParentHeight();
        animateSnackbar(startValue, endValue, () -> snackbarLayout.setVisibility(View.INVISIBLE));
        isShown = false;
    }

    private void animateSnackbar(float startValue, float endValue) {
        animateSnackbar(startValue, endValue, () -> {});
    }

    private void animateSnackbar(float startValue, float endValue, Runnable finishCallback) {
        ValueAnimator animator = ValueAnimator.ofFloat(startValue, endValue);
        animator.setDuration(DURATION);
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            snackbarLayout.setTranslationY(animatedValue);
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finishCallback.run();
            }
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
        });
        animator.start();
    }

    public void update(List<WeightedFoodstuff> newSelectedFoodstuffs) {
        selectedFoodstuffs.clear();
        if (newSelectedFoodstuffs.isEmpty()) {
            hide();
        } else {
            addFoodstuffs(newSelectedFoodstuffs);
            isShown = true; // чтобы не анимировался
            show();
        }
    }

    public void addFoodstuff(WeightedFoodstuff foodstuff) {
        addFoodstuffs(Collections.singletonList(foodstuff));
    }

    public void addFoodstuffs(List<WeightedFoodstuff> foodstuffs) {
        selectedFoodstuffs.addAll(foodstuffs);
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
        // "Высвайпываемая" часть снекбара уже ждёт жеста свайпа, поэтому вешаем на неё ещё
        // и слушателя кликов - если 2 разные вьюшки в иерархии будут ждать 2 разных жестов,
        // они будут конфликтовать за них.
        snackbarLayout.findViewById(R.id.swipeable_snackbar_part)
                .setOnClickListener(v -> runnable.run());
    }

    public void setOnDismissListener(Runnable dismissListener) {
        this.onDismissListener = dismissListener;
    }

    public List<WeightedFoodstuff> getSelectedFoodstuffs() {
        return selectedFoodstuffs;
    }

    private void setSelectedFoodstuffs(List<WeightedFoodstuff> selectedFoodstuffs) {
        this.selectedFoodstuffs = selectedFoodstuffs;
        updateSelectedFoodstuffsCounter();
    }

    public boolean isShown() {
        return isShown;
    }

    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(IS_SNACKBAR_SHOWN, isShown);
        out.putParcelableArrayList(SELECTED_FOODSTUFFS, new ArrayList<>(selectedFoodstuffs));
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(IS_SNACKBAR_SHOWN)) {
            setSelectedFoodstuffs(savedInstanceState.getParcelableArrayList(SELECTED_FOODSTUFFS));
            show();
        }
    }
}