package korablique.recipecalculator.base.prefs

import android.content.Context
import korablique.recipecalculator.ui.DecimalUtils
import javax.inject.Inject
import javax.inject.Singleton

private const val VALS_DELIMETER = ";"

/**
 * Обёртка над SharedPreferences для более удобного использования их в тестах
 * и для добавления удобных методов (например, с возможностью сохранять массивы чисел).
 */
@Singleton
class SharedPrefsManager @Inject constructor(val context: Context) {
    fun putLongList(owner: PrefsOwner, key: String, list: List<Long>) {
        val strList = list.map { it.toString() }
        putStringList(owner, key, strList)
    }

    fun getLongList(owner: PrefsOwner, key: String): List<Long>? {
        return getStringList(owner, key)?.map { it.toLong() }
    }

    fun putFloatList(owner: PrefsOwner, key: String, list: List<Float>, digitsAfterDot: Short) {
        val strList = list.map { DecimalUtils.toDecimalString(it, digitsAfterDot.toInt()) }
        putStringList(owner, key, strList)
    }

    fun getFloatList(owner: PrefsOwner, key: String): List<Float>? {
        return getStringList(owner, key)?.map { it.toFloat() }
    }

    private fun putStringList(owner: PrefsOwner, key: String, strList: List<String>) {
        context.getSharedPreferences(owner.fileName, Context.MODE_PRIVATE)
                .edit()
                .putString(key, strList.joinToString(separator = VALS_DELIMETER))
                .apply()
    }

    private fun getStringList(owner: PrefsOwner, key: String): List<String>? {
        val str = context
                .getSharedPreferences(owner.fileName, Context.MODE_PRIVATE)
                .getString(key, null)
        if (str == null || str.isEmpty()) {
            return null
        }
        return str.split(VALS_DELIMETER)
    }
}