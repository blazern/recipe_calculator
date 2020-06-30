package korablique.recipecalculator.ui.mainactivity

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtras
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import korablique.recipecalculator.R
import korablique.recipecalculator.database.CreateRecipeResult
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.ui.bucketlist.BucketListActivity.Companion.createIntent
import korablique.recipecalculator.util.EspressoUtils
import korablique.recipecalculator.util.EspressoUtils.hasValueRecursive
import korablique.recipecalculator.util.EspressoUtils.matches
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityModeDefaultTest : MainActivityTestsBase() {
    @Test
    fun cardButtons_whenSimpleFoodstuffClicked() {
        mActivityRule.launchActivity(null)

        // Клик на продукт
        onView(CoreMatchers.allOf(
                withText(foodstuffs[0].name),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header)))))
                .perform(click())

        // "В журнал" должна быть видна
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1)))
                .check(matches(isDisplayed()))
        // И должна иметь правильный текст
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1)))
                .check(matches(withText(R.string.add_foodstuff_to_history)))
        // И должна быть disabled из-за пустой массы
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1)))
                .check(matches(not(isEnabled())))

        // Второй кнопки не должно быть
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2)))
                .check(matches(not(isDisplayed())))

        // Введём массу
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"))
        // "В журнал" должна стать enabled
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1)))
                .check(matches(isEnabled()))
    }

    @Test
    fun cardButtons_whenRecipeFoodstuffClicked() {
        val recipeFoodstuff = Foodstuff.withName("111recipe").withNutrition(1f, 2f, 3f, 4f)
        val recipe = runBlocking {
            val result = recipesRepository.createRecipe(
                    recipeFoodstuff,
                    listOf(Ingredient.create(foodstuffs[0], 123f, "")),
                    "",
                    123f) as CreateRecipeResult.Ok
            result.recipe
        }

        mActivityRule.launchActivity(null)

        // Клик на продукт
        onView(CoreMatchers.allOf(
                withText(recipe.name),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header)))))
                .perform(click())

        // "В журнал" должна быть видна
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1)))
                .check(matches(isDisplayed()))
        // И должна иметь правильный текст
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1)))
                .check(matches(withText(R.string.add_foodstuff_to_history)))
        // И должна быть disabled из-за пустой массы
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1)))
                .check(matches(not(isEnabled())))

        // "Рецепт" должна быть видна
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2)))
                .check(matches(isDisplayed()))
        // И должна иметь правильный текст
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2)))
                .check(matches(withText(R.string.open_foodstuff_recipe)))
        // И должна быть enabled, потому что не зависит от массы
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2)))
                .check(matches(isEnabled()))

        // Введём массу
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"))
        // "В журнал" должна стать enabled
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1)))
                .check(matches(isEnabled()))
        // "Рецепт" должна быть по-прежнему enabled
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2)))
                .check(matches(isEnabled()))
    }

    @Test
    fun recipeButtonClick() {
        val recipeFoodstuff = Foodstuff.withName("111recipe").withNutrition(1f, 2f, 3f, 4f)
        val recipe = runBlocking {
            val result = recipesRepository.createRecipe(
                    recipeFoodstuff,
                    listOf(Ingredient.create(foodstuffs[0], 123f, "")),
                    "",
                    123f) as CreateRecipeResult.Ok
            result.recipe
        }

        mActivityRule.launchActivity(null)

        // Клик на продукт
        onView(CoreMatchers.allOf(
                withText(recipe.name),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header)))))
                .perform(click())
        // Клик на "Рецепт"
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2)))
                .perform(click())

        val expectedIntent = createIntent(context, recipe, editRecipe = false)
        intended(CoreMatchers.allOf(
                hasAction(expectedIntent.action),
                hasComponent(expectedIntent.component),
                hasExtras(hasValueRecursive(expectedIntent.extras))))

        // Карточка не должна быть закрыта при возврате на гланвый экран,
        // потому что рецепт просто был просмотрен
        Espresso.pressBack()
        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
    }
}
