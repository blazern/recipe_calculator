package korablique.recipecalculator.ui.card;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import javax.inject.Inject;

import korablique.recipecalculator.base.BaseBottomDialog;
import korablique.recipecalculator.base.prefs.SharedPrefsManager;
import korablique.recipecalculator.dagger.InjectorHolder;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.calckeyboard.CalcKeyboardController;

public class CardDialog extends BaseBottomDialog {
    private static final String CLICKED_FOODSTUFF = "CLICKED_FOODSTUFF";
    private static final String CLICKED_WEIGHTED_FOODSTUFF = "CLICKED_WEIGHTED_FOODSTUFF";
    private static final String FOODSTUFF_CARD = "FOODSTUFF_CARD";

    @Inject
    CalcKeyboardController calcKeyboardController;
    @Inject
    SharedPrefsManager prefsManager;

    private Card card;
    @Nullable
    private Card.OnMainButtonClickListener button1ClickListener;
    @Nullable
    private Card.OnMainButtonClickListener button2ClickListener;
    @StringRes
    private int button1TextRes;
    @StringRes
    private int button2TextRes;
    @Nullable
    private Card.OnEditButtonClickListener onEditButtonClickListener;
    @Nullable
    private Card.OnCloseButtonClickListener onCloseButtonClickListener = this::dismiss;
    @Nullable
    private Card.OnDeleteButtonClickListener onDeleteButtonClickListener;
    private Runnable onDismissListener;

    private boolean requestedEditingFocus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InjectorHolder.getInjector().inject(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        card = new Card(this, container, calcKeyboardController, prefsManager);
        if (button1ClickListener != null) {
            card.setUpButton1(button1ClickListener, button1TextRes);
        }
        if (button2ClickListener != null) {
            card.setUpButton2(button2ClickListener, button2TextRes);
        }
        card.setOnEditButtonClickListener(onEditButtonClickListener);
        card.setOnCloseButtonClickListener(onCloseButtonClickListener);
        card.setOnDeleteButtonClickListener(onDeleteButtonClickListener);

        Bundle args = getArguments();
        if (args.containsKey(CLICKED_WEIGHTED_FOODSTUFF)) {
            WeightedFoodstuff foodstuff = args.getParcelable(CLICKED_WEIGHTED_FOODSTUFF);
            card.setFoodstuff(foodstuff);
        } else {
            Foodstuff foodstuff = args.getParcelable(CLICKED_FOODSTUFF);
            card.setFoodstuff(foodstuff);
        }

        return card.getCardLayout();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Форсирует открытие клавиатуры на поле ввода
        if (!requestedEditingFocus) {
            card.focusOnEditing();
            requestedEditingFocus = true;
        }
    }

    public Foodstuff extractFoodstuff() {
        if (card != null) {
            return card.extractFoodstuff();
        }
        Bundle args = getArguments();
        if (args.containsKey(CLICKED_WEIGHTED_FOODSTUFF)) {
            WeightedFoodstuff foodstuff = args.getParcelable(CLICKED_WEIGHTED_FOODSTUFF);
            return foodstuff.withoutWeight();
        } else {
            Foodstuff foodstuff = args.getParcelable(CLICKED_FOODSTUFF);
            return foodstuff;
        }
    }

    public void setUpButton1(
            Card.OnMainButtonSimpleClickListener listener, @StringRes int buttonTextRes) {
        setUpButton1(listener.convert(), buttonTextRes);
    }

    public void setUpButton1(
            Card.OnMainButtonClickListener listener, @StringRes int buttonTextRes) {
        button1ClickListener = listener;
        this.button1TextRes = buttonTextRes;
        if (card != null) {
            card.setUpButton1(listener, buttonTextRes);
        }
    }

    public void setUpButton2(
            Card.OnMainButtonSimpleClickListener listener, @StringRes int buttonTextRes) {
        setUpButton2(listener.convert(), buttonTextRes);
    }

    public void setUpButton2(
            Card.OnMainButtonClickListener listener, @StringRes int buttonTextRes) {
        button2ClickListener = listener;
        this.button2TextRes = buttonTextRes;
        if (card != null) {
            card.setUpButton2(listener, buttonTextRes);
        }
    }

    public void deinitButton1() {
        button1ClickListener = null;
        if (card != null) {
            card.deinitButton1();
        }
    }

    public void deinitButton2() {
        button2ClickListener = null;
        if (card != null) {
            card.deinitButton2();
        }
    }

    public void setOnEditButtonClickListener(Card.OnEditButtonClickListener listener) {
        onEditButtonClickListener = listener;
        if (card != null) {
            card.setOnEditButtonClickListener(listener);
        }
    }

    public void setOnCloseButtonClickListener(Card.OnCloseButtonClickListener listener) {
        onCloseButtonClickListener = listener;
        if (card != null) {
            card.setOnCloseButtonClickListener(listener);
        }
    }

    public void setOnDeleteButtonClickListener(Card.OnDeleteButtonClickListener listener) {
        onDeleteButtonClickListener = listener;
        if (card != null) {
            card.setOnDeleteButtonClickListener(listener);
        }
    }

    public static CardDialog showCard(FragmentActivity activity, WeightedFoodstuff foodstuff) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(CLICKED_WEIGHTED_FOODSTUFF, foodstuff);
        return createDialogWith(activity, bundle);
    }

    public static CardDialog showCard(FragmentActivity activity, Foodstuff foodstuff) {
        CardDialog existingDialog = findCard(activity);
        if (existingDialog != null) {
            existingDialog.card.setFoodstuff(foodstuff);
            return existingDialog;
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(CLICKED_FOODSTUFF, foodstuff);
        return createDialogWith(activity, bundle);
    }

    @NonNull
    private static CardDialog createDialogWith(FragmentActivity activity, Bundle args) {
        CardDialog dialog = new CardDialog();
        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), FOODSTUFF_CARD);
        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onDismissListener != null) {
            onDismissListener.run();
        }
    }

    public void setOnDismissListener(@Nullable Runnable onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    public static void hideCard(FragmentActivity activity) {
        CardDialog cardDialog = findCard(activity);
        if (cardDialog != null) {
            cardDialog.dismiss();
        }
    }
    // Метод нужен, чтоб обрабатывать такую ситуацию:
    // При смене конфигурации экрана (пересоздании активити) диалог уже может находиться в пересозданной активити.
    // Если он уже существует, то ему надо задать onAddFoodstuffButtonClickListener
    @Nullable public static CardDialog findCard(FragmentActivity activity) {
        return (CardDialog) activity.getSupportFragmentManager().findFragmentByTag(FOODSTUFF_CARD);
    }
}
