package korablique.recipecalculator.ui.mainactivity;

import androidx.test.espresso.Espresso;

import org.junit.After;
import org.junit.Test;

import korablique.recipecalculator.R;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.util.EspressoUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsString;

public class MainActivitySearchTest extends MainActivityTestsBase {
    @Test
    public void transliteratedQueriesSearch() {
        Foodstuff foodstuff = Foodstuff.withName("шоколад Ritter Sport").withNutrition(1, 2, 3, 4);
        foodstuffsList.saveFoodstuff(foodstuff, new FoodstuffsList.SaveFoodstuffCallback() {
            @Override public void onResult(Foodstuff addedFoodstuff) {}
            @Override public void onDuplication() {}
        });

        mActivityRule.launchActivity(null);

        onView(allOf(
                isDescendantOfA(withId(R.id.search_layout)),
                withText(containsString("Ritter Sport")))).check(doesNotExist());

        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText("риттер спорт"));

        onView(allOf(
                isDescendantOfA(withId(R.id.search_layout)),
                withText(containsString("Ritter Sport")))).check(matches(isDisplayed()));

        Espresso.closeSoftKeyboard();
    }

    @Test
    public void deletingFromSearchResultsWorks() {
        mActivityRule.launchActivity(null);

        Foodstuff searchingFoodstuff = foodstuffs[0];
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(searchingFoodstuff.getName()));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter
        // нажимаем на результат поиска
        onView(allOf(
                withText(searchingFoodstuff.getName()),
                isDescendantOfA(withId(R.id.search_results_recycler_view)),
                EspressoUtils.matches(isCompletelyBelow(withId(R.id.add_new_foodstuff_button))))).perform(click());
        // удаляем его
        onView(withId(R.id.button_edit)).perform(click());
        onView(withId(R.id.button_delete)).perform(click());
        onView(withId(R.id.positive_button)).perform(click());
        // нужно проверять не только текст, но и родителя,
        // т к иначе в проверку попадут вьюшки из MainScreen
        onView(allOf(
                withText(searchingFoodstuff.getName()),
                isDescendantOfA(withId(R.id.search_results_recycler_view)),
                EspressoUtils.matches(isCompletelyBelow(withId(R.id.add_new_foodstuff_button)))))
                .check(doesNotExist());
    }

    @Test
    public void whenSavingNewFoodstuffFromSearchResultsItAppearsInSearchResults() {
        mActivityRule.launchActivity(null);

        Foodstuff newFoodstuff = Foodstuff.withName("granola").withNutrition(10, 10, 60, 450);
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(newFoodstuff.getName()));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter
        mainThreadExecutor.execute(() -> {
            foodstuffsList.saveFoodstuff(newFoodstuff, new FoodstuffsList.SaveFoodstuffCallback() {
                @Override
                public void onResult(Foodstuff addedFoodstuff) {}

                @Override
                public void onDuplication() {}
            });
        });
        onView(allOf(
                withText(newFoodstuff.getName()),
                EspressoUtils.matches(isCompletelyBelow(withId(R.id.add_new_foodstuff_button))),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void searchQueryCleaned_whenFocusedLost() {
        mActivityRule.launchActivity(null);

        // Клик на строку поиска и ввод строки поиска
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText("word"));
        // Убеждаемся, что текст на месте
        onView(withHint(R.string.search)).check(matches(withText("word")));

        // Убираем фокус нажатием на Back и проверяем, что текст пропал
        Espresso.pressBack();
        onView(withHint(R.string.search)).check(matches(withText("")));
    }

    @Test
    public void searchQueryNotCleaned_whenFocusedLost_whenSearchResultsArePresent() {
        mActivityRule.launchActivity(null);

        // Делаем поиск продукта
        Foodstuff searchingFoodstuff = foodstuffs[0];
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(searchingFoodstuff.getName()));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter

        // Убеждаемся, что показаны результаты поиска
        onView(withId(R.id.search_results_layout)).check(matches(isDisplayed()));

        // На всякий случай кликаем на строку поиска ещё раз, чтобы она точно была в фокусе
        onView(withHint(R.string.search)).perform(click());
        // Убираем фокус со строки поиска
        Espresso.pressBack();
        // Убеждаемся, что текст никуда не делся
        onView(withHint(R.string.search)).check(matches(withText(searchingFoodstuff.getName())));
    }

    @Test
    public void searchQueryCleaned_whenSearchResultsGone() {
        mActivityRule.launchActivity(null);

        // Делаем поиск продукта
        Foodstuff searchingFoodstuff = foodstuffs[0];
        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(searchingFoodstuff.getName()));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter

        // Убеждаемся, что показаны результаты поиска
        onView(withId(R.id.search_results_layout)).check(matches(isDisplayed()));

        // На всякий случай кликаем на строку поиска ещё раз, чтобы она точно была в фокусе
        onView(withHint(R.string.search)).perform(click());
        // Убираем фокус со строки поиска
        Espresso.pressBack();
        // Закрываем экран поиска
        Espresso.pressBack();
        // Убеждаемся, что результат поиска пропал
        onView(withId(R.id.search_results_layout)).check(doesNotExist());
        // Убеждаемся, что текст запроса пропал
        onView(withHint(R.string.search)).check(matches(withText("")));
    }

    @Test
    public void mainScreenDisplaysCard_afterFoodstuffCreation_fromSearchFragment() {
        mActivityRule.launchActivity(null);

        String name = "111first_foodstuff";
        onView(withText(name)).check(doesNotExist());

        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(name));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter

        // We expect the foodstuff to not exist yet
        onView(withId(R.id.nothing_found_view)).check(matches(isDisplayed()));
        onView(withId(R.id.add_new_foodstuff_button)).perform(click());

        onView(withId(R.id.foodstuff_name)).perform(replaceText(name));
        onView(withId(R.id.protein_value)).perform(replaceText("10"));
        onView(withId(R.id.fats_value)).perform(replaceText("10"));
        onView(withId(R.id.carbs_value)).perform(replaceText("10"));
        onView(withId(R.id.calories_value)).perform(replaceText("10"));
        onView(withId(R.id.save_button)).perform(click());

        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withText(name))).check(matches(isDisplayed()));
    }

    @Test
    public void searchResultsFragmentChangesResultDynamicallyWhenQueryChanges() {
        mActivityRule.launchActivity(null);

        // Banana!
        String name1 = "banana";
        onView(allOf(
                withText(name1),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(doesNotExist());

        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(name1));
        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter
        onView(allOf(
                withText(name1),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(matches(isDisplayed()));

        // Apple!
        String name2 = "apple";
        onView(allOf(
                withText(name2),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(doesNotExist());

        onView(withHint(R.string.search)).perform(click());
        onView(withHint(R.string.search)).perform(replaceText(name2));
        // No pressing enter!
//        onView(withHint(R.string.search)).perform(pressImeActionButton()); // enter

        // Apple is expected
        onView(allOf(
                withText(name2),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(matches(isDisplayed()));
        // Banana is not expected
        onView(allOf(
                withText(name1),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(doesNotExist());
    }
}
