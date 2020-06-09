package korablique.recipecalculator.database.room;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.joda.time.LocalDate;
import org.joda.time.Period;

import korablique.recipecalculator.base.Function2arg;
import korablique.recipecalculator.base.TimeProvider;
import korablique.recipecalculator.database.FoodstuffsContract;
import korablique.recipecalculator.database.HistoryContract;
import korablique.recipecalculator.database.IngredientContract;
import korablique.recipecalculator.database.room.legacy.LegacyDatabaseValues;
import korablique.recipecalculator.database.RecipeContract;
import korablique.recipecalculator.database.UserParametersContract;
import korablique.recipecalculator.database.room.legacy.LegacyDatabaseUpdater;
import korablique.recipecalculator.database.room.legacy.LegacyUserNameProvider;
import korablique.recipecalculator.database.room.legacy.LegacyFullName;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.RateCalculator;

import static korablique.recipecalculator.database.IngredientContract.INGREDIENT_TABLE_NAME;
import static korablique.recipecalculator.database.RecipeContract.RECIPE_TABLE_NAME;
import static korablique.recipecalculator.database.UserParametersContract.USER_PARAMETERS_TABLE_NAME;

public class Migrations {
    private Migrations() {}

    @VisibleForTesting
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            LegacyDatabaseUpdater.upgradeIfNeeded(database);

            // Add NOT NULL to foodstuffs table
            database.execSQL(
                    "CREATE TABLE foodstuffs_tmp(" +
                            FoodstuffsContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME + " TEXT NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_FOODSTUFF_NAME_NOCASE + " TEXT NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_PROTEIN + " REAL NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_FATS + " REAL NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_CARBS + " REAL NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_CALORIES + " REAL NOT NULL," +
                            FoodstuffsContract.COLUMN_NAME_IS_LISTED + " INTEGER DEFAULT 1 NOT NULL)");
            replaceTable(database, "foodstuffs_tmp", FoodstuffsContract.FOODSTUFFS_TABLE_NAME);

            // Add NOT NULL to history table
            database.execSQL(
                    "CREATE TABLE history_tmp (" +
                            HistoryContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            HistoryContract.COLUMN_NAME_DATE + " INTEGER NOT NULL, " +
                            HistoryContract.COLUMN_NAME_FOODSTUFF_ID + " INTEGER NOT NULL, " +
                            HistoryContract.COLUMN_NAME_WEIGHT + " REAL NOT NULL, " +
                            "FOREIGN KEY (" + HistoryContract.COLUMN_NAME_FOODSTUFF_ID + ") " +
                            "REFERENCES " + FoodstuffsContract.FOODSTUFFS_TABLE_NAME + "(" + FoodstuffsContract.ID + "))");
            replaceTable(database, "history_tmp", HistoryContract.HISTORY_TABLE_NAME);

            // Add not null to user parameters table
            database.execSQL(
                    "CREATE TABLE user_params_tmp (" +
                            UserParametersContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            LegacyDatabaseValues.COLUMN_NAME_GOAL + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_GENDER + " INTEGER NOT NULL, " +
                            UserParametersContract.DEPRECATED_COLUMN_NAME_AGE + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_HEIGHT + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_USER_WEIGHT + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_LIFESTYLE + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_FORMULA + " INTEGER NOT NULL)");
            replaceTable(database, "user_params_tmp", USER_PARAMETERS_TABLE_NAME);

            // Drop versions table - Room now controls DB versioning
            database.execSQL("DROP TABLE " + LegacyDatabaseUpdater.TABLE_DATABASE_VERSION);

            // Create an index manually - Room expects the index since it's declare in the Entity.
            database.execSQL("CREATE INDEX index_history_foodstuff_id ON history(foodstuff_id)");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // удаляем старую таблицу и создаём новую с желаемым весом вместо цели,
            // т.к. цель (похудеть/набрать) нельзя однозначно конвертировать в вес
            database.execSQL("DROP TABLE " + USER_PARAMETERS_TABLE_NAME);
            database.execSQL(
                    "CREATE TABLE " + USER_PARAMETERS_TABLE_NAME +" (" +
                            UserParametersContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_TARGET_WEIGHT + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_GENDER + " INTEGER NOT NULL, " +
                            UserParametersContract.DEPRECATED_COLUMN_NAME_AGE + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_HEIGHT + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_USER_WEIGHT + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_LIFESTYLE + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_FORMULA + " INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // store weight and target weight in float (REAL)
            String tmpTableName = "user_params_tmp";
            database.execSQL(
                    "CREATE TABLE " + tmpTableName +" (" +
                            UserParametersContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_TARGET_WEIGHT + " REAL NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_GENDER + " INTEGER NOT NULL, " +
                            UserParametersContract.DEPRECATED_COLUMN_NAME_AGE + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_HEIGHT + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_USER_WEIGHT + " REAL NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_LIFESTYLE + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_FORMULA + " INTEGER NOT NULL)");
            replaceTable(database, tmpTableName, USER_PARAMETERS_TABLE_NAME);
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // store date of birth as string instead of age
            database.execSQL("DROP TABLE " + USER_PARAMETERS_TABLE_NAME);
            database.execSQL(
                    "CREATE TABLE " + USER_PARAMETERS_TABLE_NAME +" (" +
                            UserParametersContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_TARGET_WEIGHT + " REAL NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_GENDER + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_DAY_OF_BIRTH + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_MONTH_OF_BIRTH + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_YEAR_OF_BIRTH + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_HEIGHT + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_USER_WEIGHT + " REAL NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_LIFESTYLE + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_FORMULA + " INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // store date (timestamp) when user made his measurements
            database.execSQL("DROP TABLE " + USER_PARAMETERS_TABLE_NAME);
            database.execSQL(
                    "CREATE TABLE " + USER_PARAMETERS_TABLE_NAME +" (" +
                            UserParametersContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_TARGET_WEIGHT + " REAL NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_GENDER + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_DAY_OF_BIRTH + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_MONTH_OF_BIRTH + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_YEAR_OF_BIRTH + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_HEIGHT + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_USER_WEIGHT + " REAL NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_LIFESTYLE + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_FORMULA + " INTEGER NOT NULL," +
                            UserParametersContract.COLUMN_NAME_MEASUREMENTS_TIMESTAMP + " INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // add table recipe
            database.execSQL(
                    "CREATE TABLE " + RECIPE_TABLE_NAME + " (" +
                            RecipeContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            RecipeContract.COLUMN_NAME_FOODSTUFF_ID + " INTEGER NOT NULL, " +
                            RecipeContract.COLUMN_NAME_INGREDIENTS_TOTAL_WEIGHT + " REAL NOT NULL, " +
                            RecipeContract.COLUMN_NAME_COMMENT + " TEXT NOT NULL, " +
                            "FOREIGN KEY (" + RecipeContract.COLUMN_NAME_FOODSTUFF_ID + ") " +
                            "REFERENCES " + FoodstuffsContract.FOODSTUFFS_TABLE_NAME + "(" + FoodstuffsContract.ID + ")," +
                            "UNIQUE (" + RecipeContract.COLUMN_NAME_FOODSTUFF_ID + "))");
            database.execSQL("CREATE INDEX "
                    + "index_" + RECIPE_TABLE_NAME + "_" + RecipeContract.COLUMN_NAME_FOODSTUFF_ID
                    + " ON " + RECIPE_TABLE_NAME
                    + "(" + RecipeContract.COLUMN_NAME_FOODSTUFF_ID + ")");
            // add table ingredient
            database.execSQL(
                    "CREATE TABLE " + INGREDIENT_TABLE_NAME + " (" +
                            IngredientContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            IngredientContract.COLUMN_NAME_RECIPE_ID + " INTEGER NOT NULL, " +
                            IngredientContract.COLUMN_NAME_INGREDIENT_WEIGHT + " REAL NOT NULL, " +
                            IngredientContract.COLUMN_NAME_INGREDIENT_FOODSTUFF_ID + " INTEGER NOT NULL, " +
                            IngredientContract.COLUMN_NAME_COMMENT + " TEXT NOT NULL, " +
                            IngredientContract.COLUMN_INDEX + " INTEGER NOT NULL, " +
                            "FOREIGN KEY (" + IngredientContract.COLUMN_NAME_RECIPE_ID + ") " +
                            "REFERENCES " + RECIPE_TABLE_NAME + "(" + RecipeContract.ID + "), " +
                            "FOREIGN KEY (" + IngredientContract.COLUMN_NAME_INGREDIENT_FOODSTUFF_ID + ") " +
                            "REFERENCES " + FoodstuffsContract.FOODSTUFFS_TABLE_NAME + "(" + FoodstuffsContract.ID + "))");
            database.execSQL("CREATE INDEX "
                    + "index_" + INGREDIENT_TABLE_NAME + "_" + IngredientContract.COLUMN_NAME_RECIPE_ID
                    + " ON " + INGREDIENT_TABLE_NAME
                    + "(" + IngredientContract.COLUMN_NAME_RECIPE_ID + ")");
            database.execSQL("CREATE INDEX "
                    + "index_" + INGREDIENT_TABLE_NAME + "_" + IngredientContract.COLUMN_NAME_INGREDIENT_FOODSTUFF_ID
                    + " ON " + INGREDIENT_TABLE_NAME
                    + "(" + IngredientContract.COLUMN_NAME_INGREDIENT_FOODSTUFF_ID + ")");
        }
    };

    static final Function2arg<Migration, Context, TimeProvider> MIGRATION_7_8 =
            (context, timeProvider) -> new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add name and rates columns

            String tmpTableName = USER_PARAMETERS_TABLE_NAME + "tmp";
            database.execSQL(
                    "CREATE TABLE " + tmpTableName +" (" +
                            UserParametersContract.ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_NAME + " TEXT NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_TARGET_WEIGHT + " REAL NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_GENDER + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_DAY_OF_BIRTH + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_MONTH_OF_BIRTH + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_YEAR_OF_BIRTH + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_HEIGHT + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_USER_WEIGHT + " REAL NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_LIFESTYLE + " INTEGER NOT NULL, " +
                            UserParametersContract.COLUMN_NAME_FORMULA + " INTEGER NOT NULL," +
                            UserParametersContract.COLUMN_NAME_RATE_PROTEIN + " REAL NOT NULL," +
                            UserParametersContract.COLUMN_NAME_RATE_FATS + " REAL NOT NULL," +
                            UserParametersContract.COLUMN_NAME_RATE_CARBS + " REAL NOT NULL," +
                            UserParametersContract.COLUMN_NAME_RATE_CALORIES + " REAL NOT NULL," +
                            UserParametersContract.COLUMN_NAME_MEASUREMENTS_TIMESTAMP + " INTEGER NOT NULL)");

            LegacyUserNameProvider legacyUserNameProvider = new LegacyUserNameProvider(context);
            LegacyFullName name = legacyUserNameProvider.getUserName();

            LocalDate now = timeProvider.now().toLocalDate();

            Cursor c = database.query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME);
            while (c.moveToNext()) {
                float targetWeight = c.getFloat(c.getColumnIndex(UserParametersContract.COLUMN_NAME_TARGET_WEIGHT));
                int gender = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_GENDER));
                int birthYear = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_YEAR_OF_BIRTH));
                int birthMonth = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_MONTH_OF_BIRTH));
                int dayOfBirth = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_DAY_OF_BIRTH));
                int height = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_HEIGHT));
                float weight = c.getFloat(c.getColumnIndex(UserParametersContract.COLUMN_NAME_USER_WEIGHT));
                int lifestyle = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_LIFESTYLE));
                int formula = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_FORMULA));
                long measurementTime = c.getLong(c.getColumnIndex(UserParametersContract.COLUMN_NAME_MEASUREMENTS_TIMESTAMP));

                LocalDate birthday = new LocalDate(birthYear, birthMonth, dayOfBirth);
                int age = new Period(birthday, now).getYears();
                Nutrition rates = RateCalculator.calculate(
                        targetWeight, Gender.fromId(gender), age, height,
                        weight, Lifestyle.fromId(lifestyle), Formula.fromId(formula));

                ContentValues values = new ContentValues();
                values.put(UserParametersContract.COLUMN_NAME_NAME, name.toString());
                values.put(UserParametersContract.COLUMN_NAME_TARGET_WEIGHT, targetWeight);
                values.put(UserParametersContract.COLUMN_NAME_GENDER, gender);
                values.put(UserParametersContract.COLUMN_NAME_DAY_OF_BIRTH, dayOfBirth);
                values.put(UserParametersContract.COLUMN_NAME_MONTH_OF_BIRTH, birthMonth);
                values.put(UserParametersContract.COLUMN_NAME_YEAR_OF_BIRTH, birthYear);
                values.put(UserParametersContract.COLUMN_NAME_HEIGHT, height);
                values.put(UserParametersContract.COLUMN_NAME_USER_WEIGHT, weight);
                values.put(UserParametersContract.COLUMN_NAME_LIFESTYLE, lifestyle);
                values.put(UserParametersContract.COLUMN_NAME_FORMULA, formula);
                values.put(UserParametersContract.COLUMN_NAME_MEASUREMENTS_TIMESTAMP, measurementTime);
                values.put(UserParametersContract.COLUMN_NAME_RATE_PROTEIN, rates.getProtein());
                values.put(UserParametersContract.COLUMN_NAME_RATE_FATS, rates.getFats());
                values.put(UserParametersContract.COLUMN_NAME_RATE_CARBS, rates.getCarbs());
                values.put(UserParametersContract.COLUMN_NAME_RATE_CALORIES, rates.getCalories());
                database.insert(tmpTableName, SQLiteDatabase.CONFLICT_NONE, values);
            }

            database.execSQL("DROP TABLE " + USER_PARAMETERS_TABLE_NAME);
            database.execSQL("ALTER TABLE " + tmpTableName + " RENAME TO " + USER_PARAMETERS_TABLE_NAME);
        }
    };

    private static void replaceTable(
            SupportSQLiteDatabase database, String tmpTableName, String targetTableName) {
        database.execSQL("INSERT INTO " + tmpTableName + " SELECT * FROM " + targetTableName);
        database.execSQL("DROP TABLE " + targetTableName);
        database.execSQL("ALTER TABLE " + tmpTableName + " RENAME TO " + targetTableName);
    }
}
