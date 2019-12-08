package korablique.recipecalculator.ui.mainactivity.mainscreen;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.subjects.PublishSubject;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketList;

@FragmentScope
public class MainScreenSearchController
        implements ActivityCallbacks.Observer, FragmentCallbacks.Observer {
    private static final int SEARCH_SUGGESTIONS_NUMBER = 3;
    private final MainThreadExecutor mainThreadExecutor;
    private final BucketList bucketList;
    private final FoodstuffsList foodstuffsList;
    private final FragmentActivity context;
    private final ActivityCallbacks activityCallbacks;
    private final MainScreenCardController cardController;
    private final MainScreenReadinessDispatcher mainScreenReadinessDispatcher;
    private final RxActivitySubscriptions activitySubscriptions;
    private FloatingSearchView searchView;

    private BucketList.Observer bucketListObserver = new BucketList.Observer() {
        @Override
        public void onFoodstuffAdded(WeightedFoodstuff weightedFoodstuff) {
            searchView.clearQuery();
        }
    };

    private FoodstuffsList.Observer foodstuffsListObserver = new FoodstuffsList.Observer() {
        // подписываемся на FoodstuffsList, чтобы после добавления нового продукта
        // он отображался в результатах поиска
        @Override
        public void onFoodstuffSaved(Foodstuff savedFoodstuff, int index) {
            // Поиск заново
            SearchResultsFragment.show(searchView.getQuery(), context);
            performSearch(searchView.getQuery());
        }
        @Override
        public void onFoodstuffDeleted(Foodstuff deleted) {
            // Поиск заново
            SearchResultsFragment.show(searchView.getQuery(), context);
            performSearch(searchView.getQuery());
        }
    };

    // Disposable чтобы отменить последний начатый поиск при старте нового.
    // Такая отмена нужна на случай, если поиск 2 будет быстрее поиска 1 - чтобы между
    // поисками не было состояния гонки и именно последний поиск всегда был отображен.
    // По-умолчанию Disposables.empty() чтобы не нужно было делать проверки на null.
    private Disposable lastSearchDisposable = Disposables.empty();

    // Rx-Паблишер, в который мы будем загонять все результаты поиска.
    // Механика такая:
    // 1. Стартуем поиск в разных местах класса, и в этих местах не обрабатываем результаты,
    // 2. Вместо обработки результатов в этих местах, постим их в searchResultsPublisher,
    // 3. В configureSearch подписываемся на searchResultsPublisher, и при поступлении любых
    //    результатов отображаем их либо в подсказках, либо в SearchResultFragment.
    // Т.о. этот Rx-Паблишер "разрывает" старт поиска и обработку его результатов.
    private PublishSubject<SearchResult> searchResultsPublisher = PublishSubject.create();

    @Inject
    public MainScreenSearchController(
            MainThreadExecutor mainThreadExecutor,
            FoodstuffsList foodstuffsList,
            BaseFragment fragment,
            ActivityCallbacks activityCallbacks,
            FragmentCallbacks fragmentCallbacks,
            MainScreenCardController cardController,
            MainScreenReadinessDispatcher mainScreenReadinessDispatcher,
            RxActivitySubscriptions activitySubscriptions) {
        this.mainThreadExecutor = mainThreadExecutor;
        this.bucketList = BucketList.getInstance();
        this.foodstuffsList = foodstuffsList;
        this.context = fragment.getActivity();
        this.activityCallbacks = activityCallbacks;
        this.cardController = cardController;
        this.mainScreenReadinessDispatcher = mainScreenReadinessDispatcher;
        this.activitySubscriptions = activitySubscriptions;
        fragmentCallbacks.addObserver(this);
    }

    @Override
    public  void onFragmentDestroy() {
        bucketList.removeObserver(bucketListObserver);
        foodstuffsList.removeObserver(foodstuffsListObserver);
        activityCallbacks.removeObserver(this);
    }

    @Override
    public void onFragmentViewCreated(View fragmentView, Bundle savedInstanceState) {
        searchView = fragmentView.findViewById(R.id.floating_search_view);
        mainScreenReadinessDispatcher.runWhenReady(() -> {
            configureSuggestionsDisplaying();
            configureSearch();
            bucketList.addObserver(bucketListObserver);
            foodstuffsList.addObserver(foodstuffsListObserver);
            activityCallbacks.addObserver(this);
        });
    }

    private void configureSuggestionsDisplaying() {
        searchView.setOnQueryChangeListener((oldQuery, newQuery) -> {
            performSearch(newQuery);
        });
    }

    private void configureSearch() {
        searchView.setOnFocusChangeListener(new SearchViewFocusListener() {
            @Override
            public void onFocusCleared() {
                // Когда со строки поиска ушёл фокус - очищаем текст поиска.
                // Но только в том случае, если на экране нет результатов поиска.
                // Делаем это в следующей итерации главного UI-цикла, т.к. фокус мог пропасть
                // из-за того, что вот-вот покажутся результаты поиска (в следующей итерации
                // главного UI-цикла они уже будут показаны).
                mainThreadExecutor.execute(() -> clearSearchQueryIfSearchResultsNotShown());
            }
        });

        searchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            // действие при нажатии на подсказку
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                FoodstuffSearchSuggestion suggestion = (FoodstuffSearchSuggestion) searchSuggestion;
                cardController.showCard(suggestion.getFoodstuff());
            }
            // когда пользователь нажал на клавиатуре enter
            @Override
            public void onSearchAction(String currentQuery) {
                SearchResultsFragment.show(currentQuery, context);
                performSearch(currentQuery);
            }
        });

        // когда пользователь нажал кнопку лупы в searchView
        searchView.setOnMenuItemClickListener(item -> {
            SearchResultsFragment.show(searchView.getQuery(), context);
            performSearch(searchView.getQuery());
        });

        // Настроим реакцию UI на результаты всех начинаемых нами поисков
        activitySubscriptions.subscribe(searchResultsPublisher, (searchResult) -> {
            // Если показан фрагмент с результатами поисков - покажем результаты в нём,
            // иначе в подсказках.
            SearchResultsFragment fragment = SearchResultsFragment.findFragment(context);
            if (fragment != null) {
                fragment.setDisplayedSearchResults(searchResult.foodstuffs);
                fragment.setDisplayedQuery(searchResult.query);
            } else {
                List<Foodstuff> foodstuffs = searchResult.foodstuffs;
                List<FoodstuffSearchSuggestion> newSuggestions = new ArrayList<>();
                for (int index = 0; index < SEARCH_SUGGESTIONS_NUMBER && index < foodstuffs.size(); ++index) {
                    FoodstuffSearchSuggestion suggestion = new FoodstuffSearchSuggestion(foodstuffs.get(index));
                    newSuggestions.add(suggestion);
                }
                searchView.swapSuggestions(newSuggestions);
            }
        });
    }

    private void performSearch(String query) {
        lastSearchDisposable.dispose();
        Single<List<Foodstuff>> searchResultSingle = foodstuffsList.requestFoodstuffsLike(query);
        lastSearchDisposable = searchResultSingle.subscribe((foundFoodstuffs) -> {
            searchResultsPublisher.onNext(new SearchResult(query, foundFoodstuffs));
        });
        activitySubscriptions.storeDisposable(lastSearchDisposable);
    }

    private void clearSearchQueryIfSearchResultsNotShown() {
        if (SearchResultsFragment.findFragment(context) != null) {
            return;
        }
        searchView.clearQuery();
    }

    @Override
    public boolean onActivityBackPressed() {
        SearchResultsFragment fragment = SearchResultsFragment.findFragment(context);
        if (fragment != null) {
            SearchResultsFragment.closeFragment(context, fragment);
            // В следующей итерации основного UI-цикла очистим строку поиска, если
            // фрагментов с результатами поиска больше нет
            mainThreadExecutor.execute(this::clearSearchQueryIfSearchResultsNotShown);
            // Мы поглотили событие - сами решили, что должно происходить.
            return true;
        }

        if (!TextUtils.isEmpty(searchView.getQuery())) {
            searchView.clearQuery();
            // Мы поглотили событие - сами решили, что должно происходить.
            return true;
        }

        return false;
    }

    private static class SearchResult {
        final String query;
        final List<Foodstuff> foodstuffs;
        SearchResult(String query, List<Foodstuff> foodstuffs) {
            this.query = query;
            this.foodstuffs = foodstuffs;
        }
    }
}
