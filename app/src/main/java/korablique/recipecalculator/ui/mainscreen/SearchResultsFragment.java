package korablique.recipecalculator.ui.mainscreen;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.editfoodstuff.EditFoodstuffActivity;

public class SearchResultsFragment extends BaseFragment {
    public static final String SEARCH_RESULTS_FRAGMENT_TAG = "SEARCH_RESULTS_FRAGMENT_TAG";
    public static final String REQUEST = "REQUEST";
    @Inject
    DatabaseWorker databaseWorker;
    @Inject
    Lifecycle lifecycle;
    @Inject
    MainActivity mainActivity;
    @Inject
    FoodstuffsList foodstuffsList;
    @Inject
    RxFragmentSubscriptions fragmentSubscriptions;

    @Override
    protected View createView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.search_results_layout, container, false);
        String query = getArguments().getString(REQUEST);

        TextView searchRequestTextView = fragmentView.findViewById(R.id.search_results_text_view);
        searchRequestTextView.setText(getResources().getString(R.string.search_results, query));

        RecyclerView searchResultsRecyclerView = fragmentView.findViewById(R.id.search_results_recycler_view);
        SearchResultsAdapter adapter = new SearchResultsAdapter(getActivity(), (foodstuff, position) -> {
            CardDialog cardDialog = CardDialog.showCard(getActivity(), foodstuff);
            cardDialog.prohibitEditing(true);
            cardDialog.setUpAddFoodstuffButton(foodstuff1 -> {
                CardDialog.hideCard(getActivity());
                BucketList bucketList = BucketList.getInstance();
                bucketList.add(foodstuff1);
                closeThisFragment();
            }, R.string.add_foodstuff);
            cardDialog.setOnDeleteButtonClickListener(foodstuff2 -> {
                CardDialog.hideCard(getActivity());
                foodstuffsList.deleteFoodstuff(foodstuff2.withoutWeight());
            });
        });
        searchResultsRecyclerView.setAdapter(adapter);

        performSearch(query, adapter, fragmentView);

        Button addNewFoodstuffButton = fragmentView.findViewById(R.id.add_new_foodstuff_button);
        addNewFoodstuffButton.setOnClickListener(v -> {
            EditFoodstuffActivity.startForCreation(fragmentView.getContext());
        });
        // подписываемся на FoodstuffsList, чтобы после добавления нового продукта
        // он отображался в результатах поиска
        FoodstuffsList.Observer foodstuffsListObserver = new FoodstuffsList.Observer() {
            @Override
            public void onFoodstuffSaved(Foodstuff savedFoodstuff, int index) {
                // старые результаты поиска удалить, заново осуществить поиск
                adapter.clear();
                performSearch(query, adapter, fragmentView);
            }

            @Override
            public void onFoodstuffDeleted(Foodstuff deleted) {
                adapter.removeItem(deleted);
            }
        };
        foodstuffsList.addObserver(foodstuffsListObserver);
        return fragmentView;
    }

    private void performSearch(String query, SearchResultsAdapter adapter, View fragmentView) {
        Single<List<Foodstuff>> searchResultSingle = foodstuffsList.requestFoodstuffsLike(query);
        fragmentSubscriptions.subscribe(searchResultSingle, (searchResult) -> {
            adapter.addItems(searchResult);

            if (adapter.getItemCount() == 0) {
                fragmentView.findViewById(R.id.nothing_found_view).setVisibility(View.VISIBLE);
            } else {
                fragmentView.findViewById(R.id.nothing_found_view).setVisibility(View.GONE);
            }
        });
    }

    private void closeThisFragment() {
        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
    }

    public static void show(String request, FragmentActivity context) {
        Fragment searchResultsFragment = new SearchResultsFragment();
        Bundle args = new Bundle();
        args.putString(REQUEST, request);
        searchResultsFragment.setArguments(args);
        FragmentTransaction transaction = context.getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, searchResultsFragment, SEARCH_RESULTS_FRAGMENT_TAG);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
