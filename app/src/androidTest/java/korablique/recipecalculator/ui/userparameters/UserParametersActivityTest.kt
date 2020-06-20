package korablique.recipecalculator.ui.userparameters

import android.view.ViewGroup
import android.widget.TextView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.nhaarman.mockitokotlin2.mock
import korablique.recipecalculator.FakeHttpClient
import korablique.recipecalculator.InstantComputationsThreadsExecutor
import korablique.recipecalculator.InstantDatabaseThreadExecutor
import korablique.recipecalculator.InstantIOExecutor
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.CurrentActivityProvider
import korablique.recipecalculator.base.RxActivitySubscriptions
import korablique.recipecalculator.base.RxGlobalSubscriptions
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.base.prefs.PrefsCleaningHelper
import korablique.recipecalculator.base.prefs.SharedPrefsManager
import korablique.recipecalculator.database.DatabaseWorker
import korablique.recipecalculator.database.FoodstuffsList
import korablique.recipecalculator.database.HistoryWorker
import korablique.recipecalculator.database.UserParametersWorker
import korablique.recipecalculator.database.getCurrentUserParametersKx
import korablique.recipecalculator.database.room.DatabaseHolder
import korablique.recipecalculator.model.FoodstuffsTopList
import korablique.recipecalculator.model.Formula
import korablique.recipecalculator.model.Gender
import korablique.recipecalculator.model.Lifestyle
import korablique.recipecalculator.model.Nutrition
import korablique.recipecalculator.model.UserParameters
import korablique.recipecalculator.outside.http.BroccalcHttpContext
import korablique.recipecalculator.outside.userparams.InteractiveServerUserParamsObtainer
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import korablique.recipecalculator.ui.DecimalUtils
import korablique.recipecalculator.ui.DecimalUtils.toDecimalString
import korablique.recipecalculator.ui.mainactivity.MainActivity
import korablique.recipecalculator.ui.mainactivity.MainActivityController
import korablique.recipecalculator.ui.mainactivity.MainScreenLoader
import korablique.recipecalculator.util.DBTestingUtils
import korablique.recipecalculator.util.FakeGPAuthorizer
import korablique.recipecalculator.util.InjectableActivityTestRule
import korablique.recipecalculator.util.SyncMainThreadExecutor
import korablique.recipecalculator.util.TestingTimeProvider
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.joda.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserParametersActivityTest {
    private lateinit var userParametersWorker: UserParametersWorker
    private lateinit var mainThreadExecutor: MainThreadExecutor
    private var context = InstrumentationRegistry.getInstrumentation().targetContext

    @Rule
    @JvmField
    var activityRule: ActivityTestRule<UserParametersActivity> =
            InjectableActivityTestRule.forActivity(UserParametersActivity::class.java)
            .withManualStart()
            .withSingletones {
                PrefsCleaningHelper.cleanAllPrefs(context)

                val databaseThreadExecutor = InstantDatabaseThreadExecutor()
                mainThreadExecutor = SyncMainThreadExecutor()
                val computationThreadsExecutor = InstantComputationsThreadsExecutor()
                val ioExecutor = InstantIOExecutor()

                val timeProvider = TestingTimeProvider()
                val databaseHolder = DatabaseHolder(context, timeProvider, databaseThreadExecutor)
                val databaseWorker = DatabaseWorker(
                        databaseHolder, mainThreadExecutor, databaseThreadExecutor)
                userParametersWorker = UserParametersWorker(
                        databaseHolder, mainThreadExecutor, databaseThreadExecutor,
                        RxGlobalSubscriptions(), timeProvider)

                val historyWorker = HistoryWorker(
                        databaseHolder, mainThreadExecutor, databaseThreadExecutor, timeProvider)

                val foodstuffsList = FoodstuffsList(
                        databaseWorker, mainThreadExecutor, computationThreadsExecutor)
                val topList = FoodstuffsTopList(historyWorker, foodstuffsList, timeProvider)
                val mainScreenLoader = MainScreenLoader(context, foodstuffsList, topList)

                val prefsManager = SharedPrefsManager(context)
                val fakeHttpClient = FakeHttpClient()
                val httpContext = BroccalcHttpContext(fakeHttpClient)
                val fakeGPAuthorizer = FakeGPAuthorizer()
                val serverUserParamsRegistry = ServerUserParamsRegistry(
                        context,
                        mainThreadExecutor, ioExecutor, fakeGPAuthorizer,
                        userParametersWorker, httpContext, prefsManager)

                val currentActivityProvider = CurrentActivityProvider()

                DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder)

                listOf(userParametersWorker, timeProvider,
                        mainScreenLoader, serverUserParamsRegistry,
                        currentActivityProvider, mainThreadExecutor)
            }
            .withActivityScoped { target: Any ->
                if (target is MainActivity) {
                    return@withActivityScoped listOf(
                            mock<MainActivityController>(),
                            mock<InteractiveServerUserParamsObtainer>())
                }
                val activity = target as BaseActivity
                listOf(RxActivitySubscriptions(activity.activityCallbacks))
            }
            .build()


    @Test
    fun createsUserParameters() {
        activityRule.launchActivity(null)

        val initialParams = runBlocking { userParametersWorker.getCurrentUserParametersKx() }
        assertNull(initialParams)

        onView(withId(R.id.name)).perform(typeText("John Doe"))
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.date_of_birth)).perform(scrollTo())
        onView(withId(R.id.date_of_birth)).perform(typeText("20071993"))
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.radio_male)).perform(scrollTo())
        onView(withId(R.id.radio_male)).perform(click())

        onView(withId(R.id.target_weight)).perform(scrollTo())
        onView(withId(R.id.target_weight)).perform(typeText("65"))
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.lifestyle_spinner)).perform(scrollTo())
        onView(withId(R.id.lifestyle_spinner)).perform(click())
        onView(withText(R.string.active_lifestyle)).perform(click())

        onView(withId(R.id.height)).perform(scrollTo())
        onView(withId(R.id.height)).perform(typeText("165"))
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.weight)).perform(scrollTo())
        onView(withId(R.id.weight)).perform(typeText("62"))
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.button_save)).perform(scrollTo())
        onView(withId(R.id.button_save)).perform(click())

        val finalParams = runBlocking { userParametersWorker.getCurrentUserParametersKx() }!!
        val expectedParams = UserParameters(
                "John Doe", 65f, Gender.MALE, LocalDate.parse("1993-07-20"),
                165, 62f, Lifestyle.ACTIVE_LIFESTYLE,
                finalParams.formula,
                Nutrition.withValues(124.0, 62.0, 504.977600, 3073.910400),
                finalParams.measurementsTimestamp)
        assertEquals(expectedParams, finalParams)
    }

    @Test
    fun displayinExistingUserParameters() {
        // сохраняем userParameters в БД
        val initialUserParameters = UserParameters(
                "John Doe", 45f, Gender.FEMALE, LocalDate(1993, 9, 27),
                158, 48f, Lifestyle.PASSIVE_LIFESTYLE, Formula.HARRIS_BENEDICT,
                Nutrition.zero(), // Nutrition will be recalculated
                0)
        userParametersWorker.saveUserParameters(initialUserParameters)
        activityRule.launchActivity(null)

        onView(withId(R.id.name)).check(matches(withText("John Doe")));

        onView(withId(R.id.date_of_birth)).perform(scrollTo())
        onView(withId(R.id.date_of_birth)).check(matches(withText("27.09.1993")))

        onView(withId(R.id.radio_male)).perform(scrollTo())
        onView(withId(R.id.radio_male)).check(matches(isNotChecked()))
        onView(withId(R.id.radio_female)).check(matches(isChecked()))

        onView(withId(R.id.target_weight)).perform(scrollTo())
        onView(withId(R.id.target_weight)).check(matches(withText(("45"))))

        onView(withId(R.id.height)).perform(scrollTo())
        onView(withId(R.id.height)).check(matches(withText("158")))

        onView(withId(R.id.weight)).perform(scrollTo())
        onView(withId(R.id.weight)).check(matches(withText("48")))

        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .perform(scrollTo())

        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("96")))
        onView(allOf(
                isDescendantOfA(withId(R.id.fats_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("48")))
        onView(allOf(
                isDescendantOfA(withId(R.id.carbs_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("150.1")))
        onView(allOf(
                isDescendantOfA(withId(R.id.calories_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("1416.6")))
    }

    @Test
    fun setProteinRateManually() {
        // сохраняем userParameters в БД
        val initialUserParameters = UserParameters(
                "John Doe", 45f, Gender.FEMALE, LocalDate(1993, 9, 27),
                158, 48f, Lifestyle.PASSIVE_LIFESTYLE, Formula.HARRIS_BENEDICT,
                Nutrition.zero(), // Nutrition will be recalculated
                0)
        userParametersWorker.saveUserParameters(initialUserParameters)
        activityRule.launchActivity(null)

        onView(withId(R.id.formula_spinner)).perform(scrollTo())
        onView(withId(R.id.formula_spinner)).perform(click())
        onView(withText(R.string.manually)).perform(click())

        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_edit_text)))
                .perform(scrollTo())

        val oldProtein = getRateValue(R.id.protein_layout)
        val oldFats = getRateValue(R.id.fats_layout)
        val oldCarbs = getRateValue(R.id.carbs_layout)
        val oldCalories = getRateValue(R.id.calories_layout)

        val newProtein = oldProtein - 30f
        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_edit_text)))
                .perform(click())
        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_edit_text)))
                .perform(replaceText(newProtein.toString()))

        val proteinDiff = oldProtein - newProtein
        val expectedCalories = (oldCalories - proteinDiff * 4)

        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_edit_text)))
                .check(matches(withText(toDecimalString(newProtein))))
        onView(allOf(
                isDescendantOfA(withId(R.id.fats_layout)),
                withId(R.id.nutrition_edit_text)))
                .check(matches(withText(toDecimalString(oldFats))))
        onView(allOf(
                isDescendantOfA(withId(R.id.carbs_layout)),
                withId(R.id.nutrition_edit_text)))
                .check(matches(withText(toDecimalString(oldCarbs))))

        val newCalories = getRateValue(R.id.calories_layout)
        // Allowed delta is 1f, because we compare a value we obtained from UI
        // with value which is calculated by RateCalculator - the value from UI has it's tail
        // cut out, while RateCalculator doesn't cut out anything, so the 2 floats are going to be
        // a bit different
        assertEquals(expectedCalories, newCalories, 1f)

        onView(withId(R.id.button_save)).perform(scrollTo())
        onView(withId(R.id.button_save)).perform(click())

        val finalParams = runBlocking { userParametersWorker.getCurrentUserParametersKx() }!!
        val expectedParams = UserParameters(
                "John Doe", 45f, Gender.FEMALE, LocalDate(1993, 9, 27),
                158, 48f, Lifestyle.PASSIVE_LIFESTYLE, Formula.MANUAL,
                finalParams.rates,
                finalParams.measurementsTimestamp)
        assertEquals(expectedParams, finalParams)

        // We allow final and expected rates to have a difference of 1.0 because they're
        // calculated a bit differently.
        assertEquals(finalParams.rates.protein, newProtein.toDouble(), 1.0)
        assertEquals(finalParams.rates.fats, oldFats.toDouble(), 1.0)
        assertEquals(finalParams.rates.carbs, oldCarbs.toDouble(), 1.0)
        assertEquals(finalParams.rates.calories, newCalories.toDouble(), 1.0)
    }

    @Test
    fun setCaloriesRateManually() {
        // сохраняем userParameters в БД
        val initialUserParameters = UserParameters(
                "John Doe", 45f, Gender.FEMALE, LocalDate(1993, 9, 27),
                158, 48f, Lifestyle.PASSIVE_LIFESTYLE, Formula.HARRIS_BENEDICT,
                Nutrition.zero(), // Nutrition will be recalculated
                0)
        userParametersWorker.saveUserParameters(initialUserParameters)
        activityRule.launchActivity(null)

        onView(withId(R.id.formula_spinner)).perform(scrollTo())
        onView(withId(R.id.formula_spinner)).perform(click())
        onView(withText(R.string.manually)).perform(click())

        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_edit_text)))
                .perform(scrollTo())

        val oldProtein = getRateValue(R.id.protein_layout)
        val oldFats = getRateValue(R.id.fats_layout)
        val oldCarbs = getRateValue(R.id.carbs_layout)
        val oldCalories = getRateValue(R.id.calories_layout)

        val newCalories = oldCalories / 10f
        onView(allOf(
                isDescendantOfA(withId(R.id.calories_layout)),
                withId(R.id.nutrition_edit_text)))
                .perform(click())
        onView(allOf(
                isDescendantOfA(withId(R.id.calories_layout)),
                withId(R.id.nutrition_edit_text)))
                .perform(replaceText(newCalories.toString()))

        val expectedProtein = oldProtein / 10f
        val expectedFats = oldFats / 10f
        val expectedCarbs = oldCarbs / 10f

        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_edit_text)))
                .check(matches(withText(toDecimalString(expectedProtein))))
        onView(allOf(
                isDescendantOfA(withId(R.id.fats_layout)),
                withId(R.id.nutrition_edit_text)))
                .check(matches(withText(toDecimalString(expectedFats))))
        onView(allOf(
                isDescendantOfA(withId(R.id.carbs_layout)),
                withId(R.id.nutrition_edit_text)))
                .check(matches(withText(toDecimalString(expectedCarbs))))

        onView(withId(R.id.button_save)).perform(scrollTo())
        onView(withId(R.id.button_save)).perform(click())

        val finalParams = runBlocking { userParametersWorker.getCurrentUserParametersKx() }!!
        val expectedParams = UserParameters(
                "John Doe", 45f, Gender.FEMALE, LocalDate(1993, 9, 27),
                158, 48f, Lifestyle.PASSIVE_LIFESTYLE, Formula.MANUAL,
                finalParams.rates,
                finalParams.measurementsTimestamp)
        assertEquals(expectedParams, finalParams)

        // We allow final and expected rates to have a difference of 1.0 because they're
        // calculated a bit differently.
        assertEquals(finalParams.rates.protein, expectedProtein.toDouble(), 1.0)
        assertEquals(finalParams.rates.fats, expectedFats.toDouble(), 1.0)
        assertEquals(finalParams.rates.carbs, expectedCarbs.toDouble(), 1.0)
        assertEquals(finalParams.rates.calories, newCalories.toDouble(), 1.0)
    }

    private fun getRateValue(rateLayoutId: Int): Float {
        var rate: Float? = null
        mainThreadExecutor.execute {
            rate = activityRule.activity
                    .findViewById<ViewGroup>(rateLayoutId)
                    .findViewById<TextView>(R.id.nutrition_edit_text)
                    .text.toString().toFloat()
        }
        return rate!!
    }
}