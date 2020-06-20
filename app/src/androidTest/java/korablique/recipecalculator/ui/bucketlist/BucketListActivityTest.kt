package korablique.recipecalculator.ui.bucketlist

import android.app.Activity
import android.text.TextUtils
import android.view.View
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove
import androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import korablique.recipecalculator.InstantComputationsThreadsExecutor
import korablique.recipecalculator.InstantDatabaseThreadExecutor
import korablique.recipecalculator.InstantIOExecutor
import korablique.recipecalculator.R
import korablique.recipecalculator.base.ActivityCallbacks
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.BaseBottomDialog
import korablique.recipecalculator.base.BaseFragment
import korablique.recipecalculator.base.CurrentActivityProvider
import korablique.recipecalculator.base.FragmentCallbacks
import korablique.recipecalculator.base.RxActivitySubscriptions
import korablique.recipecalculator.base.RxFragmentSubscriptions
import korablique.recipecalculator.base.RxGlobalSubscriptions
import korablique.recipecalculator.base.TimeProvider
import korablique.recipecalculator.base.executors.IOExecutor
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.base.prefs.PrefsCleaningHelper.cleanAllPrefs
import korablique.recipecalculator.base.prefs.SharedPrefsManager
import korablique.recipecalculator.database.CreateRecipeResult
import korablique.recipecalculator.database.DatabaseThreadExecutor
import korablique.recipecalculator.database.DatabaseWorker
import korablique.recipecalculator.database.FoodstuffsList
import korablique.recipecalculator.database.HistoryWorker
import korablique.recipecalculator.database.RecipeDatabaseWorker
import korablique.recipecalculator.database.RecipeDatabaseWorkerImpl
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.database.UserParametersWorker
import korablique.recipecalculator.database.room.DatabaseHolder
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Ingredient.Companion.create
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.model.Recipe.Companion.create
import korablique.recipecalculator.ui.DecimalUtils
import korablique.recipecalculator.ui.DecimalUtils.toDecimalString
import korablique.recipecalculator.ui.bucketlist.BucketListActivity.Companion.createIntent
import korablique.recipecalculator.ui.calckeyboard.CalcKeyboardController
import korablique.recipecalculator.ui.mainactivity.MainActivity
import korablique.recipecalculator.ui.mainactivity.MainActivityController
import korablique.recipecalculator.ui.mainactivity.MainActivityFragmentsController
import korablique.recipecalculator.ui.mainactivity.MainActivitySelectedDateStorage
import korablique.recipecalculator.ui.mainactivity.history.HistoryController
import korablique.recipecalculator.ui.mainactivity.history.HistoryFragment
import korablique.recipecalculator.util.DBTestingUtils.Companion.clearAllData
import korablique.recipecalculator.util.EspressoUtils
import korablique.recipecalculator.util.EspressoUtils.isNotDisplayed
import korablique.recipecalculator.util.EspressoUtils.matches
import korablique.recipecalculator.util.FloatUtils
import korablique.recipecalculator.util.InjectableActivityTestRule
import korablique.recipecalculator.util.SyncMainThreadExecutor
import korablique.recipecalculator.util.TestingTimeProvider
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matchers
import org.hamcrest.Matchers.any
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.util.*

data class UIIngredient(
        val name: String,
        val weight: String,
        val comment: String = "")

private fun Ingredient.intoUI() = UIIngredient(
        foodstuff.name,
        toDecimalString(weight),
        comment)

@RunWith(AndroidJUnit4::class)
@LargeTest
class BucketListActivityTest {
    private var context = InstrumentationRegistry.getTargetContext()
    private lateinit var databaseHolder: DatabaseHolder
    private lateinit var mainThreadExecutor: MainThreadExecutor
    private val ioExecutor: IOExecutor = InstantIOExecutor()
    private lateinit var databaseThreadExecutor: DatabaseThreadExecutor
    private lateinit var databaseWorker: DatabaseWorker
    private lateinit var historyWorker: HistoryWorker
    private lateinit var userParametersWorker: UserParametersWorker
    private lateinit var foodstuffsList: FoodstuffsList
    private lateinit var recipeDatabaseWorker: RecipeDatabaseWorker
    private lateinit var recipesRepository: RecipesRepository
    private lateinit var historyController: HistoryController
    private lateinit var timeProvider: TimeProvider
    private lateinit var currentActivityProvider: CurrentActivityProvider
    private lateinit var bucketList: BucketList
    private lateinit var prefsManager: SharedPrefsManager
    private lateinit var calcKeyboardController: CalcKeyboardController

    @Rule
    @JvmField
    var activityRule: ActivityTestRule<BucketListActivity> = InjectableActivityTestRule.forActivity(BucketListActivity::class.java)
            .withManualStart()
            .withSingletones {
                cleanAllPrefs(context)
                databaseThreadExecutor = InstantDatabaseThreadExecutor()
                databaseHolder = DatabaseHolder(
                        context, TestingTimeProvider(), databaseThreadExecutor)
                databaseHolder.database.clearAllTables()
                prefsManager = SharedPrefsManager(context)
                mainThreadExecutor = SyncMainThreadExecutor()
                databaseWorker = DatabaseWorker(
                        databaseHolder, mainThreadExecutor, databaseThreadExecutor)
                timeProvider = TestingTimeProvider()
                historyWorker = HistoryWorker(
                        databaseHolder, mainThreadExecutor, databaseThreadExecutor,
                        timeProvider)
                userParametersWorker = UserParametersWorker(
                        databaseHolder, mainThreadExecutor, databaseThreadExecutor,
                        RxGlobalSubscriptions(), timeProvider)
                foodstuffsList = FoodstuffsList(databaseWorker, mainThreadExecutor,
                        InstantComputationsThreadsExecutor())
                recipeDatabaseWorker = RecipeDatabaseWorkerImpl(
                        ioExecutor, databaseHolder, databaseWorker)
                recipesRepository = RecipesRepository(
                        recipeDatabaseWorker, foodstuffsList, mainThreadExecutor)
                currentActivityProvider = CurrentActivityProvider()
                bucketList = BucketList(prefsManager)
                calcKeyboardController = CalcKeyboardController()
                listOf(mainThreadExecutor, databaseThreadExecutor, databaseWorker,
                        historyWorker, userParametersWorker, foodstuffsList,
                        timeProvider, currentActivityProvider, bucketList,
                        CalcKeyboardController(), recipesRepository, calcKeyboardController,
                        prefsManager)
            }
            .withActivityScoped { target: Any ->
                if (target is BucketListActivity) {
                    val controller = BucketListActivityController(
                            target, recipesRepository, bucketList,
                            mainThreadExecutor, calcKeyboardController)
                    return@withActivityScoped listOf<Any>(controller)
                }
                val activity = target as MainActivity
                val activityCallbacks = ActivityCallbacks()
                val controller = MainActivityController(activity,
                        activityCallbacks,
                        Mockito.mock(MainActivityFragmentsController::class.java))
                listOf<Any>(RxActivitySubscriptions(activity.activityCallbacks),
                        controller)
            }
            .withFragmentScoped { target: Any ->
                if (target is BaseBottomDialog) {
                    return@withFragmentScoped emptyList<Any>()
                }
                val fragment = target as BaseFragment
                val fragmentCallbacks = FragmentCallbacks()
                val subscriptions = RxFragmentSubscriptions(fragmentCallbacks)
                if (fragment is HistoryFragment) {
                    historyController = HistoryController(fragment.getActivity() as BaseActivity?,
                            fragment, fragmentCallbacks, historyWorker, timeProvider,
                            Mockito.mock(MainActivityFragmentsController::class.java),
                            Mockito.mock(MainActivitySelectedDateStorage::class.java))
                    return@withFragmentScoped listOf<Any>(subscriptions, historyController)
                }
                emptyList()
            }
            .build()

    @Test
    fun totalWeightEditing() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)

        onView(withId(R.id.button_edit)).perform(click())

        onView(withId(R.id.total_weight_edit_text)).check(matches(withText("123")))
        onView(withId(R.id.total_weight_edit_text)).perform(click())
        onView(withId(R.id.button_delete)).perform(click())
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText("12")))
        onView(withId(R.id.button_delete)).perform(click())
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText("1")))
        onView(withId(R.id.button_delete)).perform(click())
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText("")))

        onView(withId(R.id.button_bracket_left)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_plus)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_bracket_right)).perform(click())
        onView(withId(R.id.button_multiply)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText("(2+2)×2")))
    }

    @Test
    fun containsFoodstuffsFromBucketList() {
        val f1 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("apple").withNutrition(1f, 2f, 3f, 4f)).blockingGet()
        val f2 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("water").withNutrition(1f, 2f, 3f, 4f)).blockingGet()
        val f3 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("beer").withNutrition(1f, 2f, 3f, 4f)).blockingGet()
        val ingredients = ArrayList<Ingredient>()
        ingredients.add(create(f1, 123f, ""))
        ingredients.add(create(f2, 123f, ""))
        ingredients.add(create(f3, 123f, ""))
        mainThreadExecutor.execute { bucketList.add(ingredients) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        onView(withText("apple")).check(matches(isDisplayed()))
        onView(withText("water")).check(matches(isDisplayed()))
        onView(withText("beer")).check(matches(isDisplayed()))
    }

    @Test
    fun usesWeightAndNameFromBucketList() {
        mainThreadExecutor.execute {
            bucketList.setTotalWeight(123f)
            bucketList.setName("Banana")
        }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText("123")))
        onView(withId(R.id.recipe_name_edit_text)).check(matches(withText("Banana")))
    }

    @Test
    fun savesRecipeFoodstuffToFoodstuffsList() {
        val f1 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32.0)).blockingGet()
        val f2 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("oil").withNutrition(0.0, 99.9, 0.0, 899.0)).blockingGet()
        val ingredients = ArrayList<Ingredient>()
        ingredients.add(create(f1, 310f, ""))
        ingredients.add(create(f2, 13f, ""))
        mainThreadExecutor.execute { bucketList.add(ingredients) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("323"))
        val dishName = "carrot with oil"
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText(dishName), closeSoftKeyboard())
        onView(withId(R.id.save_as_recipe_button)).perform(click())
        val foodstuffs = foodstuffsList
                .getAllFoodstuffs()
                .filter { foodstuff: Foodstuff -> foodstuff.name == dishName }
                .toList()
                .blockingGet()
        assertEquals(1, foodstuffs.size.toLong())
    }

    @Test
    fun setsActivityResultWhenSavesCreatedRecipe() {
        val f1 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32.0)).blockingGet()
        val ingredients = ArrayList<Ingredient>()
        ingredients.add(create(f1, 310f, ""))
        mainThreadExecutor.execute { bucketList.add(ingredients) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("323"))
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("new super carrot"), closeSoftKeyboard())
        onView(withId(R.id.save_as_recipe_button)).perform(click())
        val resultIntent = activityRule.activityResult.resultData
        val recipe = resultIntent.getParcelableExtra<Recipe>(EXTRA_PRODUCED_RECIPE)
        assertEquals("new super carrot", recipe.name)
    }

    @Test
    fun setsActivityResultAsCanceled_whenUserEditsRecipeSavesAndExits() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)
        onView(withId(R.id.button_edit)).perform(click())
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("323"))
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("new super cake"), closeSoftKeyboard())
        onView(withId(R.id.save_as_recipe_button)).perform(click())
        onView(withId(R.id.button_close)).perform(click())
        assertEquals(Activity.RESULT_CANCELED.toLong(), activityRule.activityResult.resultCode.toLong())
    }

    @Test
    fun setsActivityResultAsCanceled_whenUserCancelsRecipeCreation() {
        val f1 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32.0)).blockingGet()
        val ingredients = ArrayList<Ingredient>()
        ingredients.add(create(f1, 310f, ""))
        mainThreadExecutor.execute { bucketList.add(ingredients) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        onView(withId(R.id.button_close)).perform(click())
        onView(withId(R.id.positive_button)).perform(click()) // Yes, close
        assertEquals(Activity.RESULT_CANCELED.toLong(), activityRule.activityResult.resultCode.toLong())
    }

    @Test
    fun changingFoodstuffWeightChangesBucketList() {
        val f1 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32.0)).blockingGet()
        val ingredients = ArrayList<Ingredient>()
        ingredients.add(create(f1, 310f, ""))
        mainThreadExecutor.execute { bucketList.add(ingredients) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        onView(withText("carrot")).perform(click())
        onView(withId(R.id.weight_edit_text)).perform(replaceText("10"))
        onView(withId(R.id.button1)).perform(click())
        mainThreadExecutor.execute {
            assertEquals(1, bucketList.getList().size.toLong())
            Assert.assertTrue(FloatUtils.areFloatsEquals(10f, bucketList.getList()[0].weight))
        }
    }

    @Test
    fun changingNameChangesBucketList() {
        mainThreadExecutor.execute { bucketList.setName("original name") }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("new name"))
        mainThreadExecutor.execute { assertEquals("new name", bucketList.getName()) }
    }

    @Test
    fun updatesBucketListTotalWeight_whenUserEditsIt() {
        mainThreadExecutor.execute { bucketList.setTotalWeight(123f) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("321"))
        mainThreadExecutor.execute { assertEquals(321f, bucketList.getTotalWeight(), 0.001f) }
    }

    @Test
    fun updatesBucketListTotalWeight_whenUserRemovesIngredient() {
        val f1 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32.0)).blockingGet()
        val f2 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("oil").withNutrition(0.0, 99.9, 0.0, 899.0)).blockingGet()
        val ingredients = ArrayList<Ingredient>()
        ingredients.add(create(f1, 10f, ""))
        ingredients.add(create(f2, 10f, ""))
        mainThreadExecutor.execute {
            bucketList.add(ingredients)
            bucketList.setTotalWeight(20f)
        }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)

        // Deleting product
        onView(withText("carrot")).perform(longClick())
        onView(withText(R.string.delete_ingredient)).perform(click())
        mainThreadExecutor.execute {
            // The left product has a weight of 10
            assertEquals(10.0, bucketList.getTotalWeight().toDouble(), 0.001)
        }
    }

    @Test
    fun updatesBucketListTotalWeight_whenUserChangesIngredientWeight() {
        val f1 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32.0)).blockingGet()
        val ingredients = ArrayList<Ingredient>()
        ingredients.add(create(f1, 123f, ""))
        mainThreadExecutor.execute {
            bucketList.add(ingredients)
            bucketList.setTotalWeight(123f)
        }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        onView(withText("carrot")).perform(click())
        onView(withId(R.id.weight_edit_text)).perform(replaceText("10"))
        onView(withId(R.id.button1)).perform(click())
        mainThreadExecutor.execute { assertEquals(10f, bucketList.getTotalWeight(), 0.001f) }
    }

    @Test
    fun nutritionUpdates_whenTotalWeightIsEdited() {
        val f1 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("carrot").withNutrition(10f, 1f, 1f, 1f)).blockingGet()
        val ingredients = ArrayList<Ingredient>()
        ingredients.add(create(f1, 100f, ""))
        mainThreadExecutor.execute {
            bucketList.add(ingredients)
            bucketList.setTotalWeight(100f)
        }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("10")))
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("50"))
        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("20")))
    }

    @Test
    fun invalidNutritionIsAlteredToValid() {
        val f1 = foodstuffsList.saveFoodstuff(
                Foodstuff.withName("potato").withNutrition(10f, 10f, 10f, 1f)).blockingGet()
        val ingredients = ArrayList<Ingredient>()
        ingredients.add(create(f1, 100f, ""))
        mainThreadExecutor.execute {
            bucketList.add(ingredients)
            bucketList.setTotalWeight(100f)
        }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)

        // Initial weight
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText("100")))
        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("10")))

        // Dividing total weight by 2 gives the resulted recipe
        // nutrition: 20 + 20 + 20 = 60, which is valid
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("50"))
        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("20")))

        // Nutrition would be 40 + 40 + 40 = 120 if the activity didn't alter it, but it's not
        // a valid nutrition since 100 grams of a product cannot have 120 grams of nutrition.
        // Thus, the activity is expected to alter the nutrition to make the nutrition fit
        // the 100 grams limit.
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("25"))
        onView(allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText("33.3")))
    }

    @Test
    fun editRecipe() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)

        // Verify valid values
        verifyRecipeDisplayingState(recipe)

        // Edit
        onView(withId(R.id.button_edit)).perform(click())
        onView(withText("oil")).perform(longClick())
        onView(withText(R.string.delete_ingredient)).perform(click())
        onView(withText("dough")).perform(click())
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).perform(replaceText("3"))
        onView(withId(R.id.button1)).perform(click())
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("cake without oil"))
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("10"))
        onView(withId(R.id.add_comment_button)).perform(click())
        onView(withId(R.id.comment)).perform(replaceText("novel comment"))
        onView(withId(R.id.save_as_recipe_button)).perform(click())

        // Validate updated recipe
        val allRecipes: List<Recipe> = ArrayList(recipesRepository.getAllRecipesRx().blockingGet())
        assertEquals(1, allRecipes.size.toLong())
        val (_, foodstuff, ingredients, weight, comment) = allRecipes[0]
        assertEquals("cake without oil", foodstuff.name)
        assertEquals(10f, weight, 0.001f)
        assertEquals(1, ingredients.size.toLong())
        assertEquals("dough", ingredients[0].foodstuff.name)
        assertEquals(3f, ingredients[0].weight, 0.001f)
        assertEquals("novel comment", comment)

        // Validate updated recipe UI
        verifyRecipeDisplayingState(
                recipe.copy(
                        foodstuff = recipe.foodstuff.recreateWithName("cake without oil"),
                        weight = 10f,
                        ingredients = listOf(
                            recipe.ingredients[0].copy(weight = 3f)),
                        comment = "novel comment")
                        .recalculateNutrition())
    }

    @Test
    fun recipeModificationStateRestore() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)

        // Edit
        onView(withId(R.id.button_edit)).perform(click())
        verifyRecipeEditingState(recipe)
        onView(withText("oil")).perform(longClick())
        onView(withText(R.string.delete_ingredient)).perform(click())
        onView(withText("dough")).perform(click())
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).perform(replaceText("3"))
        onView(withId(R.id.button1)).perform(click())
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("cake without oil"))
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("10"))
        onView(withId(R.id.add_comment_button)).perform(click())
        onView(withId(R.id.comment)).perform(replaceText("novel comment"))
        mainThreadExecutor.execute { activityRule.activity.recreate() }
        verifyRecipeEditingState(
                recipe.copy(
                        foodstuff = recipe.foodstuff.recreateWithName("cake without oil"),
                        weight = 10f,
                        ingredients = listOf(
                                recipe.ingredients[0].copy(weight = 3f)),
                        comment = "novel comment")
                        .recalculateNutrition())
    }

    @Test
    fun recipeCreationStateRestore() {
        // Start creating recipe (put not saved recipe into bucket list)
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        mainThreadExecutor.execute { bucketList.setRecipe(recipe) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)

        // Verify displayed data, verify creating state, edit the recipe
        verifyRecipeCreatingState(recipe)
        onView(withText("oil")).perform(longClick())
        onView(withText(R.string.delete_ingredient)).perform(click())
        onView(withText("dough")).perform(click())
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).perform(replaceText("3"))
        onView(withId(R.id.button1)).perform(click())
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("cake without oil"))
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("10"))
        onView(withId(R.id.add_comment_button)).perform(click())
        onView(withId(R.id.comment)).perform(replaceText("novel comment"))
        mainThreadExecutor.execute { activityRule.activity.recreate() }
        verifyRecipeCreatingState(
                recipe.copy(
                        foodstuff = recipe.foodstuff.recreateWithName("cake without oil"),
                        weight = 10f,
                        ingredients = listOf(
                                recipe.ingredients[0].copy(weight = 3f)),
                        comment = "novel comment")
                        .recalculateNutrition())
    }

    @Test
    fun recipeDisplayingStateRestore() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)
        verifyRecipeDisplayingState(recipe)

        // Edit and save
        onView(withId(R.id.button_edit)).perform(click())
        verifyRecipeEditingState(recipe)
        onView(withText("oil")).perform(longClick())
        onView(withText(R.string.delete_ingredient)).perform(click())
        onView(withText("dough")).perform(click())
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).perform(replaceText("3"))
        onView(withId(R.id.button1)).perform(click())
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("cake without oil"))
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("10"))
        onView(withId(R.id.add_comment_button)).perform(click())
        onView(withId(R.id.comment)).perform(replaceText("novel comment"))
        onView(withId(R.id.save_as_recipe_button)).perform(click())
        mainThreadExecutor.execute { activityRule.activity.recreate() }
        verifyRecipeDisplayingState(
                recipe.copy(
                        foodstuff = recipe.foodstuff.recreateWithName("cake without oil"),
                        weight = 10f,
                        ingredients = listOf(
                                recipe.ingredients[0].copy(weight = 3f)),
                        comment = "novel comment")
                        .recalculateNutrition())
    }

    @Test
    fun ingredientsClicksInDisplayAndEditRecipeModes() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)
        verifyRecipeDisplayingState(recipe)

        // Display mode
        onView(withText("dough")).perform(click())
        onView(withId(R.id.foodstuff_card_layout)).check(isNotDisplayed())
        onView(withText("dough")).perform(longClick())
        onView(withText(R.string.delete_ingredient)).check(isNotDisplayed())
        onView(withText(R.string.edit_ingredient_comment)).check(isNotDisplayed())

        // Edit mode
        onView(withId(R.id.button_edit)).perform(click())
        onView(withText("dough")).perform(click())
        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.button_close)).perform(click())
        onView(withText("dough")).perform(longClick())
        onView(withText(R.string.delete_ingredient)).check(matches(isDisplayed()))
        onView(withText(R.string.edit_ingredient_comment)).check(matches(isDisplayed()))
    }

    @Test
    fun switchingBetweenDisplayAndEditStates_withRecipeEditing() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)
        verifyRecipeDisplayingState(recipe)

        // Switch states without editing recipe
        onView(withId(R.id.button_edit)).perform(click())
        verifyRecipeEditingState(recipe)
        onView(withId(R.id.button_close)).perform(click())
        verifyRecipeDisplayingState(recipe)

        // Switch states with editing recipe
        onView(withId(R.id.button_edit)).perform(click())
        verifyRecipeEditingState(recipe)
        onView(withText("oil")).perform(longClick())
        onView(withText(R.string.delete_ingredient)).perform(click())
        onView(withText("dough")).perform(click())
        onView(allOf(
                withParent(isDescendantOfA(withId(R.id.foodstuff_card_layout))),
                withId(R.id.weight_edit_text)
        )).perform(replaceText("3"))
        onView(withId(R.id.button1)).perform(click())
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("cake without oil"))
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("10"))
        onView(withId(R.id.add_comment_button)).perform(click())
        onView(withId(R.id.comment)).perform(replaceText("novel comment"))

        val updatedRecipe =
                recipe.copy(
                        foodstuff = recipe.foodstuff.recreateWithName("cake without oil"),
                        weight = 10f,
                        ingredients = listOf(
                                recipe.ingredients[0].copy(weight = 3f)),
                        comment = "novel comment")
                        .recalculateNutrition()
        verifyRecipeEditingState(updatedRecipe)
        onView(withId(R.id.save_as_recipe_button)).perform(click())
        verifyRecipeDisplayingState(updatedRecipe)
    }

    @Test
    fun cancelRecipeEditingByBackPress() {
        cancelRecipeEditing(true)
    }

    @Test
    fun cancelRecipeEditingByButtonCloseClick() {
        cancelRecipeEditing(false)
    }

    private fun cancelRecipeEditing(byBackPress: Boolean) {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)
        verifyRecipeDisplayingState(recipe)

        // Edit, try cancel, but don't cancel
        onView(withId(R.id.button_edit)).perform(click())
        verifyRecipeEditingState(recipe)
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("novel cake"))
        if (byBackPress) {
            Espresso.pressBack()
        } else {
            onView(withId(R.id.button_close)).perform(click())
        }
        onView(withId(R.id.two_options_dialog_layout)).check(matches(isDisplayed()))
        onView(allOf(
                isDescendantOfA(withId(R.id.two_options_dialog_layout)),
                withText(R.string.cancel_recipe_editing_dialog_title)
        )).perform(click())
        onView(withId(R.id.negative_button)).perform(click())
        verifyRecipeEditingState(recipe.copy(foodstuff = recipe.foodstuff.recreateWithName("novel cake")))

        // This time - confirm cancellation
        if (byBackPress) {
            Espresso.pressBack()
        } else {
            onView(withId(R.id.button_close)).perform(click())
        }
        onView(withId(R.id.two_options_dialog_layout)).check(matches(isDisplayed()))
        onView(allOf(
                isDescendantOfA(withId(R.id.two_options_dialog_layout)),
                withText(R.string.cancel_recipe_editing_dialog_title)
        )).perform(click())
        onView(withId(R.id.positive_button)).perform(click())
        verifyRecipeDisplayingState(recipe)

        // Verify that the recipe is not changed
        val allRecipes = recipesRepository.getAllRecipesRx().blockingGet()
        assertEquals(1, allRecipes.size.toLong())
        assertEquals("cake", allRecipes.iterator().next().foodstuff.name)
        assertEquals(recipe, allRecipes.iterator().next())
    }

    @Test
    fun cancelRecipeCreationByBackPress() {
        cancelRecipeCreation(true)
    }

    @Test
    fun cancelRecipeCreationByButtonCloseClick() {
        cancelRecipeCreation(false)
    }

    private fun cancelRecipeCreation(byBackPress: Boolean) {
        // Start recipe creation
        var initialRecipeNullable: Recipe? = null
        mainThreadExecutor.execute {
            bucketList.setName("cake")
            bucketList.setTotalWeight(123f)
            initialRecipeNullable = bucketList.getRecipe()
        }
        val initialRecipe = initialRecipeNullable!!
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        verifyRecipeCreatingState(initialRecipe)

        // Edit, try cancel, but don't cancel
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("novel cake"))
        if (byBackPress) {
            Espresso.pressBack()
        } else {
            onView(withId(R.id.button_close)).perform(click())
        }
        onView(withId(R.id.two_options_dialog_layout)).check(matches(isDisplayed()))
        onView(allOf(
                isDescendantOfA(withId(R.id.two_options_dialog_layout)),
                withText(R.string.cancel_recipe_creation_dialog_title)
        )).perform(click())
        onView(withId(R.id.negative_button)).perform(click())
        verifyRecipeCreatingState(
                initialRecipe.copy(foodstuff = initialRecipe.foodstuff.recreateWithName("novel cake")))

        // This time - confirm cancellation
        if (byBackPress) {
            Espresso.pressBack()
        } else {
            onView(withId(R.id.button_close)).perform(click())
        }
        onView(withId(R.id.two_options_dialog_layout)).check(matches(isDisplayed()))
        onView(allOf(
                isDescendantOfA(withId(R.id.two_options_dialog_layout)),
                withText(R.string.cancel_recipe_creation_dialog_title)
        )).perform(click())
        mainThreadExecutor.execute { Assert.assertFalse(activityRule.activity.isFinishing) }
        onView(withId(R.id.positive_button)).perform(click())
        mainThreadExecutor.execute { Assert.assertTrue(activityRule.activity.isFinishing) }

        // Verify that the recipe was not created and bucket list is cleaned
        val allRecipes = recipesRepository.getAllRecipesRx().blockingGet()
        assertEquals(0, allRecipes.size.toLong())
        assertEquals("", bucketList.getName())
        assertEquals(0f, bucketList.getTotalWeight(), 0.0001f)
    }

    @Test
    fun cancelRecipeEditingWithoutRecipeModification_byCloseButton() {
        cancelRecipeEditingWithoutRecipeModification(false)
    }

    @Test
    fun cancelRecipeEditingWithoutRecipeModification_byBackPress() {
        cancelRecipeEditingWithoutRecipeModification(true)
    }

    private fun cancelRecipeEditingWithoutRecipeModification(byBackPress: Boolean) {
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)
        verifyRecipeDisplayingState(recipe)
        onView(withId(R.id.button_edit)).perform(click())
        verifyRecipeEditingState(recipe)
        mainThreadExecutor.execute { assertEquals(recipe, bucketList.getRecipe()) }
        if (byBackPress) {
            onView(withId(R.id.button_close)).perform(click())
        } else {
            Espresso.pressBack()
        }
        onView(withId(R.id.two_options_dialog_layout)).check(isNotDisplayed())
        verifyRecipeDisplayingState(recipe)
        // BucketList expected to be cleaned
        mainThreadExecutor.execute { Assert.assertNotEquals(recipe, bucketList.getRecipe()) }
        mainThreadExecutor.execute { assertEquals("", bucketList.getName()) }
    }

    @Test
    fun cancelRecipeCreationWithoutRecipeModification_byCloseButton() {
        cancelRecipeCreationWithoutRecipeModification(false)
    }

    @Test
    fun cancelRecipeCreationWithoutRecipeModification_byBackPress() {
        cancelRecipeCreationWithoutRecipeModification(true)
    }

    private fun cancelRecipeCreationWithoutRecipeModification(byBackPress: Boolean) {
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        mainThreadExecutor.execute { bucketList.setRecipe(recipe) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        verifyRecipeCreatingState(recipe)
        mainThreadExecutor.execute { assertEquals(recipe, bucketList.getRecipe()) }
        if (byBackPress) {
            onView(withId(R.id.button_close)).perform(click())
        } else {
            Espresso.pressBack()
        }
        onView(withId(R.id.two_options_dialog_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.positive_button)).perform(click())

        // BucketList expected to be cleaned
        mainThreadExecutor.execute { Assert.assertNotEquals(recipe, bucketList.getRecipe()) }
        mainThreadExecutor.execute { assertEquals("", bucketList.getName()) }
        // Activtiy expected to be finished
        assertEquals(Activity.RESULT_CANCELED.toLong(), activityRule.activityResult.resultCode.toLong())
    }

    @Test
    fun openWithEditedExistingRecipeInBucketList() {
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")),
                "novel comment")
        mainThreadExecutor.execute { bucketList.setRecipe(recipe) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        verifyRecipeEditingState(recipe)
    }

    @Test
    fun cancelRecipeEditing_whenOpenedWithAlreadyChangedEditedRecipe() {
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val initialRecipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")),
                "comment")
        val changedRecipe = Recipe(
                initialRecipe.id,
                initialRecipe.foodstuff.recreateWithName("novel cake"),
                initialRecipe.ingredients,
                123f,
                "novel comment")
        mainThreadExecutor.execute { bucketList.setRecipe(changedRecipe) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        verifyRecipeEditingState(changedRecipe)
        onView(withId(R.id.button_close)).perform(click())
        // Cancellation dialog is expected
        onView(withText(R.string.cancel_recipe_editing_dialog_title)).check(matches(isDisplayed()))
        onView(withId(R.id.positive_button)).perform(click())
        verifyRecipeDisplayingState(initialRecipe)
    }

    @Test
    fun addIngredientButtonBehaviour_onRecipeCreation() {
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val notSavedRecipe = createRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        mainThreadExecutor.execute { bucketList.setRecipe(notSavedRecipe) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        onView(withId(R.id.bucket_list_add_ingredient_button)).perform(click())
        // Button closes activity
        assertEquals(Activity.RESULT_CANCELED.toLong(), activityRule.activityResult.resultCode.toLong())
        // But doesn't clean BucketList
        mainThreadExecutor.execute { assertEquals(notSavedRecipe, bucketList.getRecipe()) }
    }

    @Test
    fun addIngredientButtonBehaviour_onRecipeEditing() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)
        onView(withId(R.id.button_edit)).perform(click())
        onView(withId(R.id.bucket_list_add_ingredient_button)).perform(click())
        // Button closes activity
        assertEquals(Activity.RESULT_CANCELED.toLong(), activityRule.activityResult.resultCode.toLong())
        // But doesn't clean BucketList
        mainThreadExecutor.execute { assertEquals(recipe, bucketList.getRecipe()) }
    }

    @Test
    fun saveAsRecipeButtonEnabledAndDisabledStates() {
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val notSavedRecipe = createRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        mainThreadExecutor.execute { bucketList.setRecipe(notSavedRecipe) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        verifyRecipeCreatingState(notSavedRecipe)
        onView(withId(R.id.save_as_recipe_button)).check(matches(isEnabled()))

        // Name
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText(""))
        onView(withId(R.id.save_as_recipe_button)).check(matches(not(isEnabled())))
        onView(withId(R.id.recipe_name_edit_text)).perform(replaceText("novel cake"))
        onView(withId(R.id.save_as_recipe_button)).check(matches(isEnabled()))

        // Weight
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText(""))
        onView(withId(R.id.save_as_recipe_button)).check(matches(not(isEnabled())))
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("321"))
        onView(withId(R.id.save_as_recipe_button)).check(matches(isEnabled()))

        // Ingredients
        onView(withText("dough")).perform(longClick())
        onView(withText(R.string.delete_ingredient)).perform(click())
        onView(withId(R.id.save_as_recipe_button)).check(matches(isEnabled()))
        onView(withText("oil")).perform(longClick())
        onView(withText(R.string.delete_ingredient)).perform(click())
        onView(withId(R.id.save_as_recipe_button)).check(matches(not(isEnabled())))
    }

    @Test
    fun saveAsRecipeButtonEnabled_whenBucketListOpenedWithFilledCreatingRecipe() {
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val notSavedRecipe = createRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        mainThreadExecutor.execute { bucketList.setRecipe(notSavedRecipe) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        verifyRecipeCreatingState(notSavedRecipe)
        onView(withId(R.id.save_as_recipe_button)).check(matches(isEnabled()))
    }

    @Test
    fun saveAsRecipeButtonNotEnabled_whenBucketListOpenedWithCreatingRecipe_withoutName() {
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val notSavedRecipe = createRecipe(
                "", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        mainThreadExecutor.execute { bucketList.setRecipe(notSavedRecipe) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        verifyRecipeCreatingState(notSavedRecipe)
        onView(withId(R.id.save_as_recipe_button)).check(matches(not(isEnabled())))
    }

    @Test
    fun saveAsRecipeButtonNotEnabled_whenBucketListOpenedWithCreatingRecipe_withoutIngredients() {
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val notSavedRecipe = createRecipe("cake", 123, emptyList())
        mainThreadExecutor.execute { bucketList.setRecipe(notSavedRecipe) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)
        verifyRecipeCreatingState(notSavedRecipe)
        onView(withId(R.id.save_as_recipe_button)).check(matches(not(isEnabled())))
    }

    @Test
    fun commentBehaviour_duringRecipeCreation() {
        // Start creating recipe (put not saved recipe into bucket list)
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")),
                comment = "")
        mainThreadExecutor.execute { bucketList.setRecipe(recipe) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)

        verifyRecipeCreatingState(recipe)

        onView(withId(R.id.add_comment_button)).check(matches(isDisplayed()))
        onView(withId(R.id.comment_title)).check(matches(isDisplayed()))
        // Comment is empty
        onView(withId(R.id.comment)).check(isNotDisplayed())

        // Comment becomes visible after comment button click
        onView(withId(R.id.add_comment_button)).perform(click())
        onView(withId(R.id.comment)).check(matches(isDisplayed()))

        // When empty, comment becomes invisible again after it loses focus
        onView(withId(R.id.recipe_name_edit_text)).perform(click())
        onView(withId(R.id.comment)).check(isNotDisplayed())

        // When not empty, comment is visible even when loses focus
        onView(withId(R.id.add_comment_button)).perform(click())
        onView(withId(R.id.comment)).perform(replaceText("novel comment"))
        onView(withId(R.id.recipe_name_edit_text)).perform(click())
        onView(withId(R.id.comment)).check(matches(isDisplayed()))

        Espresso.closeSoftKeyboard()
        onView(withId(R.id.save_as_recipe_button)).perform(click())
        val resultIntent = activityRule.activityResult.resultData
        val resultRecipe = resultIntent.getParcelableExtra<Recipe>(EXTRA_PRODUCED_RECIPE)
        assertEquals("novel comment", resultRecipe.comment)
    }

    @Test
    fun commentBehaviour_duringRecipeEditing() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")),
                "comment")
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)

        // Verify initial filled state
        verifyRecipeDisplayingState(recipe)
        onView(withId(R.id.add_comment_button)).check(isNotDisplayed())
        onView(withId(R.id.comment_title)).check(matches(isDisplayed()))
        onView(withId(R.id.comment)).check(matches(isDisplayed()))
        onView(withId(R.id.comment)).check(matches(withText("comment")))

        // Verify initial editing state
        onView(withId(R.id.button_edit)).perform(click())
        verifyRecipeEditingState(recipe)
        onView(withId(R.id.add_comment_button)).check(matches(isDisplayed()))
        onView(withId(R.id.comment_title)).check(matches(isDisplayed()))
        onView(withId(R.id.comment)).check(matches(isDisplayed()))
        onView(withId(R.id.comment)).check(matches(withText("comment")))

        // Edit, save, verify the edit is applied
        onView(withId(R.id.comment)).perform(replaceText("novel comment"))
        onView(withId(R.id.save_as_recipe_button)).perform(click())
        verifyRecipeDisplayingState(recipe.copy(comment = "novel comment"))
        onView(withId(R.id.add_comment_button)).check(isNotDisplayed())
        onView(withId(R.id.comment_title)).check(matches(isDisplayed()))
        onView(withId(R.id.comment)).check(matches(isDisplayed()))
        onView(withId(R.id.comment)).check(matches(withText("novel comment")))

        // Edit, set comment empty, verify empty comment displaying
        onView(withId(R.id.button_edit)).perform(click())
        onView(withId(R.id.comment)).perform(click())
        onView(withId(R.id.comment)).perform(replaceText(""))
        // Still have focus
        onView(withId(R.id.add_comment_button)).check(matches(isDisplayed()))
        onView(withId(R.id.comment_title)).check(matches(isDisplayed()))
        onView(withId(R.id.comment)).check(matches(isDisplayed()))
        onView(withId(R.id.comment)).check(matches(withText("")))
        // Lose focus
        onView(withId(R.id.recipe_name_edit_text)).perform(click())
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.add_comment_button)).check(matches(isDisplayed()))
        onView(withId(R.id.comment_title)).check(matches(isDisplayed()))
        onView(withId(R.id.comment)).check(isNotDisplayed())

        // Save and verify displaying empty comment
        onView(withId(R.id.save_as_recipe_button)).perform(click())
        verifyRecipeDisplayingState(recipe.copy(comment = ""))
        onView(withId(R.id.add_comment_button)).check(isNotDisplayed())
        onView(withId(R.id.comment_title)).check(isNotDisplayed())
        onView(withId(R.id.comment)).check(isNotDisplayed())

        // Verify saved recipe comment
        val allRecipes: List<Recipe> = ArrayList(recipesRepository.getAllRecipesRx().blockingGet())
        assertEquals(1, allRecipes.size.toLong())
        assertEquals("", allRecipes[0].comment)
    }

    @Test
    fun commentIsTrimmed() {
        // Start creating recipe (put not saved recipe into bucket list)
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")),
                comment = "comment")
        mainThreadExecutor.execute { bucketList.setRecipe(recipe) }
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext())
        activityRule.launchActivity(startIntent)

        verifyRecipeCreatingState(recipe)

        onView(withId(R.id.comment)).check(matches(isDisplayed()))
        onView(withId(R.id.comment)).perform(replaceText("    "))

        // Change focus and verify that comment is still displayed - even
        // though we will trim the whitespaces, we will do that later (so we don't
        // disturb user's intention by editing what they're typing in).
        onView(withId(R.id.recipe_name_edit_text)).perform(click())
        onView(withId(R.id.comment)).check(matches(isDisplayed()))
        onView(withId(R.id.comment)).check(matches(withText("    ")))

        Espresso.closeSoftKeyboard()
        onView(withId(R.id.save_as_recipe_button)).perform(click())
        val resultIntent = activityRule.activityResult.resultData
        val resultRecipe = resultIntent.getParcelableExtra<Recipe>(EXTRA_PRODUCED_RECIPE)
        // Saved recipe has empty comment, even though we tried to put whitespaces
        assertEquals("", resultRecipe.comment)
    }

    @Test
    fun ingredientCommentBehaviour() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111", ""), UIIngredient("oil", "222", "")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)

        verifyRecipeDisplayingState(recipe)

        onView(withId(R.id.button_edit)).perform(click())

        verifyRecipeEditingState(recipe)

        onView(withText("dough")).perform(longClick())
        onView(withText(R.string.edit_ingredient_comment)).perform(click())
        onView(withId(R.id.comment)).perform(replaceText("novel comment"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.save_button)).perform(click())

        verifyRecipeEditingState(
                recipe.copy(ingredients = listOf(
                        recipe.ingredients[0].copy(comment = "novel comment"),
                        recipe.ingredients[1])))
        onView(withId(R.id.save_as_recipe_button)).perform(click())
        verifyRecipeDisplayingState(
                recipe.copy(ingredients = listOf(
                        recipe.ingredients[0].copy(comment = "novel comment"),
                        recipe.ingredients[1])))

        // Verify saved ingredient comment
        val allRecipes: List<Recipe> = ArrayList(recipesRepository.getAllRecipesRx().blockingGet())
        assertEquals(1, allRecipes.size.toLong())
        val savedIngredient = allRecipes[0].ingredients.find { it.foodstuff.name == "dough" }!!
        assertEquals("novel comment", savedIngredient.comment)
    }

    @Test
    fun ingredientCommentIsTrimmed() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111", "comment"), UIIngredient("oil", "222", "")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)

        verifyRecipeDisplayingState(recipe)

        onView(withId(R.id.button_edit)).perform(click())

        verifyRecipeEditingState(recipe)

        onView(withText("dough")).perform(longClick())
        onView(withText(R.string.edit_ingredient_comment)).perform(click())
        onView(withId(R.id.comment)).perform(replaceText("      "))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.save_button)).perform(click())

        // Whitespaces erased
        val expectedIngredients = listOf(
                recipe.ingredients[0].copy(comment = ""),
                recipe.ingredients[1])
        verifyRecipeEditingState(recipe.copy(ingredients = expectedIngredients))

        onView(withId(R.id.save_as_recipe_button)).perform(click())
        verifyRecipeDisplayingState(recipe.copy(ingredients = expectedIngredients))

        // Verify saved ingredient comment
        val allRecipes: List<Recipe> = ArrayList(recipesRepository.getAllRecipesRx().blockingGet())
        assertEquals(1, allRecipes.size.toLong())
        val savedIngredient = allRecipes[0].ingredients.find { it.foodstuff.name == "dough" }!!
        assertEquals("", savedIngredient.comment)
    }

    @Test
    fun ingredientCommentDialog_restoredOnSaveAndRestoreInstanceState() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111", ""), UIIngredient("oil", "222", "")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)

        verifyRecipeDisplayingState(recipe)

        onView(withId(R.id.button_edit)).perform(click())
        verifyRecipeEditingState(recipe)

        onView(withText("dough")).perform(longClick())
        onView(withText(R.string.edit_ingredient_comment)).perform(click())
        onView(withId(R.id.comment)).perform(replaceText("novel comment"))

        // Recreate activity!
        mainThreadExecutor.execute { activityRule.activity.recreate() }

        Espresso.closeSoftKeyboard()
        onView(withId(R.id.save_button)).perform(click())

        val expectedIngredients = listOf(
                recipe.ingredients[0].copy(comment = "novel comment"),
                recipe.ingredients[1])
        verifyRecipeEditingState(recipe.copy(ingredients = expectedIngredients))

        onView(withId(R.id.save_as_recipe_button)).perform(click())
        verifyRecipeDisplayingState(recipe.copy(ingredients = expectedIngredients))

        // Verify saved ingredient comment
        val allRecipes: List<Recipe> = ArrayList(recipesRepository.getAllRecipesRx().blockingGet())
        assertEquals(1, allRecipes.size.toLong())
        val savedIngredient = allRecipes[0].ingredients.find { it.foodstuff.name == "dough" }!!
        assertEquals("novel comment", savedIngredient.comment)
    }

    @Test
    fun ingredientsRearranging() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 123,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)

        verifyRecipeDisplayingState(recipe)

        val oilDragHandleParent = allOf(
                hasDescendant(withText("oil")),
                withId(R.id.bucket_list_ingredient_layout))
        val oilDragHandle = allOf(
                withParent(oilDragHandleParent),
                withId(R.id.drag_handle))

        // Cannot drag and drop initially
        onView(oilDragHandle).check(isNotDisplayed())

        onView(withId(R.id.button_edit)).perform(click())
        onView(oilDragHandle).check(matches(isDisplayed()))

        // Drag and drop!
        onView(oilDragHandle).perform(swipeToTop())

        // Verify changed order
        val expectedIngredients = recipe.ingredients.asReversed()
        verifyRecipeEditingState(recipe.copy(ingredients = expectedIngredients))
        onView(withId(R.id.save_as_recipe_button)).perform(click())
        verifyRecipeDisplayingState(recipe.copy(ingredients = expectedIngredients))
    }

    @Test
    fun cookingMode() {
        try {
            // Create recipe
            clearAllData(foodstuffsList, historyWorker, databaseHolder)
            val initialRecipe = createSavedRecipe(
                    "cake", 333,
                    listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
            val recipeDividedBy10 = createSavedRecipe(
                    "cake", 33,
                    listOf(UIIngredient("dough", "11"), UIIngredient("oil", "22")))
            val recipe0 = createSavedRecipe(
                    "cake", 0,
                    listOf(UIIngredient("dough", "0"), UIIngredient("oil", "0")))
            val startIntent = createIntent(
                    InstrumentationRegistry.getTargetContext(),
                    initialRecipe)
            activityRule.launchActivity(startIntent)

            verifyRecipeDisplayingState(initialRecipe)

            // Switch and verify state
            onView(withId(R.id.button_cooking)).perform(click())
            verifyCookingState(initialRecipe)

            // Divide total weight by 10
            onView(withId(R.id.total_weight_edit_text)).perform(replaceText("33"))
            verifyCookingState(recipeDividedBy10)
            // Return original weight
            onView(withId(R.id.total_weight_edit_text)).perform(replaceText("333"))
            verifyCookingState(initialRecipe)

            // Divide dough weight by 10
            onView(allOf(
                    withParent(hasDescendant(withText("dough"))),
                    withId(R.id.extra_info_block_editable)))
                    .perform(replaceText("11"))
            verifyCookingState(recipeDividedBy10)
            // Return original dough weight
            onView(allOf(
                    withParent(hasDescendant(withText("dough"))),
                    withId(R.id.extra_info_block_editable)))
                    .perform(replaceText("111"))
            verifyCookingState(initialRecipe)

            // Set total weight to 0
            onView(withId(R.id.total_weight_edit_text)).perform(replaceText("0"))
            verifyCookingState(recipe0)

            // Exit the cooking mode and verify that the weight are unchanged
            onView(withId(R.id.button_close)).perform(click())
            verifyRecipeDisplayingState(initialRecipe)
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun cookingModeExiting() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val recipe = createSavedRecipe(
                "cake", 333,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                recipe)
        activityRule.launchActivity(startIntent)

        onView(withId(R.id.button_cooking)).perform(click())
        verifyCookingState(recipe)

        // Exit by the close button
        onView(withId(R.id.button_close)).perform(click())
        verifyRecipeDisplayingState(recipe)

        onView(withId(R.id.button_cooking)).perform(click())
        verifyCookingState(recipe)

        // Exit by back press
        Espresso.pressBack()
        verifyRecipeDisplayingState(recipe)
    }

    @Test
    fun cookingMode_stateRestore() {
        // Create recipe
        clearAllData(foodstuffsList, historyWorker, databaseHolder)
        val initialRecipe = createSavedRecipe(
                "cake", 333,
                listOf(UIIngredient("dough", "111"), UIIngredient("oil", "222")))
        val recipeDividedBy10 = createSavedRecipe(
                "cake", 33,
                listOf(UIIngredient("dough", "11"), UIIngredient("oil", "22")))
        val startIntent = createIntent(
                InstrumentationRegistry.getTargetContext(),
                initialRecipe)
        activityRule.launchActivity(startIntent)

        verifyRecipeDisplayingState(initialRecipe)

        // Switch and verify state
        onView(withId(R.id.button_cooking)).perform(click())
        verifyCookingState(initialRecipe)

        // Divide total weight by 10
        onView(withId(R.id.total_weight_edit_text)).perform(replaceText("33"))
        verifyCookingState(recipeDividedBy10)

        // Recreate activity
        mainThreadExecutor.execute { activityRule.activity.recreate() }
        verifyCookingState(recipeDividedBy10)

        // Exit cooking mode
        onView(withId(R.id.button_close)).perform(click())
        verifyRecipeDisplayingState(initialRecipe)
    }

    private fun createSavedRecipe(
            name: String,
            weight: Int,
            ingredients: List<UIIngredient>,
            comment: String = ""): Recipe {
        val notSavedRecipe = createRecipe(name, weight, ingredients, comment)
        val recipeResult = recipesRepository.saveRecipeRx(notSavedRecipe).blockingGet()
        return (recipeResult as CreateRecipeResult.Ok).recipe
    }

    private fun createRecipe(
            name: String,
            weight: Int,
            uiIngredients: List<UIIngredient>,
            comment: String = "comment"): Recipe {
        val ingredients: MutableList<Ingredient> = ArrayList()
        for (uiIngredient in uiIngredients) {
            var ingredient = Foodstuff.withName(uiIngredient.name).withNutrition(1f, 2f, 3f, 4f)
            ingredient = foodstuffsList.saveFoodstuff(ingredient).blockingGet()
            ingredients.add(create(ingredient, uiIngredient.weight.toFloat(), uiIngredient.comment))
        }
        val foodstuff = Foodstuff.withName(name).withNutrition(1f, 2f, 3f, 4f)
        return Recipe.create(foodstuff, ingredients, weight.toFloat(), comment).recalculateNutrition()
    }

    private fun verifyRecipeDisplayingState(recipe: Recipe) {
        onView(withId(R.id.button_edit)).check(matches(isDisplayed()))
        onView(withId(R.id.button_cooking)).check(matches(isDisplayed()))
        onView(withId(R.id.title_text)).check(matches(withText(R.string.bucket_list_title_recipe)))
        onView(withId(R.id.save_as_recipe_button)).check(isNotDisplayed())
        onView(withId(R.id.recipe_name_edit_text)).check(matches(withText(recipe.name)))
        onView(withId(R.id.recipe_name_edit_text)).check(matches(not(isEnabled())))
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText(
                toDecimalString(recipe.weight))))
        onView(withId(R.id.total_weight_edit_text)).check(matches(not(isEnabled())))
        onView(withId(R.id.bucket_list_add_ingredient_button)).check(isNotDisplayed())
        verifyDisplayedIngredients(recipe.ingredients)

        onView(withId(R.id.add_comment_button)).check(isNotDisplayed())
        if (recipe.comment.isEmpty()) {
            onView(withId(R.id.comment_title)).check(isNotDisplayed())
            onView(withId(R.id.comment)).check(isNotDisplayed())
        } else {
            onView(withId(R.id.comment_title)).check(matches(isDisplayed()))
            onView(withId(R.id.comment)).check(matches(withText(recipe.comment)))
        }
        verifyNutritionSummary(recipe)
    }

    private fun verifyDisplayedIngredients(
            expectedIngredients: List<Ingredient>,
            editableWeight: Boolean = false) {
        expectedIngredients.forEachIndexed { index, ingredient ->
            val previous = if (index != 0) {
                expectedIngredients[index - 1]
            } else {
                null
            }
            val next = if (index < expectedIngredients.size - 1) {
                expectedIngredients[index + 1]
            } else {
                null
            }
            checkDisplayStatus(ingredient.intoUI(), previous?.intoUI(), next?.intoUI(), editableWeight)
        }
    }

    private fun checkDisplayStatus(ingredient: UIIngredient,
                                   previousIngredient: UIIngredient?,
                                   nextIngredient: UIIngredient?,
                                   expectedEditableWeight: Boolean) {
        val expectedGramsTextId = if (expectedEditableWeight) {
            R.id.extra_info_block_editable
        } else {
            R.id.extra_info_block
        }
        val expectedGramsText = if (expectedEditableWeight) {
            ingredient.weight.toInt().toString()
        } else {
            activityRule.activity.getString(
                    R.string.n_gramms,
                    ingredient.weight.toInt())
        }
        val gramsMatcher = hasDescendant(allOf(
            withText(expectedGramsText),
            withId(expectedGramsTextId),
            isDisplayed()))

        val commentMatcher = if (!TextUtils.isEmpty(ingredient.comment)) {
            hasDescendant(withText(ingredient.comment))
        } else {
            any(View::class.java)
        }
        val previousIngredientMatcher = if (previousIngredient != null) {
            matches(isCompletelyBelow(allOf(
                    hasDescendant(withText(previousIngredient.name)),
                    withId(R.id.bucket_list_ingredient_layout))))
        } else {
            any(View::class.java)
        }
        val nextIngredientMatcher = if (nextIngredient != null) {
            matches(isCompletelyAbove(allOf(
                    hasDescendant(withText(nextIngredient.name)),
                    withId(R.id.bucket_list_ingredient_layout))))
        } else {
            any(View::class.java)
        }

        val targetIngredientMatcher = allOf(
                hasDescendant(withText(ingredient.name)),
                gramsMatcher,
                withId(R.id.bucket_list_ingredient_layout),
                commentMatcher,
                previousIngredientMatcher,
                nextIngredientMatcher)
        onView(targetIngredientMatcher).check(matches(isDisplayed()))

        if (previousIngredient == null) {
            onView(allOf(
                    withId(R.id.bucket_list_ingredient_layout),
                    matches(isCompletelyAbove(targetIngredientMatcher))))
                    .check(doesNotExist())
        }
        if (nextIngredient == null) {
            onView(allOf(
                    withId(R.id.bucket_list_ingredient_layout),
                    matches(isCompletelyBelow(targetIngredientMatcher))))
                    .check(doesNotExist())
        }
    }

    private fun verifyNutritionSummary(recipe: Recipe) {
        onView(allOf(
                withParent(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText(
                        toDecimalString(recipe.foodstuff.protein))))

        onView(allOf(
                withParent(withId(R.id.fats_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText(
                        toDecimalString(recipe.foodstuff.fats))))

        onView(allOf(
                withParent(withId(R.id.carbs_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText(
                        toDecimalString(recipe.foodstuff.carbs))))

        onView(allOf(
                withParent(withId(R.id.calories_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText(
                        toDecimalString(recipe.foodstuff.calories))))
    }

    private fun verifyRecipeEditingState(recipe: Recipe) {
        onView(withId(R.id.button_edit)).check(isNotDisplayed())
        onView(withId(R.id.button_cooking)).check(isNotDisplayed())
        onView(withId(R.id.title_text)).check(matches(withText(R.string.bucket_list_title_recipe_editing)))
        onView(withId(R.id.save_as_recipe_button)).check(matches(isDisplayed()))
        onView(withId(R.id.recipe_name_edit_text)).check(matches(withText(recipe.name)))
        onView(withId(R.id.recipe_name_edit_text)).check(matches(isEnabled()))
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText(
                toDecimalString(recipe.weight))))
        onView(withId(R.id.total_weight_edit_text)).check(matches(isEnabled()))
        onView(withId(R.id.bucket_list_add_ingredient_button)).check(matches(isDisplayed()))
        verifyDisplayedIngredients(recipe.ingredients)

        onView(withId(R.id.add_comment_button)).check(matches(isDisplayed()))
        onView(withId(R.id.comment_title)).check(matches(isDisplayed()))
        if (recipe.comment.isEmpty()) {
            onView(withId(R.id.comment)).check(isNotDisplayed())
        } else {
            // If expected comment is empty, the comment view could be both
            // visible and not visible in the current state
        }
        verifyNutritionSummary(recipe)
    }

    private fun verifyRecipeCreatingState(recipe: Recipe) {
        onView(withId(R.id.button_edit)).check(isNotDisplayed())
        onView(withId(R.id.button_cooking)).check(isNotDisplayed())
        onView(withId(R.id.title_text)).check(matches(withText(R.string.bucket_list_title_recipe_creation)))
        onView(withId(R.id.save_as_recipe_button)).check(matches(isDisplayed()))
        onView(withId(R.id.recipe_name_edit_text)).check(matches(withText(recipe.name)))
        onView(withId(R.id.recipe_name_edit_text)).check(matches(isEnabled()))
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText(
                toDecimalString(recipe.weight))))
        onView(withId(R.id.total_weight_edit_text)).check(matches(isEnabled()))
        onView(withId(R.id.bucket_list_add_ingredient_button)).check(matches(isDisplayed()))
        verifyDisplayedIngredients(recipe.ingredients)

        onView(withId(R.id.add_comment_button)).check(matches(isDisplayed()))
        onView(withId(R.id.comment_title)).check(matches(isDisplayed()))
        if (recipe.comment.isEmpty()) {
            onView(withId(R.id.comment)).check(isNotDisplayed())
        } else {
            // If expected comment is empty, the comment view could be both
            // visible and not visible in the current state
        }
        verifyNutritionSummary(recipe)
    }

    private fun verifyCookingState(recipe: Recipe) {
        onView(withId(R.id.button_edit)).check(isNotDisplayed())
        onView(withId(R.id.button_cooking)).check(isNotDisplayed())
        onView(withId(R.id.title_text)).check(matches(withText(R.string.bucket_list_title_cooking)))
        onView(withId(R.id.save_as_recipe_button)).check(isNotDisplayed())
        onView(withId(R.id.recipe_name_edit_text)).check(matches(withText(recipe.name)))
        onView(withId(R.id.recipe_name_edit_text)).check(matches(not(isEnabled())))
        onView(withId(R.id.total_weight_edit_text)).check(matches(withText(
                toDecimalString(recipe.weight))))
        onView(withId(R.id.total_weight_edit_text)).check(matches(isEnabled()))
        onView(withId(R.id.bucket_list_add_ingredient_button)).check(isNotDisplayed())
        verifyDisplayedIngredients(recipe.ingredients, editableWeight = true)

        onView(withId(R.id.add_comment_button)).check(isNotDisplayed())
        if (recipe.comment.isEmpty()) {
            onView(withId(R.id.comment_title)).check(isNotDisplayed())
            onView(withId(R.id.comment)).check(isNotDisplayed())
        } else {
            onView(withId(R.id.comment_title)).check(matches(isDisplayed()))
            onView(withId(R.id.comment)).check(matches(withText(recipe.comment)))
        }
        verifyNutritionSummary(recipe)
    }

    private fun swipeToTop(): ViewAction {
        return GeneralSwipeAction(Swipe.SLOW,
                GeneralLocation.CENTER,
                CoordinatesProvider { view ->
                    val coordinates = GeneralLocation.CENTER.calculateCoordinates(view)
                    coordinates[1] = 0f
                    coordinates
                }, Press.FINGER)
    }

}