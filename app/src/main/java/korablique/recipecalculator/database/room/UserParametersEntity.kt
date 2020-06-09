package korablique.recipecalculator.database.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import korablique.recipecalculator.database.UserParametersContract

@Entity(tableName = UserParametersContract.USER_PARAMETERS_TABLE_NAME)
class UserParametersEntity(
        @ColumnInfo(name = UserParametersContract.ID)
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_NAME)
        val name: String,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_TARGET_WEIGHT)
        val targetWeight: Float,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_GENDER)
        val genderId: Int,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_DAY_OF_BIRTH)
        val dayOfBirth: Int,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_MONTH_OF_BIRTH)
        val monthOfBirth: Int,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_YEAR_OF_BIRTH)
        val yearOfBirth: Int,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_HEIGHT)
        val height: Int,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_USER_WEIGHT)
        val weight: Float,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_LIFESTYLE)
        val lifestyleId: Int,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_FORMULA)
        val formulaId: Int,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_RATE_PROTEIN)
        val rateProtein: Double,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_RATE_FATS)
        val rateFats: Double,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_RATE_CARBS)
        val rateCarbs: Double,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_RATE_CALORIES)
        val rateCalories: Double,

        @ColumnInfo(name = UserParametersContract.COLUMN_NAME_MEASUREMENTS_TIMESTAMP)
        val measurementsTimestamp: Long)