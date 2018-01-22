package korablique.recipecalculator.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.ui.calculator.CalculatorActivity;
import korablique.recipecalculator.util.DbUtil;
import korablique.recipecalculator.util.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.util.InstantMainThreadExecutor;

import static korablique.recipecalculator.database.FoodstuffsContract.COLUMN_NAME_IS_LISTED;
import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_DATE;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_FOODSTUFF_ID;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_WEIGHT;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DatabaseWorkerTest {
    private DatabaseWorker databaseWorker;

    @Rule
    public ActivityTestRule<CalculatorActivity> mActivityRule =
            new ActivityTestRule<>(CalculatorActivity.class);

    @Before
    public void setUp() {
        databaseWorker = new DatabaseWorker(
                new InstantMainThreadExecutor(), new InstantDatabaseThreadExecutor());
        DbUtil.clearTable(mActivityRule.getActivity(), FOODSTUFFS_TABLE_NAME);
    }

    @Test
    public void requestListedFoodstuffsFromDbWorks() throws InterruptedException {
        Foodstuff foodstuff1 = new Foodstuff("продукт1", 1, 1, 1, 1, 1);
        Foodstuff foodstuff2 = new Foodstuff("продукт2", 1, 1, 1, 1, 1);
        Foodstuff foodstuff3 = new Foodstuff("продукт3", 1, 1, 1, 1, 1);
        Foodstuff foodstuff4 = new Foodstuff("продукт4", 1, 1, 1, 1, 1);
        databaseWorker.saveFoodstuff(
                mActivityRule.getActivity(),
                foodstuff1,
                new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {}
            @Override
            public void onDuplication() {
                throw new RuntimeException("Видимо, продукт уже существует");
            }
        });
        databaseWorker.saveFoodstuff(mActivityRule.getActivity(), foodstuff2, new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {}

            @Override
            public void onDuplication() {
                throw new RuntimeException("Видимо, продукт уже существует");
            }
        });
        //сохраняем два unlisted foodstuff'а
        databaseWorker.saveUnlistedFoodstuff(mActivityRule.getActivity(), foodstuff3, null);
        databaseWorker.saveUnlistedFoodstuff(mActivityRule.getActivity(), foodstuff4, null);

        final int[] listedFoodstuffsCount = new int[1];
        databaseWorker.requestListedFoodstuffsFromDb(
                mActivityRule.getActivity(),
                20,
                new DatabaseWorker.FoodstuffsRequestCallback() {
            @Override
            public void onResult(ArrayList<Foodstuff> foodstuffs) {
                listedFoodstuffsCount[0] = foodstuffs.size();
            }
        });

        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READONLY);
        Cursor unlistedFoodstuffs = database.rawQuery(
                "SELECT * FROM " + FOODSTUFFS_TABLE_NAME + " WHERE " + COLUMN_NAME_IS_LISTED + "=0", null);
        int unlistedFoodstuffsCount = unlistedFoodstuffs.getCount();
        unlistedFoodstuffs.close();

        Assert.assertEquals(2, unlistedFoodstuffsCount);
        Assert.assertEquals(2, listedFoodstuffsCount[0]);
    }

    @Test
    public void canSaveListedProductSameAsUnlisted() throws InterruptedException {
        Foodstuff foodstuff = new Foodstuff("falafel", -1, 10, 10, 10, 100);
        final long[] id = {-1};
        databaseWorker.saveUnlistedFoodstuff(
                mActivityRule.getActivity(),
                foodstuff,
                new DatabaseWorker.SaveUnlistedFoodstuffCallback() {
            @Override
            public void onResult(long foodstuffId) {
                id[0] = foodstuffId;
            }
        });

        databaseWorker.makeFoodstuffUnlisted(mActivityRule.getActivity(), id[0], null);

        final boolean[] containsListedFoodstuff = new boolean[1];
        databaseWorker.saveFoodstuff(
                mActivityRule.getActivity(),
                foodstuff,
                new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {
                containsListedFoodstuff[0] = false;
            }

            @Override
            public void onDuplication() {
                containsListedFoodstuff[0] = true;
            }
        });
        Assert.assertEquals(false, containsListedFoodstuff[0]);
    }

    @Test
    public void checkSaveGroupOfFoodstuffsCallbackCallsCount() throws InterruptedException {
        // создаем 10 продуктов
        int foodstuffsNumber = 10;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        for (int index = 0; index < foodstuffsNumber; index++) {
            foodstuffs[index] = new Foodstuff("foodstuff" + index, -1, 5, 5, 5, 5);
        }

        // сохраняем продукты в список
        final ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                mActivityRule.getActivity(),
                foodstuffs,
                new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
                    @Override
                    public void onResult(ArrayList<Long> ids) {
                        foodstuffsIds.addAll(ids);
                    }
                });
        Assert.assertEquals(foodstuffsNumber, foodstuffsIds.size());

        // Запрашиваем все фудстафы с размером батча 3
        int batchSize = 3;
        final int[] counter = {0};
        databaseWorker.requestListedFoodstuffsFromDb(
                mActivityRule.getActivity(),
                batchSize,
                new DatabaseWorker.FoodstuffsRequestCallback() {
                    @Override
                    public void onResult(ArrayList<Foodstuff> foodstuffs) {
                        ++counter[0];
                    }
                });
        Assert.assertEquals(4, counter[0]);
    }

    @Test
    public void checkReturnedFoodstuffs() throws InterruptedException {
        // создаем 10 продуктов
        int foodstuffsNumber = 10;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        for (int index = 0; index < foodstuffsNumber; index++) {
            foodstuffs[index] = new Foodstuff("foodstuff" + index, -1, 5, 5, 5, 5);
        }

        // сохраняем продукты в список
        final ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                mActivityRule.getActivity(),
                foodstuffs,
                new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
                    @Override
                    public void onResult(ArrayList<Long> ids) {
                        foodstuffsIds.addAll(ids);
                    }
                });
        Assert.assertEquals(foodstuffsNumber, foodstuffsIds.size());

        // Запрашиваем все фудстафы с размером батча 3
        int batchSize = 3;
        final ArrayList<Foodstuff> returnedFoodstuffs = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(
                mActivityRule.getActivity(),
                batchSize,
                new DatabaseWorker.FoodstuffsRequestCallback() {
                    @Override
                    public void onResult(ArrayList<Foodstuff> foodstuffs) {
                        returnedFoodstuffs.addAll(foodstuffs);
                    }
                });
        Collections.sort(foodstuffsIds);
        Collections.sort(returnedFoodstuffs, new Comparator<Foodstuff>() {
            @Override
            public int compare(Foodstuff lhs, Foodstuff rhs) {
                return Long.compare(lhs.getId(), rhs.getId());
            }
        });
        for (int index = 0; index < returnedFoodstuffs.size(); index++) {
            Assert.assertEquals(foodstuffsIds.get(index).longValue(), returnedFoodstuffs.get(index).getId());
        }
    }

    @Test
    public void listOfRequestedFoodstuffsReturnedInCorrectOrder() throws InterruptedException {
        // создаем продукт с названиями с маленькой и заглавной букв
        int foodstuffsNumber = 3;
        final Foodstuff[] foodstuffs = new Foodstuff[foodstuffsNumber];
        String apple = "Яблоко";
        foodstuffs[0] = new Foodstuff("абрикос", -1, 5, 5, 5, 5);
        foodstuffs[1] = new Foodstuff("Абрикос", -1, 5, 5, 5, 5);
        foodstuffs[2] = new Foodstuff(apple, -1, 5, 5, 5, 5);

        // сохраняем продукты в список
        final ArrayList<Long> foodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                mActivityRule.getActivity(),
                foodstuffs,
                new DatabaseWorker.SaveGroupOfFoodstuffsCallback() {
                    @Override
                    public void onResult(ArrayList<Long> ids) {
                        foodstuffsIds.addAll(ids);
                    }
                });
        Assert.assertEquals(foodstuffsNumber, foodstuffsIds.size());

        // Запрашиваем все фудстафы
        int batchSize = 4;
        final ArrayList<Foodstuff> returnedFoodstuffs = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(
                mActivityRule.getActivity(),
                batchSize,
                new DatabaseWorker.FoodstuffsRequestCallback() {
                    @Override
                    public void onResult(ArrayList<Foodstuff> foodstuffs) {
                        returnedFoodstuffs.addAll(foodstuffs);
                    }
                });
        Assert.assertEquals(apple, returnedFoodstuffs.get(2).getName());
    }

    public Foodstuff getAnyFoodstuffFromDb() throws InterruptedException {
        final ArrayList<Foodstuff> foodstuffArrayList = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(
                mActivityRule.getActivity(),
                20,
                new DatabaseWorker.FoodstuffsRequestCallback() {
            @Override
            public void onResult(ArrayList<Foodstuff> foodstuffs) {
                foodstuffArrayList.addAll(foodstuffs);
            }
        });

        if (foodstuffArrayList.size() != 0) {
            return foodstuffArrayList.get(0);
        }

        Foodstuff foodstuff = new Foodstuff("apricot", -1, 10, 10, 10, 10);
        databaseWorker.saveFoodstuff(mActivityRule.getActivity(), foodstuff, new DatabaseWorker.SaveFoodstuffCallback() {
            @Override
            public void onResult(long id) {}
            @Override
            public void onDuplication() {}
        });
        return getAnyFoodstuffFromDb();
    }
}
