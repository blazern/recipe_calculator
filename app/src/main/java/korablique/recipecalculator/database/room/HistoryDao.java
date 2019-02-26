package korablique.recipecalculator.database.room;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import korablique.recipecalculator.database.FoodstuffsContract;
import korablique.recipecalculator.database.HistoryContract;

import static korablique.recipecalculator.database.FoodstuffsContract.FOODSTUFFS_TABLE_NAME;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_DATE;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_FOODSTUFF_ID;
import static korablique.recipecalculator.database.HistoryContract.COLUMN_NAME_WEIGHT;
import static korablique.recipecalculator.database.HistoryContract.HISTORY_TABLE_NAME;
import static korablique.recipecalculator.database.HistoryContract.ID;

@Dao
public interface HistoryDao {
    @Insert
    List<Long> insertHistoryEntities(List<HistoryEntity> historyEntities);

    @Query("DELETE FROM " + HISTORY_TABLE_NAME + " WHERE " + ID + " = :historyEntityId")
    void deleteHistoryEntity(long historyEntityId);

    @Query("UPDATE " + HISTORY_TABLE_NAME + " SET " + COLUMN_NAME_WEIGHT +
            " = :weight WHERE " + ID + " = :historyId")
    void updateWeight(long historyId, double weight);

    @Query("UPDATE " + HISTORY_TABLE_NAME + " SET " + COLUMN_NAME_FOODSTUFF_ID +
            " = :foodstuffId WHERE " + ID + " = :historyId")
    void updateFoodstuff(long historyId, long foodstuffId);

    @Query("SELECT " + COLUMN_NAME_FOODSTUFF_ID + " FROM " + HISTORY_TABLE_NAME +
            " WHERE " + COLUMN_NAME_DATE + " >= :from AND " + COLUMN_NAME_DATE + " <= :to" +
            " ORDER BY " + COLUMN_NAME_DATE + " ASC")
    List<Long> loadFoodstuffsIdsForPeriod(long from, long to);

    @Query("SELECT * FROM " + HISTORY_TABLE_NAME + " LEFT OUTER JOIN " + FOODSTUFFS_TABLE_NAME +
            " ON " + HISTORY_TABLE_NAME + "." + COLUMN_NAME_FOODSTUFF_ID +
            "=" + FOODSTUFFS_TABLE_NAME + "." + FoodstuffsContract.ID + " ORDER BY " + COLUMN_NAME_DATE + " DESC")
    Cursor loadHistory();

    @Query("SELECT * FROM " + HISTORY_TABLE_NAME + " LEFT OUTER JOIN " + FOODSTUFFS_TABLE_NAME +
            " ON " + HISTORY_TABLE_NAME + "." + COLUMN_NAME_FOODSTUFF_ID +
            "=" + FOODSTUFFS_TABLE_NAME + "." + FoodstuffsContract.ID + " ORDER BY " + COLUMN_NAME_DATE + " DESC LIMIT :limit")
    Cursor loadHistoryWithLimit(int limit);
}
