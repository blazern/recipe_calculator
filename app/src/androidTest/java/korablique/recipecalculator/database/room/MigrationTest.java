package korablique.recipecalculator.database.room;

import android.content.Context;
import android.database.Cursor;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import korablique.recipecalculator.database.DatabaseUtils;
import korablique.recipecalculator.database.UserParametersContract;
import korablique.recipecalculator.database.room.legacy.LegacyUserNameProvider;
import korablique.recipecalculator.database.room.legacy.LegacyFullName;
import korablique.recipecalculator.util.FloatUtils;
import korablique.recipecalculator.util.TestingTimeProvider;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static korablique.recipecalculator.database.IngredientContract.INGREDIENT_TABLE_NAME;
import static korablique.recipecalculator.database.RecipeContract.RECIPE_TABLE_NAME;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_NAME;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_TARGET_WEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_USER_WEIGHT;
import static korablique.recipecalculator.database.UserParametersContract.USER_PARAMETERS_TABLE_NAME;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_2_3;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_3_4;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_4_5;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_5_6;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_6_7;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_7_8;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MigrationTest {
    public static final String TEST_DB = "migration-test";

    @Rule
    public MigrationTestHelper helper;

    public MigrationTest() {
        helper = new MigrationTestHelper(getInstrumentation(),
                AppDatabase.class.getCanonicalName(),
                new FrameworkSQLiteOpenHelperFactory());
    }

    @Test
    public void migrate2To3() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);

        // db has schema version 2. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.
        int id = 1, goalId = 1, genderId = 1, age = 25, height = 158, weight = 48, lifestyleId = 0, formulaId = 0;
        db.execSQL("INSERT INTO " + USER_PARAMETERS_TABLE_NAME + " VALUES (" +
                + id + ", " + goalId + ", " + genderId + ", " + age + ", " + height + ", "
                + weight + ", " + lifestyleId + ", " + formulaId + ")");

        // Prepare for the next version.
        db.close();

        // Re-open the database with version 3 and provide
        // MIGRATION_2_3 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3);

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        Cursor cursor = db.query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME);
        // убеждаемся, что таблица пуста, т к при миграции мы удаляем её и создаём заново,
        // не перенося старые данные
        Assert.assertTrue(!cursor.moveToFirst());
    }

    @Test
    public void migrate3To4() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 3);
        int id = 1, targetWeight = 45, genderId = 1, age = 25, height = 158, weight = 48, lifestyleId = 0, formulaId = 0;
        db.execSQL("INSERT INTO " + USER_PARAMETERS_TABLE_NAME + " VALUES (" +
                + id + ", " + targetWeight + ", " + genderId + ", " + age + ", " + height + ", "
                + weight + ", " + lifestyleId + ", " + formulaId + ")");
        db.close();

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4);
        Cursor cursor = db.query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME);
        while (cursor.moveToNext()) {
            float targetWeightFloat = cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_TARGET_WEIGHT));
            Assert.assertTrue(FloatUtils.areFloatsEquals(targetWeight, targetWeightFloat));

            float weightFloat = cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_USER_WEIGHT));
            Assert.assertTrue(FloatUtils.areFloatsEquals(weight, weightFloat));
        }
    }

    @Test
    public void migrate4To5() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 4);
        int id = 1, genderId = 1, age = 25, height = 158, lifestyleId = 0, formulaId = 0;
        float targetWeight = 45, weight = 47.5f;
        db.execSQL("INSERT INTO " + USER_PARAMETERS_TABLE_NAME + " VALUES (" +
                + id + ", " + targetWeight + ", " + genderId + ", " + age + ", " + height + ", "
                + weight + ", " + lifestyleId + ", " + formulaId + ")");
        db.close();

        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5);
        Cursor cursor = db.query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME);
        Assert.assertTrue(!cursor.moveToFirst());
    }

    @Test
    public void migrate5To6() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 5);
        int id = 1, genderId = 1, day = 1, month = 2, year = 1993, height = 158, lifestyleId = 0, formulaId = 0;
        float targetWeight = 45, weight = 47.5f;
        db.execSQL("INSERT INTO " + USER_PARAMETERS_TABLE_NAME + " VALUES (" +
                id + ", " +
                targetWeight + ", " +
                genderId + ", " +
                day + ", " +
                month + ", " +
                year + ", " +
                height + ", " +
                weight + ", " +
                lifestyleId + ", " +
                formulaId + ")");
        db.close();

        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6);
        Cursor cursor = db.query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME);
        Assert.assertTrue(!cursor.moveToFirst());
    }

    @Test
    public void migrate6To7() throws IOException {
        helper.createDatabase(TEST_DB, 6);
        SupportSQLiteDatabase db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7);
        Assert.assertTrue(DatabaseUtils.tableExists(db, RECIPE_TABLE_NAME));
        Assert.assertTrue(DatabaseUtils.tableExists(db, INGREDIENT_TABLE_NAME));
    }

    @Test
    public void migrate7To8() throws IOException {
        Context context = getInstrumentation().getTargetContext();
        LegacyUserNameProvider legacyUserNameProvider = new LegacyUserNameProvider(context);
        legacyUserNameProvider.saveUserName(new LegacyFullName("John", "Doe"));

        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 7);
        int id = 1, targetWeight = 65, genderId = 0, birthDay = 25, birthMonth = 8, birthYear = 1993,
                height = 165, weight = 63, lifestyleId = 1, formulaId = 0, measurementsTime = 123;
        db.execSQL("INSERT INTO " + USER_PARAMETERS_TABLE_NAME + " VALUES ("
                + id + ", " + targetWeight + ", " + genderId + ", " +
                birthDay + ", " + birthMonth + ", " + birthYear + ", " + height + ", " +
                weight + ", " + lifestyleId + ", " + formulaId + ", " + measurementsTime + ")");

        TestingTimeProvider timeProvider = new TestingTimeProvider(new DateTime(2020, 6, 7, 22, 16));

        db = helper.runMigrationsAndValidate(
                TEST_DB, 8, true, MIGRATION_7_8.call(context, timeProvider));

        Cursor c = db.query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME);
        assertEquals(1, c.getCount());
        while (c.moveToNext()) {
            String nameActual = c.getString(c.getColumnIndex(COLUMN_NAME_NAME));
            float targetWeightActual = c.getFloat(c.getColumnIndex(UserParametersContract.COLUMN_NAME_TARGET_WEIGHT));
            int genderActual = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_GENDER));
            int dayOfBirthActual = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_DAY_OF_BIRTH));
            int birthMonthActual = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_MONTH_OF_BIRTH));
            int birthYearActual = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_YEAR_OF_BIRTH));
            int heightActual = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_HEIGHT));
            float weightActual = c.getFloat(c.getColumnIndex(UserParametersContract.COLUMN_NAME_USER_WEIGHT));
            int lifestyleActual = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_LIFESTYLE));
            int formulaActual = c.getInt(c.getColumnIndex(UserParametersContract.COLUMN_NAME_FORMULA));
            long measurementTimeActual = c.getLong(c.getColumnIndex(UserParametersContract.COLUMN_NAME_MEASUREMENTS_TIMESTAMP));
            double proteinRatesActual = c.getDouble(c.getColumnIndex(UserParametersContract.COLUMN_NAME_RATE_PROTEIN));
            double fatsRatesActual = c.getDouble(c.getColumnIndex(UserParametersContract.COLUMN_NAME_RATE_FATS));
            double carbsRatesActual = c.getDouble(c.getColumnIndex(UserParametersContract.COLUMN_NAME_RATE_CARBS));
            double caloriesRatesActual = c.getDouble(c.getColumnIndex(UserParametersContract.COLUMN_NAME_RATE_CALORIES));

            assertEquals(legacyUserNameProvider.getUserName().toString(), nameActual);
            assertEquals(targetWeight, targetWeightActual, 0.0001);
            assertEquals(genderId, genderActual);
            assertEquals(birthYear, birthYearActual);
            assertEquals(birthMonth, birthMonthActual);
            assertEquals(birthDay, dayOfBirthActual);
            assertEquals(height, heightActual);
            assertEquals(weight, weightActual, 0.0001);
            assertEquals(lifestyleId, lifestyleActual);
            assertEquals(formulaId, formulaActual);
            assertEquals(measurementsTime, measurementTimeActual);
            assertEquals(126.0, proteinRatesActual, 0.00001);
            assertEquals(63.0, fatsRatesActual, 0.00001);
            assertEquals(328.4041748, carbsRatesActual, 0.00001);
            assertEquals(2384.6166992, caloriesRatesActual, 0.00001);
        }
    }
}
