package korablique.recipecalculator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static korablique.recipecalculator.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.FoodstuffsContract.ID;
import static korablique.recipecalculator.HistoryContract.COLUMN_NAME_DATE;
import static korablique.recipecalculator.HistoryContract.COLUMN_NAME_FOODSTUFF_ID;
import static korablique.recipecalculator.HistoryContract.COLUMN_NAME_WEIGHT;
import static korablique.recipecalculator.HistoryContract.HISTORY_TABLE_NAME;

public class DatabaseWorker {
    private static DatabaseWorker databaseWorker;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    public interface FoodstuffsRequestCallback {
        void onResult(ArrayList<Foodstuff> foodstuffs);
    }
    public interface SaveFoodstuffCallback {
        void onResult(boolean hasAlreadyContainsFoodstuff);
    }
    public interface RequestHistoryCallback {
        void onResult(ArrayList<TimedFoodstuff> timedFoodstuffs);
    }
    public interface SaveUnlistedFoodstuffCallback {
        void onResult(long foodstuffId);
    }

    private DatabaseWorker() {}

    public static synchronized DatabaseWorker getInstance() {
        if (databaseWorker == null) {
            databaseWorker = new DatabaseWorker();
        }
        return databaseWorker;
    }

    public void saveFoodstuff(final Context context, final Foodstuff foodstuff, @NonNull final SaveFoodstuffCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                Cursor cursor = database.rawQuery("SELECT * FROM " + FOODSTUFFS_TABLE_NAME
                        + " WHERE " + COLUMN_NAME_FOODSTUFF_NAME + " = '" + foodstuff.getName() + "' AND "
                        + COLUMN_NAME_PROTEIN + " = " + foodstuff.getProtein() + " AND "
                        + COLUMN_NAME_FATS + " = " + foodstuff.getFats() + " AND "
                        + COLUMN_NAME_CARBS + " = " + foodstuff.getCarbs() + " AND "
                        + COLUMN_NAME_CALORIES + " = " + foodstuff.getCalories() + ";", null);
                //если такого продукта нет в БД:
                boolean hasAlreadyContainsFoodstuff = false;
                if (cursor.getCount() == 0) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuff.getName());
                    values.put(COLUMN_NAME_PROTEIN, foodstuff.getProtein());
                    values.put(COLUMN_NAME_FATS, foodstuff.getFats());
                    values.put(COLUMN_NAME_CARBS, foodstuff.getCarbs());
                    values.put(COLUMN_NAME_CALORIES, foodstuff.getCalories());
                    database.insert(FOODSTUFFS_TABLE_NAME, null, values);
                } else {
                    hasAlreadyContainsFoodstuff = true;
                }
                cursor.close();
                callback.onResult(hasAlreadyContainsFoodstuff);
            }
        });
    }

    public void saveUnlistedFoodstuff(
            final Context context,
            final Foodstuff foodstuff,
            @NonNull final SaveUnlistedFoodstuffCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuff.getName());
                values.put(COLUMN_NAME_PROTEIN, foodstuff.getProtein());
                values.put(COLUMN_NAME_FATS, foodstuff.getFats());
                values.put(COLUMN_NAME_CARBS, foodstuff.getCarbs());
                values.put(COLUMN_NAME_CALORIES, foodstuff.getCalories());
                values.put(COLUMN_NAME_IS_LISTED, 0);
                long foodstuffId = database.insert(FOODSTUFFS_TABLE_NAME, null, values);
                callback.onResult(foodstuffId);
            }
        });
    }

    public void editFoodstuff(final Context context, final long editedFoodstuffId, final Foodstuff newFoodstuff) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_NAME_FOODSTUFF_NAME, newFoodstuff.getName());
                contentValues.put(COLUMN_NAME_PROTEIN, newFoodstuff.getProtein());
                contentValues.put(COLUMN_NAME_FATS, newFoodstuff.getFats());
                contentValues.put(COLUMN_NAME_CARBS, newFoodstuff.getCarbs());
                contentValues.put(COLUMN_NAME_CALORIES, newFoodstuff.getCalories());
                database.update(FOODSTUFFS_TABLE_NAME, contentValues, "id = ?", new String[]{String.valueOf(editedFoodstuffId)});
            }
        });
    }

    public void deleteFoodstuff(final Context context, final long foodstuffsId) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                database.delete(FOODSTUFFS_TABLE_NAME, "id = ?", new String[]{String.valueOf(foodstuffsId)});
            }
        });
    }

    public void requestListedFoodstuffsFromDb(final Context context, @NonNull final FoodstuffsRequestCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase db = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = db.rawQuery(
                        "SELECT * FROM " + FOODSTUFFS_TABLE_NAME + " WHERE " + COLUMN_NAME_IS_LISTED + "=1", null);
                ArrayList<Foodstuff> allFoodstuffsFromDb = new ArrayList<>();
                while (cursor.moveToNext()) {
                    Foodstuff foodstuff = new Foodstuff(
                            cursor.getLong(cursor.getColumnIndex(ID)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME)),
                            -1,
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN)),
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS)),
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS)),
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES)));
                    allFoodstuffsFromDb.add(foodstuff);
                }
                cursor.close();
                Collections.sort(allFoodstuffsFromDb);
                callback.onResult(allFoodstuffsFromDb);
            }
        });
    }

    public void requestAllHistoryFromDb(final Context context, @NonNull final RequestHistoryCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase db = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = db.rawQuery("SELECT * FROM " + HISTORY_TABLE_NAME + " JOIN " + FOODSTUFFS_TABLE_NAME
                        + " ON " + HISTORY_TABLE_NAME + "." + COLUMN_NAME_FOODSTUFF_ID
                        + "=" + FOODSTUFFS_TABLE_NAME + "." + FoodstuffsContract.ID, null);
                ArrayList<TimedFoodstuff> timedFoodstuffs = new ArrayList<>();
                while (cursor.moveToNext()) {
                    long foodstuffId = cursor.getLong(
                            cursor.getColumnIndex(HISTORY_TABLE_NAME + "." + COLUMN_NAME_FOODSTUFF_ID));
                    double weight = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_WEIGHT));
                    String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME));
                    double protein = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN));
                    double fats = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS));
                    double carbs = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS));
                    double calories = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES));
                    Foodstuff foodstuff = new Foodstuff(foodstuffId, name, weight, protein, fats, carbs, calories);

                    long time = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_DATE));
                    long historyId = cursor.getLong(cursor.getColumnIndex(HISTORY_TABLE_NAME + "." + HistoryContract.ID));
                    TimedFoodstuff timedFoodstuff = new TimedFoodstuff(historyId, foodstuff, new Date(time), weight);
                    timedFoodstuffs.add(timedFoodstuff);
                }
                cursor.close();
                callback.onResult(timedFoodstuffs);
            }
        });
    }

    public void saveFoodstuffToHistory(
            final Context context,
            final Date date,
            final long foodstuffId,
            final double foodstuffWeight,
            final Runnable callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME_DATE, date.getTime());
                values.put(COLUMN_NAME_FOODSTUFF_ID, foodstuffId);
                values.put(COLUMN_NAME_WEIGHT, foodstuffWeight);
                database.insert(HISTORY_TABLE_NAME, null, values);
                if (callback != null) {
                    callback.run();
                }
            }
        });
    }

    public void saveFoodstuffToHistory(
            final Context context,
            final Date date,
            final long foodstuffId,
            double foodstuffWeight) {
        saveFoodstuffToHistory(context, date, foodstuffId, foodstuffWeight, null);
    }
}
