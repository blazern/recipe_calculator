package korablique.recipecalculator.ui.mainactivity.mainscreen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.Lifecycle;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import korablique.recipecalculator.R;
import korablique.recipecalculator.RequestCodes;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.RecipesRepository;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Recipe;
import korablique.recipecalculator.ui.KeyboardHandler;
import korablique.recipecalculator.ui.TwoOptionsDialog;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.ui.card.Card;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;
import korablique.recipecalculator.ui.mainactivity.MainActivitySelectedDateStorage;
import korablique.recipecalculator.ui.mainactivity.mainscreen.modes.MainScreenMode;
import korablique.recipecalculator.ui.mainactivity.mainscreen.modes.MainScreenModesController;

import static android.app.Activity.RESULT_OK;

@FragmentScope
public class MainScreenCardController implements FragmentCallbacks.Observer {
    private static final String ADD_FOODSTUFF_TO_ANOTHER_DATE_DIALOG_TAG =
            "ADD_FOODSTUFF_TO_ANOTHER_DATE_DIALOG_TAG";
    @StringRes
    private static final int ADD_FOODSTUFF_TO_HISTORY_CARD_TEXT = R.string.add_foodstuff_to_history;
    private final BaseActivity context;
    private final BaseFragment fragment;
    private final Lifecycle lifecycle;
    private final HistoryWorker historyWorker;
    private final TimeProvider timeProvider;
    private final MainActivitySelectedDateStorage selectedDateStorage;
    private final RecipesRepository recipesRepository;
    private final FoodstuffsList foodstuffsList;
    private final RxFragmentSubscriptions rxSubscriptions;
    private final MainScreenModesController mainScreenModesController;
    private final BucketList bucketList;
    private final FoodstuffsList.Observer foodstuffsListObserver = new FoodstuffsList.Observer() {
        @Override
        public void onFoodstuffEdited(Foodstuff edited) {
            CardDialog card = CardDialog.findCard(context);
            if (card != null && card.extractFoodstuff().getId() == edited.getId()) {
                showCard(edited);
            }
        }
    };
    private Card.OnMainButtonSimpleClickListener onAddFoodstuffToHistoryListener;
    private Card.OnEditButtonClickListener onEditButtonClickListener;

    private final List<Observer> observers = new CopyOnWriteArrayList<>();
    private boolean lastCardClosingReportedToObservers;

    // Действие, которое нужно выполнить с диалогом после savedInstanceState (показ или скрытие диалога)
    // Поле нужно, чтобы приложение не крешило при показе диалога, когда тот показывается в момент,
    // когда активити в фоне (запаузена).
    // fragment manager не позваляет выполнять никакие операции с фрагментами, пока активити запаузена -
    // ведь fragment manager уже сохранил состояние всех фрагментов,
    // и ещё раз это сделать до резьюма активити невозможно (больше не вызовается Activity.onSaveInstanceState).
    // Чтобы сохранение стейта случилось ещё раз, активити должна выйти на передний план.
    // А когда активити в фоне, неизвестно, выйдет ли она на передний план - fm от этой неизвестности страхуется исключением.
    // (Если не выйдет, то будет потеря состояния.)
    // (Тут иерархичное подчинение - ОС требует от Активити сохранение стейта,
    // Активти требует от всех своих компонентов, в т.ч. от fm,
    // а fm требует сохранение стейта от всех своих компонентов, и т.д.)
    private Runnable dialogAction;

    public interface Observer {
        /**
         * Пользователь нажал на одну из кнопок, выполняющую действия.
         * Например, на "Добавить в журнал".
         */
        default void onCardClosedByPerformedAction() {}

        /**
         * Карточка закрыта без какого-либо влияния на что-либо.
         * Например, нажатием на крестик.
         */
        default void onCardDismissed() {}
    }

    @Inject
    public MainScreenCardController(
            BaseActivity context,
            BaseFragment fragment,
            FragmentCallbacks fragmentCallbacks,
            Lifecycle lifecycle,
            HistoryWorker historyWorker,
            TimeProvider timeProvider,
            MainActivitySelectedDateStorage selectedDateStorage,
            RecipesRepository recipesRepository,
            FoodstuffsList foodstuffsList,
            RxFragmentSubscriptions rxSubscriptions,
            MainScreenModesController mainScreenModesController,
            BucketList bucketList) {
        this.context = context;
        this.fragment = fragment;
        this.lifecycle = lifecycle;
        this.historyWorker = historyWorker;
        this.timeProvider = timeProvider;
        this.selectedDateStorage = selectedDateStorage;
        this.recipesRepository = recipesRepository;
        this.foodstuffsList = foodstuffsList;
        this.rxSubscriptions = rxSubscriptions;
        this.mainScreenModesController = mainScreenModesController;
        this.bucketList = bucketList;
        fragmentCallbacks.addObserver(this);
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        TwoOptionsDialog existingAnotherDateDialog =
                TwoOptionsDialog.findDialog(context.getSupportFragmentManager(), ADD_FOODSTUFF_TO_ANOTHER_DATE_DIALOG_TAG);
        if (existingAnotherDateDialog != null) {
            // Малозначительный диалог, не будем хранить его стейт и восстанавливать при смене
            // сессии.
            existingAnotherDateDialog.dismiss();
        }

        onAddFoodstuffToHistoryListener = foodstuff -> {
            new KeyboardHandler(context).hideKeyBoard();

            LocalDate selectedDate = selectedDateStorage.getSelectedDate();
            DateTime now = timeProvider.now();
            String selectedDateStr = selectedDate.toString("dd.MM.yy");
            String nowStr = now.toLocalDate().toString("dd.MM.yy");
            if (nowStr.equals(selectedDateStr)) {
                hideCardAfterUserAction();
                historyWorker.saveFoodstuffToHistory(
                        timeProvider.now().toDate(), foodstuff.getId(), foodstuff.getWeight());
            } else {
                TwoOptionsDialog dialog = TwoOptionsDialog.showDialog(
                        context.getSupportFragmentManager(),
                        ADD_FOODSTUFF_TO_ANOTHER_DATE_DIALOG_TAG,
                        context.getString(R.string.add_foodstuff_to_other_date_dialog_title, selectedDateStr),
                        context.getString(R.string.add_foodstuff_to_other_date_dialog_other_date_response, selectedDateStr),
                        context.getString(R.string.add_foodstuff_to_other_date_dialog_current_day_response));
                dialog.setOnButtonsClickListener(buttonName -> {
                    if (buttonName == TwoOptionsDialog.ButtonName.POSITIVE) {
                        hideCardAfterUserAction();
                        historyWorker.saveFoodstuffToHistoryAfterAllOther(
                                selectedDate.toDate(), foodstuff.getId(), foodstuff.getWeight());
                    } else if (buttonName == TwoOptionsDialog.ButtonName.NEGATIVE) {
                        hideCardAfterUserAction();
                        historyWorker.saveFoodstuffToHistory(
                                now.toDate(), foodstuff.getId(), foodstuff.getWeight());
                        selectedDateStorage.setSelectedDate(now.toLocalDate());
                    } else {
                        throw new IllegalStateException("Unknown button: " + buttonName);
                    }
                    dialog.dismiss();
                });

            }
        };
        onEditButtonClickListener = foodstuff -> {
            rxSubscriptions.subscribe(
                    recipesRepository.getRecipeOfFoodstuffRx(foodstuff),
                    (Optional<Recipe> recipe) -> {
                        if (recipe.isPresent()) {
                            BucketListActivity.startForRecipe(
                                    fragment,
                                    RequestCodes.MAIN_SCREEN_BUCKET_LIST_CREATE_FOODSTUFF,
                                    recipe.get(),
                                    true);
                        } else {
                            EditFoodstuffActivity.startForEditing(fragment, foodstuff, RequestCodes.MAIN_SCREEN_CARD_EDIT_FOODSTUFF);
                        }
                    });
        };
        foodstuffsList.addObserver(foodstuffsListObserver);
    }

    @Override
    public void onFragmentDestroy() {
        foodstuffsList.addObserver(foodstuffsListObserver);
    }

    @Override
    public void onFragmentResume() {
        if (dialogAction != null) {
            dialogAction.run();
        }
        // If we were resumed with a showing card, and BucketList also
        // contains card's recipe - we probably were resumed because user
        // wants to add an ingredient to the recipe.
        // In such case we should close the card, because user probably doesn't want
        // to add the recipe into itself as an ingredient.
        CardDialog cardDialog = CardDialog.findCard(context);
        if (cardDialog != null
                && bucketList.getRecipe().getFoodstuff().getId() == cardDialog.extractFoodstuff().getId()) {
            cardDialog.dismiss();
        }
    }

    @Override
    public void onFragmentActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodes.MAIN_SCREEN_CARD_EDIT_FOODSTUFF
                && resultCode == RESULT_OK) {
            Foodstuff editedFoodstuff = data.getParcelableExtra(EditFoodstuffActivity.EXTRA_RESULT_FOODSTUFF);
            if (editedFoodstuff == null) {
                hideCardAfterUserAction();
            } else {
                showCard(editedFoodstuff);
            }
        }
    }

    public void showCard(Foodstuff foodstuff) {
        dialogAction = () -> {
            CardDialog cardDialog = CardDialog.showCard(context, foodstuff);
            setUpCard(cardDialog);
            dialogAction = null;
        };
        if (lifecycle.getCurrentState() == Lifecycle.State.RESUMED) {
            dialogAction.run();
        }
    }

    public void hideCardAfterUserAction() {
        dialogAction = () -> {
            if (!lastCardClosingReportedToObservers) {
                // NOTE: мы сперва репортим слушателям о закрытой карточке,
                // и только затем её закрываем. Это нужно из-за того, что закрытие карточки
                // приведёт к вызову у неё onDismissed, а по событию onDismissed мы репортим
                // событие закрытия карточки без полезных действий (Observer.onCardDismissed).
                lastCardClosingReportedToObservers = true;
                for (Observer observer : observers) {
                    observer.onCardClosedByPerformedAction();
                }
            }
            CardDialog.hideCard(context);
            dialogAction = null;
        };
        if (lifecycle.getCurrentState() == Lifecycle.State.RESUMED) {
            dialogAction.run();
        }
    }

    private void setUpCard(CardDialog card) {
        lastCardClosingReportedToObservers = false;
        card.setOnEditButtonClickListener(onEditButtonClickListener);
        card.setOnDismissListener(() -> {
            if (!lastCardClosingReportedToObservers) {
                lastCardClosingReportedToObservers = true;
                for (Observer observer : observers) {
                    observer.onCardDismissed();
                }
            }
        });

        MainScreenMode.CardButtonSettings button1DefaultSettings =
                new MainScreenMode.CardButtonSettings(
                        onAddFoodstuffToHistoryListener,
                        ADD_FOODSTUFF_TO_HISTORY_CARD_TEXT,
                        true,
                        true);
        setUpCardButton(card, 1,
                mainScreenModesController.setupCardButton1(card.extractFoodstuff()),
                button1DefaultSettings);

        setUpCardButton(card, 2,
                mainScreenModesController.setupCardButton2(card.extractFoodstuff()),
                null);
    }

    private void setUpCardButton(
            CardDialog card,
            int buttonNumber,
            Single<Optional<MainScreenMode.CardButtonSettings>> futureSettings,
            @Nullable MainScreenMode.CardButtonSettings defaultSettings) {
        Disposable d = futureSettings.subscribe((params) -> {
            if (params.isPresent()) {
                // On button click, first notify our observers and close keyboard,
                // only then call the received clicks listener.
                card.setUpButtonN(
                        buttonNumber,
                        foodstuff -> {
                            // TODO: test
                            if (params.get().getCloseCardOnButtonClick()) {
                                hideCardAfterUserAction();
                            }
                            new KeyboardHandler(context).hideKeyBoard();
                            params.get().getClickListener().onClick(foodstuff);
                        },
                        params.get().getBtnTitleStrId());
                card.setDisableButtonNWhenWeight0(
                        buttonNumber, params.get().getDisableButtonWhenWeight0());
            } else if (defaultSettings != null) {
                card.setUpButtonN(
                        buttonNumber,
                        defaultSettings.getClickListener(),
                        defaultSettings.getBtnTitleStrId());
                card.setDisableButtonNWhenWeight0(
                        buttonNumber,
                        defaultSettings.getDisableButtonWhenWeight0());
            }
        });
        rxSubscriptions.storeDisposable(d);
    }
}
