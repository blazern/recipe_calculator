package korablique.recipecalculator.ui.bucketlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import korablique.recipecalculator.FloatUtils;
import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.ui.NutritionProgressWithValuesWrapper;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.history.HistoryActivity;

public class BucketListActivity extends BaseActivity {
    public static final String EXTRA_FOODSTUFFS_LIST = "EXTRA_FOODSTUFFS_LIST";
    private NutritionProgressWithValuesWrapper nutritionWrapper;
    @Inject
    DatabaseWorker databaseWorker;
    private BucketListAdapter adapter;
    private EditText totalWeightEditText;
    private Button saveToHistoryButton;
    private Button saveAsSingleFoodstuffButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bucket_list);

        nutritionWrapper =
                new NutritionProgressWithValuesWrapper(this, findViewById(R.id.nutrition_progress_with_values));

        List<Foodstuff> foodstuffs = getIntent().getParcelableArrayListExtra(EXTRA_FOODSTUFFS_LIST);
        if (foodstuffs == null) {
            throw new IllegalArgumentException("Can't start without " + EXTRA_FOODSTUFFS_LIST);
        }

        double totalWeight = countTotalWeight(foodstuffs);
        Nutrition totalNutrition = countTotalNutrition(foodstuffs);
        nutritionWrapper.setNutrition(totalNutrition);

        saveToHistoryButton = findViewById(R.id.save_to_history_button);
        saveAsSingleFoodstuffButton = findViewById(R.id.save_as_single_foodstuff_button);

        totalWeightEditText = findViewById(R.id.total_weight_edit_text);
        totalWeightEditText.setText(String.valueOf(totalWeight));
        totalWeightEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                super.afterTextChanged(s);
                updateSaveButtonsEnability();
            }
        });

        BucketListAdapter.OnItemsCountChangeListener onItemsCountChangeListener = count -> {
            updateSaveButtonsEnability();
        };

        BucketListAdapter.OnItemClickedObserver onItemClickedObserver = (foodstuff, position) -> {
            CardDialog cardDialog = CardDialog.showCard(BucketListActivity.this, foodstuff);
            cardDialog.setOnAddFoodstuffButtonClickListener(newFoodstuff -> {
                adapter.replaceItem(newFoodstuff, position);
                CardDialog.hideCard(BucketListActivity.this);

                Nutrition newNutrition = countTotalNutrition(adapter.getItems());
                nutritionWrapper.setNutrition(newNutrition);

                double newTotalWeight = countTotalWeight(adapter.getItems());
                totalWeightEditText.setText(String.valueOf(newTotalWeight));
            });
        };

        adapter = new BucketListAdapter(R.layout.new_foodstuff_layout, onItemsCountChangeListener, onItemClickedObserver);
        adapter.addItems(foodstuffs);

        RecyclerView foodstuffsListRecyclerView = findViewById(R.id.foodstuffs_list);
        foodstuffsListRecyclerView.setAdapter(adapter);

        OnSwipeItemCallback onSwipeItemCallback = new OnSwipeItemCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                adapter.deleteItem(position);
                double newWeight = countTotalWeight(adapter.getItems());
                totalWeightEditText.setText(String.valueOf(newWeight));

                Nutrition newNutrition = countTotalNutrition(adapter.getItems());
                nutritionWrapper.setNutrition(newNutrition);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(onSwipeItemCallback);
        itemTouchHelper.attachToRecyclerView(foodstuffsListRecyclerView);

        // диалог, появляющийся при сохранении блюда
        SaveDishDialog.OnSaveDishButtonClickListener saveDishButtonClickListener = (foodstuff) -> {
            databaseWorker.saveFoodstuff(this, foodstuff, new DatabaseWorker.SaveFoodstuffCallback() {
                @Override
                public void onResult(long id) {
                    SaveDishDialog dialog = SaveDishDialog.findDialog(BucketListActivity.this);
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    Toast.makeText(BucketListActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onDuplication() {
                    Toast.makeText(BucketListActivity.this, R.string.foodstuff_already_exists, Toast.LENGTH_LONG).show();
                }
            });
        };

        SaveDishDialog existingDialog = SaveDishDialog.findDialog(this);
        if (existingDialog != null) {
            existingDialog.setOnSaveDishButtonClickListener(saveDishButtonClickListener);
        }

        saveAsSingleFoodstuffButton.setOnClickListener((view) -> {
            SaveDishDialog dialog = SaveDishDialog.showDialog(BucketListActivity.this, foodstuffs, totalWeight);
            dialog.setOnSaveDishButtonClickListener(saveDishButtonClickListener);
        });

        saveToHistoryButton.setOnClickListener((view) -> {
            HistoryActivity.startAndAdd(adapter.getItems(), BucketListActivity.this);
            finish();
        });
    }

    private double countTotalWeight(List<Foodstuff> foodstuffs) {
        double result = 0;
        for (Foodstuff foodstuff : foodstuffs) {
            result += foodstuff.getWeight();
        }
        return result;
    }

    private Nutrition countTotalNutrition(List<Foodstuff> foodstuffs) {
        Nutrition totalNutrition = Nutrition.zero();
        for (Foodstuff foodstuff : foodstuffs) {
            totalNutrition = totalNutrition.plus(Nutrition.of(foodstuff));
        }
        return totalNutrition;
    }

    private void updateSaveButtonsEnability() {
        String text = totalWeightEditText.getText().toString();
        if (text.isEmpty()
                || FloatUtils.areFloatsEquals(Double.parseDouble(text), 0.0)
                || adapter.getItemCount() == 0) {
            saveAsSingleFoodstuffButton.setEnabled(false);
            saveToHistoryButton.setEnabled(false);
        } else {
            saveAsSingleFoodstuffButton.setEnabled(true);
            saveToHistoryButton.setEnabled(true);
        }
    }

    public static void start(ArrayList<Foodstuff> foodstuffs, Context context) {
        context.startActivity(createStartIntentFor(foodstuffs, context));
    }

    public static Intent createStartIntentFor(List<Foodstuff> foodstuffs, Context context) {
        Intent intent = new Intent(context, BucketListActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_FOODSTUFFS_LIST, new ArrayList<>(foodstuffs));
        return intent;
    }
}
