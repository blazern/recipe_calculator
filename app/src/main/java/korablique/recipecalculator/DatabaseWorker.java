package korablique.recipecalculator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FATS;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.ID;
import static korablique.recipecalculator.FoodstuffsContract.Foodstuffs.TABLE_NAME;

public class DatabaseWorker {
    private static DatabaseWorker databaseWorker;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    public interface FoodstuffsRequestCallback {
        void onResult(ArrayList<Foodstuff> foodstuffs);
    }
    public interface SaveFoodstuffCallback {
        void onResult(boolean hasAlreadyContainsFoodstuff);
    }

    private DatabaseWorker() {}

    public static synchronized DatabaseWorker getInstance() {
        if (databaseWorker == null) {
            databaseWorker = new DatabaseWorker();
        }
        return databaseWorker;
    }

    private void runInBackground(final Runnable runnable) {
        // Executor'ы (в т.ч. Executors.newSingleThreadExecutor) ловят исключения, выброшенные
        // переданными в них Runnable'ами, и ничего с ними не делают, пока не будет вызван
        // Future.get (Executor.submit возвращает Future).
        //
        // Мы никогда не вызываем Future.get (потому что он блокирует вызывающий поток) -
        // а значит исключения втихую проглатываются.
        //
        // Чтобы запретить проглатывание исключений, передадим в Executor не конечный Runnable,
        // а свой, промежуточный. Внутри промежуточного Runnable обернём конечный в try-catch,
        // и т.о. сами поймаем исключение, которое было бы молча проглочено Executor'ом.
        //
        // Далее переместим исключение на главный поток и выбросим оттуда, чтобы приложение закрешилось.
        Runnable realRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (final Throwable e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        };
        executorService.submit(realRunnable);
    }

    public void saveFoodstuff(final Context context, final Foodstuff foodstuff, final SaveFoodstuffCallback callback) {
        runInBackground(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE_NAME
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
                    database.insert(TABLE_NAME, null, values);
                } else {
                    hasAlreadyContainsFoodstuff = true;
                }
                cursor.close();
                callback.onResult(hasAlreadyContainsFoodstuff);
            }
        });
    }

    public void editFoodstuff(final Context context, final long editedFoodstuffId, final Foodstuff newFoodstuff) {
        runInBackground(new Runnable() {
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
                database.update(TABLE_NAME, contentValues, "id = ?", new String[]{String.valueOf(editedFoodstuffId)});
            }
        });
    }

    public void deleteFoodstuff(final Context context, final long foodstuffsId) {
        runInBackground(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                database.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(foodstuffsId)});
            }
        });
    }

    public void requestAllFoodstuffsFromDb(final Context context, final FoodstuffsRequestCallback callback) {
        runInBackground(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase db = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
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
}
