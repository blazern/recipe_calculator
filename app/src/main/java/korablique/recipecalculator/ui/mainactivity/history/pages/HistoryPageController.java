package korablique.recipecalculator.ui.mainactivity.history.pages;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.Optional;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.card.Card;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.mainactivity.history.DividerItemDecorationWithoutDividerAfterLastItem;
import korablique.recipecalculator.ui.mainactivity.history.HistoryAdapter;
import korablique.recipecalculator.ui.mainactivity.history.HistoryNutritionValuesWrapper;
import korablique.recipecalculator.ui.mainactivity.history.NutritionProgressWrapper;

@FragmentScope
public class HistoryPageController implements
        FragmentCallbacks.Observer,
        HistoryWorker.HistoryChangeObserver {
    private static final int CARD_BUTTON_TEXT_RES = R.string.save;
    private LocalDate date;
    private BaseActivity context;
    private View fragmentView;
    private HistoryWorker historyWorker;
    private UserParametersWorker userParametersWorker;
    private RxFragmentSubscriptions subscriptions;
    private HistoryAdapter adapter;
    private HistoryNutritionValuesWrapper nutritionValuesWrapper;
    private NutritionProgressWrapper nutritionProgressWrapper;
    private Card.OnMainButtonClickListener onAddFoodstuffButtonClickListener
            = new Card.OnMainButtonClickListener() {
        @Override
        public void onClick(WeightedFoodstuff foodstuff) {
            CardDialog.hideCard(context);
            long replacedItemId = adapter.replaceItem(foodstuff);
            historyWorker.editWeightInHistoryEntry(replacedItemId, foodstuff.getWeight());

            // update wrappers
            Nutrition updatedNutrition = Nutrition.zero();
            for (HistoryEntry entry : adapter.getItems()) {
                updatedNutrition = updatedNutrition.plus(Nutrition.of(entry.getFoodstuff()));
            }
            Single<Optional<UserParameters>> currentUserParamsSingle = userParametersWorker.requestCurrentUserParameters();
            Nutrition finalUpdatedNutrition = updatedNutrition;
            subscriptions.subscribe(currentUserParamsSingle, new Consumer<Optional<UserParameters>>() {
                @Override
                public void accept(Optional<UserParameters> userParametersOptional) {
                    UserParameters currentUserParams = userParametersOptional.get();
                    Rates rates = RateCalculator.calculate(currentUserParams);
                    nutritionProgressWrapper.setProgresses(finalUpdatedNutrition, rates);
                    nutritionValuesWrapper.setNutrition(finalUpdatedNutrition, rates);
                }
            });
        }
    };
    private Card.OnDeleteButtonClickListener onDeleteButtonClickListener = new Card.OnDeleteButtonClickListener() {
        @Override
        public void onClick(WeightedFoodstuff foodstuff) {
            CardDialog.hideCard(context);
            HistoryEntry removingItem = adapter.removeItem(foodstuff);
            historyWorker.deleteEntryFromHistory(removingItem);

            // update wrappers
            Nutrition updatedNutrition = nutritionValuesWrapper.getCurrentNutrition()
                    .minus(Nutrition.of(foodstuff));
            Single<Optional<UserParameters>> currentUserParamsSingle = userParametersWorker.requestCurrentUserParameters();
            subscriptions.subscribe(currentUserParamsSingle, new Consumer<Optional<UserParameters>>() {
                @Override
                public void accept(Optional<UserParameters> userParametersOptional) {
                    UserParameters currentUserParams = userParametersOptional.get();
                    Rates rates = RateCalculator.calculate(currentUserParams);
                    nutritionProgressWrapper.setProgresses(updatedNutrition, rates);
                    nutritionValuesWrapper.setNutrition(updatedNutrition, rates);
                }
            });
        }
    };

    @Inject
    public HistoryPageController(
            HistoryPageFragment fragment,
            HistoryWorker historyWorker,
            UserParametersWorker userParametersWorker,
            FragmentCallbacks fragmentCallbacks,
            RxFragmentSubscriptions subscriptions) {
        fragmentCallbacks.addObserver(this);
        this.context = (BaseActivity) fragment.getActivity();
        this.historyWorker = historyWorker;
        this.userParametersWorker = userParametersWorker;
        this.subscriptions = subscriptions;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        this.fragmentView = fragmentView;
    }

    // Наш фрагмент переиспользуется внутри ViewPager (ресайклится), поэтому зададим значения в
    // в onFragmentViewStateRestored, а не в onFragmentViewCreated, иначе
    // onFragmentViewStateRestored перетрёт всё, что мы зададим в onFragmentViewCreated.
    @Override
    public void onFragmentViewStateRestored(Bundle savedInstanceState) {
        // обёртки заголовка с БЖУК (значений и прогрессов БЖУК)
        ViewGroup nutritionHeaderParentLayout = fragmentView.findViewById(R.id.nutrition_parent_layout);
        nutritionValuesWrapper = new HistoryNutritionValuesWrapper(
                context, nutritionHeaderParentLayout);
        nutritionProgressWrapper = new NutritionProgressWrapper(nutritionHeaderParentLayout);

        RecyclerView recyclerView = fragmentView.findViewById(R.id.history_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(fragmentView.getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new HistoryAdapter(fragmentView.getContext());

        initHistoryList(fragmentView);
        initCard();
        updateWrappers();

        historyWorker.addHistoryChangeObserver(this);
    }

    @Override
    public void onFragmentDestroy() {
        historyWorker.removeHistoryChangeObserver(this);
    }

    private void initCard() {
        CardDialog existingCardDialog = CardDialog.findCard(context);
        if (existingCardDialog != null) {
            existingCardDialog.setUpButton1(onAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
            existingCardDialog.setOnDeleteButtonClickListener(onDeleteButtonClickListener);
        }
    }

    private void initHistoryList(View fragmentView) {
        RecyclerView recyclerView = fragmentView.findViewById(R.id.history_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(fragmentView.getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new HistoryAdapter(fragmentView.getContext());
        recyclerView.setAdapter(adapter);
        DividerItemDecorationWithoutDividerAfterLastItem dividerItemDecoration =
                new DividerItemDecorationWithoutDividerAfterLastItem(
                        recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(fragmentView.getResources().getDrawable(R.drawable.divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    private void updateWrappers() {
        // загрузить историю за выбранный день
        DateTime from = new DateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 0, 0, 0);
        DateTime to = new DateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 23, 59, 59);
        Single<List<HistoryEntry>> historySingle =
                historyWorker.requestHistoryForPeriod(from.getMillis(), to.getMillis()).toList();
        Single<Optional<UserParameters>> currentUserParamsSingle = userParametersWorker.requestCurrentUserParameters();
        Single<Pair<List<HistoryEntry>, Optional<UserParameters>>> historyWithUserParamsSingle =
                historySingle.zipWith(currentUserParamsSingle, Pair::create);
        subscriptions.subscribe(historyWithUserParamsSingle, historyAndUserParamsPair -> {
            List<HistoryEntry> historyEntries = historyAndUserParamsPair.first;
            UserParameters currentUserParams = historyAndUserParamsPair.second.get();

            adapter.clear();
            adapter.addItems(historyEntries);
            // листенер на нажатия на элемент адаптера
            adapter.setOnItemClickObserver((historyEntry) -> {
                CardDialog card = CardDialog.showCard(context, historyEntry.getFoodstuff());
                card.prohibitEditing(true);
                card.setUpButton1(onAddFoodstuffButtonClickListener, CARD_BUTTON_TEXT_RES);
                card.setOnDeleteButtonClickListener(onDeleteButtonClickListener);
            });
            updateWrappers(historyEntries, currentUserParams);
        });
    }

    private void updateWrappers(List<HistoryEntry> historyEntries, UserParameters currentUserParams) {
        Nutrition totalNutrition = Nutrition.zero();
        for (HistoryEntry entry : historyEntries) {
            WeightedFoodstuff foodstuff = entry.getFoodstuff();
            totalNutrition = totalNutrition.plus(Nutrition.of(foodstuff));
        }

        // заполнение заголовка с БЖУК
        Rates rates = RateCalculator.calculate(currentUserParams);
        nutritionValuesWrapper.setNutrition(totalNutrition, rates);
        nutritionProgressWrapper.setProgresses(totalNutrition, rates);
    }

    @Override
    public void onHistoryChange() {
        // Если История поменялась, но фрагмент Истории не показан - История была изменена
        // не через экран Истории - обновимся, чтобы при заходе на экран Истории были отображены
        // правильные продукты.
        if (!fragmentView.isShown()) {
            updateWrappers();
        }
    }
}
