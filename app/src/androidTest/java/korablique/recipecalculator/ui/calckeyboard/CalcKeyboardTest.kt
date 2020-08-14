package korablique.recipecalculator.ui.calckeyboard

import android.content.Context
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import korablique.recipecalculator.R
import korablique.recipecalculator.base.CurrentActivityProvider
import korablique.recipecalculator.base.prefs.PrefsCleaningHelper
import korablique.recipecalculator.base.prefs.PrefsOwner
import korablique.recipecalculator.base.prefs.SharedPrefsManager
import korablique.recipecalculator.test.CalcKeyboardTestActivity
import korablique.recipecalculator.util.EspressoUtils.isNotDisplayed
import korablique.recipecalculator.util.FloatUtils
import korablique.recipecalculator.util.InjectableActivityTestRule
import korablique.recipecalculator.util.SyncMainThreadExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CalcKeyboardTest {
    private lateinit var context: Context
    private val mainThreadExecutor = SyncMainThreadExecutor()
    private lateinit var prefsManager: SharedPrefsManager

    @get:Rule
    val activityRule: ActivityTestRule<CalcKeyboardTestActivity> =
            InjectableActivityTestRule.forActivity(CalcKeyboardTestActivity::class.java)
            .withManualStart()
            .withSingletones {
                context = InstrumentationRegistry.getTargetContext()
                PrefsCleaningHelper.cleanAllPrefs(context)
                prefsManager = SharedPrefsManager(context)
                val currentActivityProvider = CurrentActivityProvider()
                val calcKeyboardController = CalcKeyboardController(context, prefsManager)
                listOf(calcKeyboardController, currentActivityProvider)
            }
            .build()

    @Test
    fun calcKeyboardAppearsWhenCalcEditTextIsFocused() {
        activityRule.launchActivity(null)
        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
        onView(withId(R.id.calc_edit_text)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(matches(isDisplayed()))
    }

    @Test
    fun calcKeyboardDoesNotAppearsWhenPreferenceSaysSo() {
        prefsManager.putBool(
                PrefsOwner.NO_OWNER,
                context.getString(R.string.preference_key_calc_keyboard_enabled),
                false)

        activityRule.launchActivity(null)

        onView(withId(R.id.calc_edit_text)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
    }

    @Test
    fun calcKeyboardDoesNotAppearsWhenNormalEditTextIsFocused() {
        activityRule.launchActivity(null)
        onView(withId(R.id.normal_edit_text)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
    }

    @Test
    fun calcKeyboardShownWhenFocusMovesFromNormalToCalcEditText() {
        activityRule.launchActivity(null)
        onView(withId(R.id.normal_edit_text)).perform(click())

        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
        onView(withId(R.id.calc_edit_text)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(matches(isDisplayed()))
    }

    @Test
    fun calcKeyboardHiddenWhenFocusMovesFromCalcToNormalEditText() {
        activityRule.launchActivity(null)
        onView(withId(R.id.calc_edit_text)).perform(click())

        onView(withId(R.id.calc_keyboard)).check(matches(isDisplayed()))
        onView(withId(R.id.normal_edit_text)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
    }

    @Test
    fun canWriteAndEraseTextWithCalcKeyboard() {
        activityRule.launchActivity(null)

        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_point)).perform(click())
        onView(withId(R.id.button_9)).perform(click())

        // Проверяем, что текст введен
        onView(withId(R.id.calc_edit_text)).check(matches(withText("12.9")))

        // Стираем символы
        onView(withId(R.id.button_delete)).perform(click())
        onView(withId(R.id.button_delete)).perform(click())

        // Проверяем, что символы стерлись
        onView(withId(R.id.calc_edit_text)).check(matches(withText("12")))
    }

    @Test
    fun canWriteAndCalculateMathExpressions() {
        activityRule.launchActivity(null)

        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_plus)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_multiply)).perform(click())
        onView(withId(R.id.button_2)).perform(click())

        // Проверяем текст
        onView(withId(R.id.calc_edit_text)).check(matches(withText("2+2×2")))

        // Проверим вычисляемое значение
        val value = getValueOf(R.id.calc_edit_text)
        assertTrue(FloatUtils.areFloatsEquals(6f, value!!))
    }

    @Test
    fun backspaceLongClickWorks() {
        activityRule.launchActivity(null)

        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_point)).perform(click())
        onView(withId(R.id.button_9)).perform(click())

        val initialText = getTextOf(R.id.calc_edit_text)
        onView(withId(R.id.button_delete)).perform(longClick())
        val afterBackspaceText = getTextOf(R.id.calc_edit_text)

        // Убеждаемся, что что-то стёрлось (мы не можем контролировать
        // длительность нажатия бекспейса, поэтому не знаем, как много текста удалилось).
        assertTrue(afterBackspaceText.length < initialText.length)

        // Убедимся, что после долгого нажатия на бэкспейс новый текст можно ввести
        onView(withId(R.id.button_5)).perform(click())
        onView(withId(R.id.button_6)).perform(click())
        onView(withId(R.id.calc_edit_text)).check(matches(withText(afterBackspaceText + "56")))
    }

    @Test
    fun cannotWriteTextOutOfPositiveBound() {
        activityRule.launchActivity(null)

        mainThreadExecutor.execute {
            val calcEditText = activityRule.activity.findViewById<CalcEditText>(R.id.calc_edit_text)
            calcEditText.setBounds(0f, 10f)
        }

        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_0)).perform(click())

        // 10 в пределах заданных границ
        onView(withId(R.id.calc_edit_text)).check(matches(withText("10")))

        onView(withId(R.id.button_delete)).perform(click())
        onView(withId(R.id.button_1)).perform(click())
        // 11 за пределами заданных границ
        onView(withId(R.id.calc_edit_text)).check(matches(withText("1")))
    }

    @Test
    fun cannotWriteTextOutOfNegativeBound() {
        activityRule.launchActivity(null)

        mainThreadExecutor.execute {
            val calcEditText = activityRule.activity.findViewById<CalcEditText>(R.id.calc_edit_text)
            calcEditText.setBounds(-10f, 10f)
        }

        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_minus)).perform(click())
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_0)).perform(click())

        // -10 в пределах заданных границ
        onView(withId(R.id.calc_edit_text)).check(matches(withText("-10")))

        onView(withId(R.id.button_delete)).perform(click())
        onView(withId(R.id.button_1)).perform(click())
        // -11 за пределами заданных границ
        onView(withId(R.id.calc_edit_text)).check(matches(withText("-1")))
    }

    @Test
    fun cannotWriteMinusSign_ifTextMustBePositive() {
        activityRule.launchActivity(null)

        mainThreadExecutor.execute {
            val calcEditText = activityRule.activity.findViewById<CalcEditText>(R.id.calc_edit_text)
            calcEditText.setBounds(0f, 10f)
        }

        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())

        // Пытаемся ввести "-1"
        onView(withId(R.id.button_minus)).perform(click())
        onView(withId(R.id.button_1)).perform(click())

        // "-" не должен быть введён - допустимы только положительные значения и ноль (0..10)
        onView(withId(R.id.calc_edit_text)).check(matches(withText("1")))
    }

    @Test
    fun editProgressText_worksSameAsCalcEditText() {
        activityRule.launchActivity(null)

        // Открываем клавиатуру
        onView(withId(R.id.edit_progress_text)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_0)).perform(click())
        onView(withId(R.id.button_divide)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_multiply)).perform(click())
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_0)).perform(click())

        // Проверяем текст
        onView(withId(R.id.edit_progress_text)).check(matches(withText("10÷2×10")))

        // Проверим вычисляемое значение
        val value = getValueOf(R.id.edit_progress_text)
        assertTrue(FloatUtils.areFloatsEquals(50f, value!!))
    }

    @Test
    fun limitationOfDigitsAfterDot_works() {
        activityRule.launchActivity(null)

        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text_with_1_digit_after_dot)).perform(click())

        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_point)).perform(click())
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_1)).perform(click())

        // Проверяем, что текст "1.1", а не "1.111"
        onView(withId(R.id.calc_edit_text_with_1_digit_after_dot)).check(matches(withText("1.1")))
    }

    @Test
    fun canEraseAllTypedText() {
        activityRule.launchActivity(null)

        mainThreadExecutor.execute {
            val calcEditText = activityRule.activity.findViewById<CalcEditText>(R.id.calc_edit_text)
            calcEditText.setBounds(0f, 10f)
        }

        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text)).perform(click())
        // Кликаем на кнопочки на клавиатуре
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.calc_edit_text)).check(matches(withText("1")))

        onView(withId(R.id.button_delete)).perform(click())
        // Проверяем, что текст стёрт
        onView(withId(R.id.calc_edit_text)).check(matches(withText("")))
    }

    @Test
    fun focusJumping() {
        activityRule.launchActivity(null)

        // Открываем клавиатуру
        onView(withId(R.id.calc_edit_text_with_next_focus)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(matches(isDisplayed()))
        assertEquals(R.id.calc_edit_text_with_next_focus, getFocusedViewID())
        // Проверяем, что enter кнопки нет, но есть кнопка смены фокуса
        onView(withId(R.id.button_next)).check(matches(isDisplayed()))
        onView(withId(R.id.button_enter)).check(isNotDisplayed())

        // Жмякаем next, проверяем, что фокус сместился
        onView(withId(R.id.button_next)).perform(click())
        assertEquals(R.id.calc_edit_text_without_next_focus, getFocusedViewID())
        // Проверяем, что появилась enter, а кнопка смены фокуса ушла
        onView(withId(R.id.button_next)).check(isNotDisplayed())
        onView(withId(R.id.button_enter)).check(matches(isDisplayed()))

        // Жмякаем enter, проверяем, что клавиатура закрылась
        onView(withId(R.id.button_enter)).perform(click())
        onView(withId(R.id.calc_keyboard)).check(doesNotExist())
    }

    @Test
    fun bracketsUsage() {
        activityRule.launchActivity(null)

        onView(withId(R.id.calc_edit_text)).perform(click())

        onView(withId(R.id.button_bracket_left)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_plus)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_bracket_right)).perform(click())
        onView(withId(R.id.button_multiply)).perform(click())
        onView(withId(R.id.button_2)).perform(click())

        val value = getValueOf(R.id.calc_edit_text)!!
        assertEquals(8f, value, 0.000f)
    }

    private fun getTextOf(viewId: Int): String {
        var text: String? = null
        mainThreadExecutor.execute {
            val calcEditText = activityRule.activity.findViewById<CalcEditText>(viewId)
            text = calcEditText.text.toString()
        }
        return text!!
    }

    private fun getValueOf(viewId: Int): Float? {
        var value: Float? = null
        mainThreadExecutor.execute {
            val calcEditText = activityRule.activity.findViewById<CalcEditText>(viewId)
            value = calcEditText.getCurrentCalculatedValue()
        }
        return value
    }

    private fun getFocusedViewID(): Int? {
        var id: Int? = null
        mainThreadExecutor.execute {
            id = activityRule.activity.currentFocus?.id
        }
        return id
    }
}
