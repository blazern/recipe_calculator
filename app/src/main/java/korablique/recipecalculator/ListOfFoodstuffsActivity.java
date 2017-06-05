package korablique.recipecalculator;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FATS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.ID;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.TABLE_NAME;

public class ListOfFoodstuffsActivity extends AppCompatActivity {
    public static final String SEARCH_MESSAGE = "SEARCH_MESSAGE";
    private Card card;
    private FoodstuffsAdapter recyclerViewAdapter;
    private FoodstuffsAdapter.Observer adapterObserver = new FoodstuffsAdapter.Observer() {
        @Override
        public void onItemClicked(Foodstuff foodstuff, int position) {
            card.displayForFoodstuff(foodstuff, position);
        }

        @Override
        public void onItemsCountChanged(int count) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_foodstuffs);

        card = new Card(this, (ViewGroup)findViewById(R.id.list_of_recipes_parent));
        card.getButtonOk().setVisibility(View.GONE);
        card.getWeightEditText().setVisibility(View.GONE);
        card.getButtonSave().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!card.areAllEditTextsFull()) {
                    Toast.makeText(ListOfFoodstuffsActivity.this, "Заполните название и БЖУК", Toast.LENGTH_LONG).show();
                    return;
                }
                String newName = card.getNameEditText().getText().toString();
                double newProtein = Double.parseDouble(card.getProteinEditText().getText().toString());
                double newFats = Double.parseDouble(card.getFatsEditText().getText().toString());
                double newCarbs = Double.parseDouble(card.getCarbsEditText().getText().toString());
                double newCalories = Double.parseDouble(card.getCaloriesEditText().getText().toString());
                //сохраняем новые значения в базу данных
                long id = card.getEditedFoodstuff().getId();
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(ListOfFoodstuffsActivity.this);
                SQLiteDatabase database = dbHelper.getWritableDatabase();

                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_NAME_FOODSTUFF_NAME, newName);
                contentValues.put(COLUMN_NAME_PROTEIN, newProtein);
                contentValues.put(COLUMN_NAME_FATS, newFats);
                contentValues.put(COLUMN_NAME_CARBS, newCarbs);
                contentValues.put(COLUMN_NAME_CALORIES, newCalories);
                database.update(TABLE_NAME, contentValues, "id = ?", new String[]{ String.valueOf(id) });
                recyclerViewAdapter.notifyDataSetChanged();
                Toast.makeText(ListOfFoodstuffsActivity.this, "Изменения сохранены", Toast.LENGTH_SHORT).show();
                card.hide();
                KeyboardHandler keyboardHandler = new KeyboardHandler(ListOfFoodstuffsActivity.this);
                keyboardHandler.hideKeyBoard();

            }
        });
        card.getButtonDelete().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long id = card.getEditedFoodstuff().getId();
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(ListOfFoodstuffsActivity.this);
                SQLiteDatabase database = dbHelper.getWritableDatabase();
                database.delete(TABLE_NAME, "id = ?", new String[]{ String.valueOf(id) });
                recyclerViewAdapter.notifyDataSetChanged();
                recyclerViewAdapter.deleteItem(card.getEditedFoodstuffPosition());
                Toast.makeText(ListOfFoodstuffsActivity.this, "Продукт удалён", Toast.LENGTH_SHORT).show();
                card.hide();
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        recyclerViewAdapter = new FoodstuffsAdapter(adapterObserver);
        recyclerView.setAdapter(recyclerViewAdapter);

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        while (cursor.moveToNext()) {
            Foodstuff foodstuff = new Foodstuff(
                    cursor.getLong(cursor.getColumnIndex(ID)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME)),
                    -1,
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS)),
                    cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES)));
            recyclerViewAdapter.addItem(foodstuff);
        }
        cursor.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        // задаём слушатель запросов
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // запускаем поисковый intent
                Intent searchIntent = new Intent(Intent.ACTION_SEARCH);
                searchIntent.putExtra(SEARCH_MESSAGE, query);
                startActivity(searchIntent);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                //в этом случае не делаем ничего
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (card.isDisplayed()) {
            card.hide();
        } else {
            super.onBackPressed();
        }
    }
}
