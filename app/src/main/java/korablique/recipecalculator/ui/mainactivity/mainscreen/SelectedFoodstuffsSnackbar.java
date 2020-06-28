package korablique.recipecalculator.ui.mainactivity.mainscreen;

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.transition.TransitionManager;

import com.google.android.material.behavior.SwipeDismissBehavior;

import korablique.recipecalculator.R;

public class SelectedFoodstuffsSnackbar {
    private final ViewGroup snackbarLayout;
    private final TextView selectedFoodstuffsCounter;

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
                hide();

                // Уведомляем о "высвайпывании"
                if (onDismissListener != null) {
                    onDismissListener.run();
                }

                // Сбрасываем состояние "высвайпности" (https://stackoverflow.com/a/40193547)
                new Handler().postDelayed(() -> {
                    CoordinatorLayout.LayoutParams swipedParams =
                            (CoordinatorLayout.LayoutParams) view.getLayoutParams();
                    swipedParams.setMargins(0, 0, 0, 0);
                    view.requestLayout();
                    view.setAlpha(1.0f);
                }, 250);
            }
        });

        // В начале спрятан
        hide();
    }

    public void show(String title) {
        ConstraintLayout parent = (ConstraintLayout) snackbarLayout.getParent();
        TransitionManager.beginDelayedTransition(parent);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(parent);
        constraintSet.clear(
                snackbarLayout.getId(), ConstraintSet.TOP);
        constraintSet.connect(
                snackbarLayout.getId(), ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        constraintSet.applyTo(parent);

        TextView titleView = snackbarLayout.findViewById(R.id.selected_foodstuffs_snackbar_title);
        titleView.setText(title);
    }

    public void hide() {
        ConstraintLayout parent = (ConstraintLayout) snackbarLayout.getParent();
        TransitionManager.beginDelayedTransition(parent);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(parent);
        constraintSet.connect(
                snackbarLayout.getId(), ConstraintSet.TOP,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        constraintSet.clear(
                snackbarLayout.getId(), ConstraintSet.BOTTOM);
        constraintSet.applyTo(parent);
    }

    public void updateSelectedFoodstuffsCounter(int count) {
        selectedFoodstuffsCounter.setText(String.valueOf(count));
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
                .setOnClickListener(v -> {
                    if (runnable != null) {
                        runnable.run();
                    }
                });
    }

    public void setOnDismissListener(Runnable dismissListener) {
        this.onDismissListener = dismissListener;
    }
}
