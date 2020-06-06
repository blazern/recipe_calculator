package korablique.recipecalculator.ui.userparameters

import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
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
import korablique.recipecalculator.model.Gender
import korablique.recipecalculator.model.Lifestyle
import korablique.recipecalculator.model.UserParameters
import korablique.recipecalculator.outside.http.BroccalcHttpContext
import korablique.recipecalculator.outside.userparams.InteractiveServerUserParamsObtainer
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import korablique.recipecalculator.ui.mainactivity.MainActivity
import korablique.recipecalculator.ui.mainactivity.MainActivityController
import korablique.recipecalculator.ui.mainactivity.MainScreenLoader
import korablique.recipecalculator.util.DBTestingUtils
import korablique.recipecalculator.util.FakeGPAuthorizer
import korablique.recipecalculator.util.InjectableActivityTestRule
import korablique.recipecalculator.util.SyncMainThreadExecutor
import korablique.recipecalculator.util.TestingTimeProvider
import kotlinx.coroutines.runBlocking
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
    private var context = InstrumentationRegistry.getTargetContext()

    @Rule
    @JvmField
    var activityRule: ActivityTestRule<UserParametersActivity> =
            InjectableActivityTestRule.forActivity(UserParametersActivity::class.java)
            .withSingletones {
                PrefsCleaningHelper.cleanAllPrefs(context)

                val databaseThreadExecutor = InstantDatabaseThreadExecutor()
                mainThreadExecutor = SyncMainThreadExecutor()
                val computationThreadsExecutor = InstantComputationsThreadsExecutor()
                val ioExecutor = InstantIOExecutor()

                val databaseHolder = DatabaseHolder(context, databaseThreadExecutor)
                val databaseWorker = DatabaseWorker(
                        databaseHolder, mainThreadExecutor, databaseThreadExecutor)
                val timeProvider = TestingTimeProvider()
                userParametersWorker = UserParametersWorker(
                        databaseHolder, mainThreadExecutor, databaseThreadExecutor,
                        RxGlobalSubscriptions())

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
                        currentActivityProvider)
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
        val initialParams = runBlocking { userParametersWorker.getCurrentUserParametersKx() }
        assertNull(initialParams)

        onView(withId(R.id.name)).perform(typeText("John Doe"))

        onView(withId(R.id.date_of_birth)).perform(scrollTo())
        onView(withId(R.id.date_of_birth)).perform(typeText("20071993"))

        onView(withId(R.id.radio_male)).perform(scrollTo())
        onView(withId(R.id.radio_male)).perform(click())

        onView(withId(R.id.target_weight)).perform(scrollTo())
        onView(withId(R.id.target_weight)).perform(replaceText("65"))

        onView(withId(R.id.lifestyle_spinner)).perform(scrollTo())
        onView(withId(R.id.lifestyle_spinner)).perform(click())
        onView(withText(R.string.active_lifestyle)).perform(click())

        onView(withId(R.id.height)).perform(scrollTo())
        onView(withId(R.id.height)).perform(replaceText("165"))

        onView(withId(R.id.weight)).perform(scrollTo())
        onView(withId(R.id.weight)).perform(replaceText("62"))

        onView(withId(R.id.button_save)).perform(scrollTo())
        onView(withId(R.id.button_save)).perform(click())

        val finalParams = runBlocking { userParametersWorker.getCurrentUserParametersKx() }!!
        val expectedParams = UserParameters(
                "John Doe", 65f, Gender.MALE, LocalDate.parse("1993-07-20"),
                165, 62f, Lifestyle.ACTIVE_LIFESTYLE,
                finalParams.formula,
                finalParams.measurementsTimestamp)
        assertEquals(expectedParams, finalParams)
    }
}