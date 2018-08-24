package korablique.recipecalculator.ui.editfoodstuff;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter;

import javax.inject.Inject;

import korablique.recipecalculator.R;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.ui.NutritionProgressWrapper;

import static korablique.recipecalculator.IntentConstants.EDITED_FOODSTUFF_ID;
import static korablique.recipecalculator.IntentConstants.EDIT_FOODSTUFF_REQUEST;
import static korablique.recipecalculator.IntentConstants.EDIT_RESULT;
import static korablique.recipecalculator.ui.card.NewCard.EDITED_FOODSTUFF;

public class EditFoodstuffActivity extends BaseActivity {
    public static final String EDIT_FOODSTUFF_ACTION = "korablique.recipecalculator.EDIT_FOODSTUFF_ACTION";
    @Inject
    DatabaseWorker databaseWorker;
    private NutritionProgressWrapper nutritionProgressWrapper;
    private EditText foodstuffNameEditText;
    private EditText proteinEditText;
    private EditText fatsEditText;
    private EditText carbsEditText;
    private EditText caloriesEditText;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_foodstuff);

        TextView titleTextView = findViewById(R.id.title_text);
        titleTextView.setText(R.string.new_foodstuff);

        foodstuffNameEditText = findViewById(R.id.foodstuff_name);
        proteinEditText = findViewById(R.id.protein_value);
        fatsEditText = findViewById(R.id.fats_value);
        carbsEditText = findViewById(R.id.carbs_value);
        caloriesEditText = findViewById(R.id.calories_value);
        saveButton = findViewById(R.id.save_button);

        nutritionProgressWrapper = new NutritionProgressWrapper(this, findViewById(R.id.nutrition_progress_bar));
        nutritionProgressWrapper.setNutrition(Nutrition.zero());
        updateSaveButtonEnability();
        TextWatcher nutritionChangeWatcher = new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                double protein = parseNutrient(proteinEditText);
                double fats = parseNutrient(fatsEditText);
                double carbs = parseNutrient(carbsEditText);
                nutritionProgressWrapper.setNutrition(protein, fats, carbs);
            }
        };
        proteinEditText.addTextChangedListener(nutritionChangeWatcher);
        fatsEditText.addTextChangedListener(nutritionChangeWatcher);
        carbsEditText.addTextChangedListener(nutritionChangeWatcher);

        TextWatcher foodstuffInfoWatcher = new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                super.afterTextChanged(s);
                updateSaveButtonEnability();
            }
        };
        foodstuffNameEditText.addTextChangedListener(foodstuffInfoWatcher);
        proteinEditText.addTextChangedListener(foodstuffInfoWatcher);
        fatsEditText.addTextChangedListener(foodstuffInfoWatcher);
        carbsEditText.addTextChangedListener(foodstuffInfoWatcher);
        caloriesEditText.addTextChangedListener(foodstuffInfoWatcher);

        View cancelButton = findViewById(R.id.button_close);
        cancelButton.setOnClickListener(v -> {
            finish();
        });

        Intent receivedIntent = getIntent();
        if (EDIT_FOODSTUFF_ACTION.equals(receivedIntent.getAction())) {
            titleTextView.setText(R.string.change_foodstuff);
            Foodstuff editingFoodstuff = receivedIntent.getParcelableExtra(EDITED_FOODSTUFF);
            setDisplayingFoodstuff(editingFoodstuff);

            saveButton.setOnClickListener(v -> {
                Foodstuff editedFoodstuff = parseFoodstuff();
                long id = editingFoodstuff.getId();
                databaseWorker.editFoodstuff(EditFoodstuffActivity.this, id, editedFoodstuff);

                Intent intent = createEditingResultIntent(editedFoodstuff, id);
                setResult(RESULT_OK, intent);
                finish();
            });
        } else {
            saveButton.setOnClickListener(v -> {
                databaseWorker.saveFoodstuff(EditFoodstuffActivity.this, parseFoodstuff(), new DatabaseWorker.SaveFoodstuffCallback() {
                    @Override
                    public void onResult(long id) {
                        Toast.makeText(EditFoodstuffActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onDuplication() {
                        Toast.makeText(EditFoodstuffActivity.this, R.string.foodstuff_already_exists, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }

    public static void startForCreation(Context context) {
        Intent intent = new Intent(context, EditFoodstuffActivity.class);
        context.startActivity(intent);
    }

    public static void startForEditing(Activity context, Foodstuff foodstuff) {
        Intent intent = new Intent(context, EditFoodstuffActivity.class);
        intent.setAction(EDIT_FOODSTUFF_ACTION);
        intent.putExtra(EDITED_FOODSTUFF, foodstuff);
        context.startActivityForResult(intent, EDIT_FOODSTUFF_REQUEST);
    }

    public static Intent createEditingResultIntent(Foodstuff editedFoodstuff, long id) {
        Intent intent = new Intent();
        intent.putExtra(EDIT_RESULT, editedFoodstuff);
        intent.putExtra(EDITED_FOODSTUFF_ID, id);
        return intent;
    }

    private void setDisplayingFoodstuff(Foodstuff editingFoodstuff) {
        foodstuffNameEditText.setText(editingFoodstuff.getName());
        proteinEditText.setText(String.valueOf(editingFoodstuff.getProtein()));
        fatsEditText.setText(String.valueOf(editingFoodstuff.getFats()));
        carbsEditText.setText(String.valueOf(editingFoodstuff.getCarbs()));
        caloriesEditText.setText(String.valueOf(editingFoodstuff.getCalories()));
        nutritionProgressWrapper.setNutrition(Nutrition.of100gramsOf(editingFoodstuff));
    }

    private Foodstuff parseFoodstuff() {
        String foodstuffName = foodstuffNameEditText.getText().toString();
        double protein = Double.parseDouble(proteinEditText.getText().toString());
        double fats = Double.parseDouble(fatsEditText.getText().toString());
        double carbs = Double.parseDouble(carbsEditText.getText().toString());
        double calories = Double.parseDouble(caloriesEditText.getText().toString());
        return new Foodstuff(foodstuffName, -1, protein, fats, carbs, calories);
    }

    private double parseNutrient(EditText editText) {
        String valueString = editText.getText().toString();
        if (valueString.isEmpty()) {
            return 0.0;
        } else {
            return Double.parseDouble(valueString);
        }
    }

    private void updateSaveButtonEnability() {
        if (foodstuffNameEditText.getText().toString().isEmpty()
                || proteinEditText.getText().toString().isEmpty()
                || fatsEditText.getText().toString().isEmpty()
                || carbsEditText.getText().toString().isEmpty()
                || caloriesEditText.getText().toString().isEmpty()) {
            saveButton.setEnabled(false);
        } else {
            saveButton.setEnabled(true);
        }
    }
}
