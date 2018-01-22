package korablique.recipecalculator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;

import korablique.recipecalculator.base.MainThreadExecutor;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.UserParameters;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CALORIES;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_CARBS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FATS;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME_NOCASE;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_PROTEIN;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.FoodstuffsContract.ID;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_AGE;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_COEFFICIENT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_FORMULA;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_GENDER;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_GOAL;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_HEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_USER_WEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.USER_PARAMETERS_TABLE_NAME;

public class DatabaseWorker {
    private DatabaseThreadExecutor databaseThreadExecutor;
    private MainThreadExecutor mainThreadExecutor;

    public interface FoodstuffsRequestCallback {
        void onResult(ArrayList<Foodstuff> foodstuffs);
    }

    public interface SaveFoodstuffCallback {
        void onResult(long id);
        void onDuplication();
    }

    public interface SaveGroupOfFoodstuffsCallback {
        void onResult(ArrayList<Long> ids);
    }

    public interface SaveUnlistedFoodstuffCallback {
        void onResult(long foodstuffId);
    }

    public interface RequestCurrentUserParametersCallback {
        void onResult(UserParameters userParameters);
    }

    public DatabaseWorker(MainThreadExecutor mainThreadExecutor, DatabaseThreadExecutor databaseThreadExecutor) {
        this.mainThreadExecutor = mainThreadExecutor;
        this.databaseThreadExecutor = databaseThreadExecutor;
    }

    public void saveFoodstuff(
            final Context context,
            final Foodstuff foodstuff) {
        saveFoodstuff(context, foodstuff, null);
    }

    public void saveFoodstuff(
            final Context context,
            final Foodstuff foodstuff,
            final SaveFoodstuffCallback callback) {
        databaseThreadExecutor.execute(() -> {
            FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
            String whereClause = COLUMN_NAME_FOODSTUFF_NAME + "=? AND " +
                    COLUMN_NAME_PROTEIN + "=? AND " +
                    COLUMN_NAME_FATS + "=? AND " +
                    COLUMN_NAME_CARBS + "=? AND " +
                    COLUMN_NAME_CALORIES + "=? AND " +
                    COLUMN_NAME_IS_LISTED + "=?";
            String[] selectionArgs = new String[] {
                    String.valueOf(foodstuff.getName()),
                    String.valueOf(foodstuff.getProtein()),
                    String.valueOf(foodstuff.getFats()),
                    String.valueOf(foodstuff.getCarbs()),
                    String.valueOf(foodstuff.getCalories()),
                    String.valueOf(1)}; // listed
            Cursor cursor = database.query(
                    FOODSTUFFS_TABLE_NAME,
                    null,
                    whereClause,
                    selectionArgs,
                    null, null, null);
            //если такого продукта нет в БД:
            boolean alreadyContainsListedFoodstuff = false;
            long id = -1;
            if (cursor.getCount() == 0) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuff.getName());
                values.put(COLUMN_NAME_FOODSTUFF_NAME_NOCASE, foodstuff.getName().toLowerCase());
                values.put(COLUMN_NAME_PROTEIN, foodstuff.getProtein());
                values.put(COLUMN_NAME_FATS, foodstuff.getFats());
                values.put(COLUMN_NAME_CARBS, foodstuff.getCarbs());
                values.put(COLUMN_NAME_CALORIES, foodstuff.getCalories());
                id = database.insert(FOODSTUFFS_TABLE_NAME, null, values);
            } else {
                alreadyContainsListedFoodstuff = true;
            }
            cursor.close();

            if (callback != null) {
                if (alreadyContainsListedFoodstuff) {
                    mainThreadExecutor.execute(() -> callback.onDuplication());
                } else {
                    long finalId = id;
                    mainThreadExecutor.execute(() -> callback.onResult(finalId));
                }
            }
        });
    }

    /**
     * Порядок возвращаемых идентификаторов гарантированно идентичен порядку переданных фудстаффов.
     */
    public void saveGroupOfFoodstuffs(
            final Context context,
            final Foodstuff[] foodstuffs,
            @NonNull final SaveGroupOfFoodstuffsCallback callback) {
        databaseThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
                final ArrayList<Long> ids = new ArrayList<>();

                database.beginTransaction();
                try {
                    for (Foodstuff foodstuff : foodstuffs) {
                        ContentValues values = new ContentValues();
                        values.put(COLUMN_NAME_FOODSTUFF_NAME, foodstuff.getName());
                        values.put(COLUMN_NAME_FOODSTUFF_NAME_NOCASE, foodstuff.getName().toLowerCase());
                        values.put(COLUMN_NAME_PROTEIN, foodstuff.getProtein());
                        values.put(COLUMN_NAME_FATS, foodstuff.getFats());
                        values.put(COLUMN_NAME_CARBS, foodstuff.getCarbs());
                        values.put(COLUMN_NAME_CALORIES, foodstuff.getCalories());
                        long id = database.insert(FOODSTUFFS_TABLE_NAME, null, values);
                        ids.add(id);
                    }
                    database.setTransactionSuccessful();
                } finally {
                    database.endTransaction();
                }
                mainThreadExecutor.execute(() -> callback.onResult(ids));
            }
        });
    }

    public void saveUnlistedFoodstuff(
            final Context context,
            final Foodstuff foodstuff,
            @NonNull final SaveUnlistedFoodstuffCallback callback) {
        databaseThreadExecutor.execute(new Runnable() {
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
                if (callback != null) {
                    mainThreadExecutor.execute(() -> callback.onResult(foodstuffId));
                }
            }
        });
    }

    public void editFoodstuff(final Context context, final long editedFoodstuffId, final Foodstuff newFoodstuff) {
        databaseThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_NAME_FOODSTUFF_NAME, newFoodstuff.getName());
                contentValues.put(COLUMN_NAME_FOODSTUFF_NAME_NOCASE, newFoodstuff.getName().toLowerCase());
                contentValues.put(COLUMN_NAME_PROTEIN, newFoodstuff.getProtein());
                contentValues.put(COLUMN_NAME_FATS, newFoodstuff.getFats());
                contentValues.put(COLUMN_NAME_CARBS, newFoodstuff.getCarbs());
                contentValues.put(COLUMN_NAME_CALORIES, newFoodstuff.getCalories());
                database.update(
                        FOODSTUFFS_TABLE_NAME,
                        contentValues,
                        FoodstuffsContract.ID + " = ?",
                        new String[]{String.valueOf(editedFoodstuffId)});
            }
        });
    }

    public void deleteFoodstuff(final Context context, final long foodstuffsId) {
        databaseThreadExecutor.execute(() -> {
            FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
            database.delete(
                    FOODSTUFFS_TABLE_NAME,
                    FoodstuffsContract.ID + "=?",
                    new String[]{String.valueOf(foodstuffsId)});
        });
    }

    public void makeFoodstuffUnlisted(final Context context, final long foodstuffId, final Runnable callback) {
        databaseThreadExecutor.execute(() -> {
            FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_IS_LISTED, 0);
            database.update(
                    FOODSTUFFS_TABLE_NAME,
                    values,
                    FoodstuffsContract.ID + "=?",
                    new String[]{String.valueOf(foodstuffId)});
            if (callback != null) {
                mainThreadExecutor.execute(callback);
            }
        });
    }

    public void requestListedFoodstuffsFromDb(
            final Context context,
            final int batchSize,
            @NonNull final FoodstuffsRequestCallback callback) {
        databaseThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
                SQLiteDatabase db = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = db.query(
                        FOODSTUFFS_TABLE_NAME,
                        null,
                        COLUMN_NAME_IS_LISTED + "=?",
                        new String[]{ String.valueOf(1) },
                        null,
                        null,
                        COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " ASC");
                ArrayList<Foodstuff> batchOfFoodstuffs = new ArrayList<>();
                int index = 0;
                while (cursor.moveToNext()) {
                    Foodstuff foodstuff = new Foodstuff(
                            cursor.getLong(cursor.getColumnIndex(ID)),
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOODSTUFF_NAME)),
                            -1,
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_PROTEIN)),
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_FATS)),
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CARBS)),
                            cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_CALORIES)));
                    batchOfFoodstuffs.add(foodstuff);
                    ++index;
                    if (index >= batchSize) {
                        ArrayList<Foodstuff> batchCopy = new ArrayList<>(batchOfFoodstuffs);
                        mainThreadExecutor.execute(() -> callback.onResult(batchCopy));
                        batchOfFoodstuffs.clear();
                        index = 0;
                    }
                }
                if (batchOfFoodstuffs.size() > 0) {
                    mainThreadExecutor.execute(() -> callback.onResult(batchOfFoodstuffs));
                }
                cursor.close();
            }
        });
    }

    public void saveUserParameters(
            final Context context, final UserParameters userParameters, final Runnable callback) {
        databaseThreadExecutor.execute(() -> {
            FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_GOAL, userParameters.getGoal());
            values.put(COLUMN_NAME_GENDER, userParameters.getGender());
            values.put(COLUMN_NAME_AGE, userParameters.getAge());
            values.put(COLUMN_NAME_HEIGHT, userParameters.getHeight());
            values.put(COLUMN_NAME_USER_WEIGHT, userParameters.getWeight());
            values.put(COLUMN_NAME_COEFFICIENT, userParameters.getPhysicalActivityCoefficient());
            values.put(COLUMN_NAME_FORMULA, userParameters.getFormula());
            database.insert(USER_PARAMETERS_TABLE_NAME, null, values);
            if (callback != null) {
                mainThreadExecutor.execute(callback);
            }
        });
    }

    public void requestCurrentUserParameters(
            final Context context,
            final RequestCurrentUserParametersCallback callback) {
        databaseThreadExecutor.execute(() -> {
            FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(context);
            SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
            Cursor cursor = database.query(
                    USER_PARAMETERS_TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    UserParametersContract.ID + " DESC",
                    String.valueOf(1));
            UserParameters userParameters = null;
            if (cursor.getCount() != 0) {
                while (cursor.moveToNext()) {
                    String goal = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_GOAL));
                    String gender = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_GENDER));
                    int age = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_AGE));
                    int height = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_HEIGHT));
                    int weight = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_USER_WEIGHT));
                    float coefficient = cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_COEFFICIENT));
                    String formula = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FORMULA));
                    userParameters = new UserParameters(goal, gender, age, height, weight, coefficient, formula);
                }
            }
            cursor.close();
            UserParameters finalUserParameters = userParameters;
            mainThreadExecutor.execute(() -> callback.onResult(finalUserParameters));
        });
    }
}
