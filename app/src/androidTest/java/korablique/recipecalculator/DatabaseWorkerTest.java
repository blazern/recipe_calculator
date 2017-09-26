package korablique.recipecalculator;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import static korablique.recipecalculator.HistoryContract.COLUMN_NAME_DATE;
import static korablique.recipecalculator.HistoryContract.COLUMN_NAME_FOODSTUFF_ID;
import static korablique.recipecalculator.HistoryContract.HISTORY_TABLE_NAME;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DatabaseWorkerTest {
    @Rule
    public ActivityTestRule<CalculatorActivity> mActivityRule =
            new ActivityTestRule<>(CalculatorActivity.class);

    @Test
    public void savingToHistoryWorks() throws InterruptedException {
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);

        database.delete(HISTORY_TABLE_NAME, null, null);
        Cursor cursorBeforeSaving = database.rawQuery("SELECT * FROM " + HISTORY_TABLE_NAME, null);
        int entriesCountBeforeSaving = cursorBeforeSaving.getCount();
        Assert.assertTrue(cursorBeforeSaving.getCount() == 0);
        cursorBeforeSaving.close();

        Foodstuff foodstuff = getAnyFoodstuffFromDb();

        final CountDownLatch mutex = new CountDownLatch(1);
        Runnable saveFinishCallback = new Runnable() {
            @Override
            public void run() {
                mutex.countDown();
            }
        };
        DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
        Date date = new Date();
        databaseWorker.saveFoodstuffFromListToHistory(mActivityRule.getActivity(), date, foodstuff.getId(), 100, saveFinishCallback);
        mutex.await();
        Cursor cursorAfterSaving = database.rawQuery("SELECT * FROM " + HISTORY_TABLE_NAME, null);
        long dateInt = -1, foodstuffId = -1;
        while (cursorAfterSaving.moveToNext()) {
            dateInt = cursorAfterSaving.getLong(cursorAfterSaving.getColumnIndex(COLUMN_NAME_DATE));
            foodstuffId = cursorAfterSaving.getLong(cursorAfterSaving.getColumnIndex(COLUMN_NAME_FOODSTUFF_ID));
        }
        int entriesCountAfterSaving = cursorAfterSaving.getCount();
        cursorAfterSaving.close();
        Assert.assertTrue(entriesCountAfterSaving - entriesCountBeforeSaving == 1);
        Assert.assertEquals(foodstuff.getId(), foodstuffId);
        Assert.assertEquals(date.getTime(), dateInt);
    }

    @Test
    public void requestAllHistoryFromDbWorks() throws InterruptedException {
        FoodstuffsDbHelper dbHelper = new FoodstuffsDbHelper(mActivityRule.getActivity());
        SQLiteDatabase database = dbHelper.openDatabase(SQLiteDatabase.OPEN_READWRITE);
        database.delete(HISTORY_TABLE_NAME, null, null);
        Cursor cursor = database.rawQuery("SELECT * FROM " + HISTORY_TABLE_NAME, null);
        Assert.assertTrue(cursor.getCount() == 0);
        cursor.close();

        final CountDownLatch mutex = new CountDownLatch(1);
        Runnable savingFinishCallback = new Runnable() {
            @Override
            public void run() {
                mutex.countDown();
            }
        };
        DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
        Foodstuff foodstuff = getAnyFoodstuffFromDb();
        double weight = 100;
        Date date = new Date();
        databaseWorker.saveFoodstuffFromListToHistory(
                mActivityRule.getActivity(), date, foodstuff.getId(), weight, savingFinishCallback);
        mutex.await();

        final CountDownLatch mutex1 = new CountDownLatch(1);
        final ArrayList<TimedFoodstuff> historyList = new ArrayList<>();
        databaseWorker.requestAllHistoryFromDb(mActivityRule.getActivity(), new DatabaseWorker.RequestHistoryCallback() {
            @Override
            public void onResult(ArrayList<TimedFoodstuff> timedFoodstuffs) {
                mutex1.countDown();
                historyList.addAll(timedFoodstuffs);
            }
        });
        mutex1.await();
        Assert.assertTrue(historyList.size() == 1);
        Assert.assertEquals(historyList.get(0).getFoodstuff().getId(), foodstuff.getId());
        Assert.assertEquals(historyList.get(0).getTime(), date);
        Assert.assertEquals(historyList.get(0).getWeight(), weight);
    }

    public Foodstuff getAnyFoodstuffFromDb() throws InterruptedException {
        final CountDownLatch mutex = new CountDownLatch(1);
        DatabaseWorker databaseWorker = DatabaseWorker.getInstance();
        final ArrayList<Foodstuff> foodstuffArrayList = new ArrayList<>();
        databaseWorker.requestAllFoodstuffsFromDb(mActivityRule.getActivity(), new DatabaseWorker.FoodstuffsRequestCallback() {
            @Override
            public void onResult(ArrayList<Foodstuff> foodstuffs) {
                foodstuffArrayList.addAll(foodstuffs);
                mutex.countDown();
            }
        });
        mutex.await();
        return foodstuffArrayList.get(0);
    }
}
