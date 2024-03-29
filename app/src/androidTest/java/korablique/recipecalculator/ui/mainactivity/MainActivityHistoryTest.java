package korablique.recipecalculator.ui.mainactivity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.widget.DatePicker;

import androidx.fragment.app.Fragment;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import korablique.recipecalculator.R;
import korablique.recipecalculator.RequestCodes;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.util.EspressoUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;
import static korablique.recipecalculator.util.EspressoUtils.callActivityAndFragmentsOnPause;
import static korablique.recipecalculator.util.EspressoUtils.callActivityAndFragmentsOnResume;
import static korablique.recipecalculator.util.EspressoUtils.hasMaxProgress;
import static korablique.recipecalculator.util.EspressoUtils.hasProgress;
import static korablique.recipecalculator.util.EspressoUtils.isNotDisplayed;
import static korablique.recipecalculator.util.EspressoUtils.matches;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityHistoryTest extends MainActivityTestsBase {

    @Test
    public void todaysFoodstuffsDisplayedInHistory() {
        addFoodstuffsToday();
        mActivityRule.launchActivity(null);

        onView(withId(R.id.menu_item_history)).perform(click());

        Matcher<View> foodstuffBelowMatcher1 = allOf(
                withText(containsString(foodstuffs[6].getName())),
                matches(isCompletelyBelow(withId(R.id.title_layout))),
                isCompletelyDisplayed());
        onView(foodstuffBelowMatcher1).check(matches(isDisplayed()));

        Matcher<View> foodstuffBelowMatcher2 = allOf(
                withText(containsString(foodstuffs[5].getName())),
                matches(isCompletelyBelow(foodstuffBelowMatcher1)),
                isCompletelyDisplayed());
        onView(foodstuffBelowMatcher2).check(matches(isDisplayed()));

        Matcher<View> foodstuffBelowMatcher3 = allOf(
                withText(containsString(foodstuffs[0].getName())),
                matches(isCompletelyBelow(foodstuffBelowMatcher2)),
                isCompletelyDisplayed());
        onView(foodstuffBelowMatcher3).check(matches(isDisplayed()));
    }

    @Test
    public void deletingItemsInHistoryWorks() {
        addFoodstuffsToday();
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // нажать на элемент
        Foodstuff deletedFoodstuff = foodstuffs[0];
        onView(allOf(
                withText(containsString(deletedFoodstuff.getName())),
                isDescendantOfA(withId(R.id.history_list)),
                isCompletelyDisplayed())).perform(click());
        // нажать на кнопку удаления в карточке
        onView(withId(R.id.frame_layout_button_delete)).perform(click());
        // проверить, что элемент удалился
        onView(allOf(
                withText(containsString(deletedFoodstuff.getName())),
                isDescendantOfA(withId(R.id.history_list)),
                isCompletelyDisplayed())).check(doesNotExist());
        // проверить заголовок с БЖУ
        Nutrition totalNutrition = Nutrition.of(foodstuffs[5].withWeight(100))
                .plus(Nutrition.of(foodstuffs[6].withWeight(100)));
        onView(allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getProtein()))));
        onView(allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getFats()))));
        onView(allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));
        onView(allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCalories()))));
        // перезапустить активити и убедиться, что элемент удалён
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(withText(containsString(deletedFoodstuff.getName()))).check(doesNotExist());
        // ещё раз проверить заголовок
        onView(allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getProtein()))));
        onView(allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getFats()))));
        onView(allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));
        onView(allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCalories()))));
    }

    @Test
    public void editingItemsInHistoryWorks() {
        addFoodstuffsToday();
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // нажать на элемент
        Foodstuff editedFoodstuff = foodstuffs[0];
        onView(allOf(
                withText(containsString(editedFoodstuff.getName())),
                isCompletelyDisplayed())).perform(click());
        // отредактировать вес
        double newWeight = 200;
        onView(withId(R.id.weight_edit_text)).perform(replaceText(String.valueOf(newWeight)));
        onView(withId(R.id.button1)).perform(click());
        // проверить, что элемент отредактировался
        onView(allOf(
                withText(containsString(editedFoodstuff.getName())),
                isCompletelyDisplayed()))
                .check(matches(withText(containsString(toDecimalString(newWeight)))));
        // проверить заголовок с БЖУ
        Nutrition totalNutrition = Nutrition.of(editedFoodstuff.withWeight(newWeight))
                .plus(Nutrition.of(foodstuffs[5].withWeight(100)))
                .plus(Nutrition.of(foodstuffs[6].withWeight(100)));
        onView(allOf(
                withParent(allOf(withId(R.id.protein_layout), isCompletelyDisplayed())),
                withId(R.id.nutrition_text_view),
                isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getProtein()))));
        onView(allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getFats()))));
        onView(allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));
        onView(allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCalories()))));
        // перезапустить активити и убедиться, что элемент изменён
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(allOf(
                withText(containsString(editedFoodstuff.getName())),
                isCompletelyDisplayed()))
                .check(matches(withText(containsString(toDecimalString(newWeight)))));
        // ещё раз проверить заголовок
        onView(allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getProtein()))));
        onView(allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getFats()))));
        onView(allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));
        onView(allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed()))
                .check(matches(withText(toDecimalString(totalNutrition.getCalories()))));
    }

    @Test
    public void todaysTotalNutritionAndNormsDisplayedInHistory() {
        addFoodstuffsToday();
        mActivityRule.launchActivity(null);

        Nutrition totalNutrition = Nutrition.of(foodstuffs[0].withWeight(100))
                .plus(Nutrition.of(foodstuffs[5].withWeight(100)))
                .plus(Nutrition.of(foodstuffs[6].withWeight(100)));
        Nutrition rates = userParameters.getRates();

        onView(withId(R.id.menu_item_history)).perform(click());

        // проверяем значение съеденного нутриента
        Matcher<View> proteinMatcher = allOf(withParent(withId(R.id.protein_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed());
        onView(proteinMatcher).check(matches(withText(toDecimalString(totalNutrition.getProtein()))));

        Matcher<View> fatsMatcher = allOf(withParent(withId(R.id.fats_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed());
        onView(fatsMatcher).check(matches(withText(toDecimalString(totalNutrition.getFats()))));

        Matcher<View> carbsMatcher = allOf(withParent(withId(R.id.carbs_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed());
        onView(carbsMatcher).check(matches(withText(toDecimalString(totalNutrition.getCarbs()))));

        Matcher<View> caloriesMatcher = allOf(withParent(withId(R.id.calories_layout)), withId(R.id.nutrition_text_view), isCompletelyDisplayed());
        onView(caloriesMatcher).check(matches(withText(toDecimalString(totalNutrition.getCalories()))));

        checkNutritionNorms(totalNutrition, rates);

        // новые userParameters
        userParameters = new UserParameters(
                userParameters.getName() + "new",
                userParameters.getTargetWeight() + 1,
                Gender.MALE,
                userParameters.getDateOfBirth().plusDays(400),
                userParameters.getHeight() - 10,
                userParameters.getWeight() + 10,
                Lifestyle.ACTIVE_LIFESTYLE,
                Formula.MIFFLIN_JEOR,
                userParameters.getRates().plus(Nutrition.withValues(10, 20, 30, 40)),
                timeProvider.nowUtc().getMillis());
        userParametersWorker.saveUserParameters(userParameters);

        checkNutritionNorms(totalNutrition, userParameters.getRates());
    }

    private void checkNutritionNorms(Nutrition totalNutrition, Nutrition rates) {
        // проверяем значения норм БЖУК
        Matcher<View> proteinRateMatcher = allOf(
                withParent(withId(R.id.protein_layout)),
                withId(R.id.of_n_grams),
                isCompletelyDisplayed());
        onView(proteinRateMatcher).check(matches(withText(containsString(String.valueOf(Math.round(rates.getProtein()))))));

        Matcher<View> fatsRateMatcher = allOf(
                withParent(withId(R.id.fats_layout)),
                withId(R.id.of_n_grams),
                isCompletelyDisplayed());
        onView(fatsRateMatcher).check(matches(withText(containsString(String.valueOf(Math.round(rates.getFats()))))));

        Matcher<View> carbsRateMatcher = allOf(
                withParent(withId(R.id.carbs_layout)),
                withId(R.id.of_n_grams),
                isCompletelyDisplayed());
        onView(carbsRateMatcher).check(matches(withText(containsString(String.valueOf(Math.round(rates.getCarbs()))))));

        Matcher<View> caloriesRateMatcher = allOf(
                withParent(withId(R.id.calories_layout)),
                withId(R.id.of_n_grams),
                isCompletelyDisplayed());
        onView(caloriesRateMatcher).check(matches(withText(containsString(String.valueOf(Math.round(rates.getCalories()))))));

        // проверяем прогресс
        Matcher<View> proteinProgress = allOf(
                isDescendantOfA(withId(R.id.protein_layout)),
                isDisplayed(),
                withId(R.id.nutrition_progress));
        onView(proteinProgress).check(hasProgress(Math.round((float)totalNutrition.getProtein())));
        onView(proteinProgress).check(hasMaxProgress(Math.round((float)rates.getProtein())));

        Matcher<View> fatsProgress = allOf(
                isDescendantOfA(withId(R.id.fats_layout)),
                isDisplayed(),
                withId(R.id.nutrition_progress));
        onView(fatsProgress).check(hasProgress(Math.round((float)totalNutrition.getFats())));
        onView(fatsProgress).check(hasMaxProgress(Math.round((float)rates.getFats())));

        Matcher<View> carbsProgress = allOf(
                isDescendantOfA(withId(R.id.carbs_layout)),
                isDisplayed(),
                withId(R.id.nutrition_progress));
        onView(carbsProgress).check(hasProgress(Math.round((float)totalNutrition.getCarbs())));
        onView(carbsProgress).check(hasMaxProgress(Math.round((float)rates.getCarbs())));

        Matcher<View> caloriesProgress = allOf(
                isDescendantOfA(withId(R.id.calories_layout)),
                isDisplayed(),
                withId(R.id.nutrition_progress));
        onView(caloriesProgress).check(hasProgress(Math.round((float)totalNutrition.getCalories())));
        onView(caloriesProgress).check(hasMaxProgress(Math.round((float)rates.getCalories())));
    }

    @Test
    public void addingToHistoryFromBucketListWorks() {
        // сохраняем в БД фудстаффы, которые будем потом добавлять в историю
        Foodstuff f1 = Foodstuff.withName("carrot").withNutrition(1.3, 0.1, 6.9, 32);
        Foodstuff f2 = Foodstuff.withName("oil").withNutrition(0, 99.9, 0, 899);
        List<Long> addingFoodstuffsIds = new ArrayList<>();
        databaseWorker.saveGroupOfFoodstuffs(
                new Foodstuff[]{f1, f2},
                addingFoodstuffsIds::addAll);
        List<WeightedFoodstuff> foodstuffs = new ArrayList<>();
        foodstuffs.add(Foodstuff.withId(addingFoodstuffsIds.get(0))
                .withName(f1.getName())
                .withNutrition(f1.getProtein(), f1.getFats(), f1.getCarbs(), f1.getCalories())
                .withWeight(310));
        foodstuffs.add(Foodstuff.withId(addingFoodstuffsIds.get(1))
                .withName(f2.getName())
                .withNutrition(f2.getProtein(), f2.getFats(), f2.getCarbs(), f2.getCalories())
                .withWeight(13));

        Intent startIntent =
                MainActivityController.createOpenHistoryAndAddFoodstuffsIntent(
                        context, foodstuffs, timeProvider.now().toLocalDate());
        mActivityRule.launchActivity(startIntent);

        onView(withText(containsString(f1.getName()))).check(matches(isDisplayed()));
        onView(withText(containsString(f2.getName()))).check(matches(isDisplayed()));
    }

    public void canSwitchDateInHistoryFragment() {
        // сохранить продукты в историю на другую дату (30 января)
        NewHistoryEntry[] newEntries1 = new NewHistoryEntry[3];
        DateTime jan30 = new DateTime(2019, 1, 30, 0, 0, 0);
        newEntries1[0] = new NewHistoryEntry(foodstuffsIds.get(0), 100, jan30.toDate());
        newEntries1[1] = new NewHistoryEntry(foodstuffsIds.get(5), 100, jan30.toDate());
        newEntries1[2] = new NewHistoryEntry(foodstuffsIds.get(6), 100, jan30.toDate());
        historyWorker.saveGroupOfFoodstuffsToHistory(newEntries1);

        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        onView(withText(containsString(foodstuffs[0].getName()))).check(doesNotExist());
        onView(withText(containsString(foodstuffs[5].getName()))).check(doesNotExist());
        onView(withText(containsString(foodstuffs[6].getName()))).check(doesNotExist());

        onView(withId(R.id.calendar_button)).perform(click());
        // Change the date of the DatePicker.
        // Don't use "withId" as at runtime Android shares the DatePicker id between several sub-elements
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(jan30.getYear(), jan30.getMonthOfYear(), jan30.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());

        onView(withText(containsString(foodstuffs[0].getName()))).check(matches(isDisplayed()));
        onView(withText(containsString(foodstuffs[5].getName()))).check(matches(isDisplayed()));
        onView(withText(containsString(foodstuffs[6].getName()))).check(matches(isDisplayed()));
    }

    @Test
    public void returnForTodayButtonWorksAndDisappearsOnToday() {
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // проверяем, что на сегодняшней дате кнопки "Сегодня" нет
        onView(withId(R.id.return_for_today_button)).check(matches(not(isDisplayed())));

        // открываем другую дату и проверяем, что кнопка появилась
        DateTime anotherDate = timeProvider.now().minusDays(10);
        onView(withId(R.id.calendar_button)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        anotherDate.getYear(), anotherDate.getMonthOfYear(), anotherDate.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.return_for_today_button)).check(matches(isDisplayed()));

        // нажимаем на кнопку "Сегодня" и проверяем, что она пропадает
        onView(withId(R.id.return_for_today_button)).perform(click());
        onView(withId(R.id.return_for_today_button)).check(matches(not(isDisplayed())));

        // нажимаем на календарь и проверяем, что выбрана сегодняшняя дата
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime now = timeProvider.now();
        onView(withClassName(equalTo(DatePicker.class.getName()))).check(matches(matchesDate(
                now.getYear(), now.getMonthOfYear(), now.getDayOfMonth())));
    }

    @Test
    public void showsDatesInHistoryToolbar() {
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // проверяем, что надпись Сегодня
        onView(withId(R.id.title_text)).check(matches(withText(R.string.today)));
        // выбираем вчера, проверяем
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime today = timeProvider.now();
        DateTime yesterday = today.minusDays(1);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        yesterday.getYear(), yesterday.getMonthOfYear(), yesterday.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.yesterday)));
        // выбираем позавчера
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime dayBeforeYesterday = today.minusDays(2);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        dayBeforeYesterday.getYear(), dayBeforeYesterday.getMonthOfYear(), dayBeforeYesterday.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.day_before_yesterday)));
        // выбираем завтра
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime tomorrow = today.plusDays(1);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        tomorrow.getYear(), tomorrow.getMonthOfYear(), tomorrow.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.tomorrow)));
        // выбираем послезавтра
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime dayAfterTomorrow = today.plusDays(2);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        dayAfterTomorrow.getYear(), dayAfterTomorrow.getMonthOfYear(), dayAfterTomorrow.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.day_after_tomorrow)));
        // выбираем случайную дату (-50 дней)
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime anyDay = today.minusDays(50);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        anyDay.getYear(), anyDay.getMonthOfYear(), anyDay.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.title_text)).check(matches(withText(anyDay.toString("dd.MM.yy"))));
        // нажимаем на кнопку Сегодня
        onView(withId(R.id.return_for_today_button)).perform(click());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.today)));
    }

    @Test
    public void toolbarDateIsCorrectOnScreenRotation() throws InterruptedException {
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());
        // выбрать дату
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime anyDay = timeProvider.now().minusDays(50);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        anyDay.getYear(), anyDay.getMonthOfYear(), anyDay.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        // повернуть экран, проверить
        Activity activity = mActivityRule.getActivity();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        Thread.sleep(500); // Попытка исправить флакучесть
        onView(withId(R.id.title_text)).check(matches(withText(anyDay.toString("dd.MM.yy"))));

        // выбрать дату
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime anyDay2 = timeProvider.now().minusDays(30);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        anyDay2.getYear(), anyDay2.getMonthOfYear(), anyDay2.getDayOfMonth()));
        // повернуть экран и нажать в календаре ОК
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        onView(withId(android.R.id.button1)).perform(click());
        // проверить, что дата правильная
        onView(withId(R.id.title_text)).check(matches(withText(anyDay2.toString("dd.MM.yy"))));
    }

    @Test
    public void addingFoodstuffsToCertainDateWorks() {
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // День в прошлом, но не полночь
        DateTime pastDay = timeProvider.now().minusDays(50).plusHours(3);

        // Сохраним продукт на старую дату
        Foodstuff oldFoodstuff = Foodstuff.withName("oldfoodstuff").withNutrition(1, 2, 3, 4);
        oldFoodstuff = foodstuffsList.saveFoodstuff(oldFoodstuff).blockingGet();
        historyWorker.saveFoodstuffToHistory(pastDay.toDate(), oldFoodstuff.getId(), 123);

        // выбрать дату
        onView(withId(R.id.calendar_button)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        pastDay.getYear(), pastDay.getMonthOfYear(), pastDay.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        // проверим, что дата выбрана
        onView(withText(pastDay.toString("dd.MM.yy"))).check(matches(isDisplayed()));

        // добавить продукт
        ArrayList<WeightedFoodstuff> addedFoodstuffs = new ArrayList<>(1);
        addedFoodstuffs.add(
                Foodstuff.withId(foodstuffsIds.get(0))
                        .withName(foodstuffs[0].getName())
                        .withNutrition(Nutrition.of100gramsOf(foodstuffs[0]))
                        .withWeight(123));
        onView(withId(R.id.history_fab)).perform(click());
        onView(allOf(
                withText(addedFoodstuffs.get(0).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))))
                .perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button1)).perform(click());

        // подтверждаем намерение добавить продукт на не текущаю дату
        onView(withId(R.id.positive_button)).perform(click());

        // переключаем обратно на Историю
        onView(withId(R.id.menu_item_history)).perform(click());
        // проверим, что дата всё ещё выбрана
        onView(withText(pastDay.toString("dd.MM.yy"))).check(matches(isDisplayed()));
        // проверим, что продукт есть на экране и что он _над_ старым продуктом
        Matcher<View> oldFoodstuffMatcher = allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(oldFoodstuff.getName())));
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(addedFoodstuffs.get(0).getName())),
                matches(isCompletelyAbove(oldFoodstuffMatcher))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void addingFoodstuffsToCertainDate_canBeSwitchedToAddingToToday() {
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // выбрать дату
        onView(withId(R.id.calendar_button)).perform(click());
        DateTime anyDay = timeProvider.now().minusDays(50);
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        anyDay.getYear(), anyDay.getMonthOfYear(), anyDay.getDayOfMonth()));
        onView(withId(android.R.id.button1)).perform(click());
        // проверим, что дата выбрана
        onView(withText(anyDay.toString("dd.MM.yy"))).check(matches(isDisplayed()));

        // добавить продукт
        ArrayList<WeightedFoodstuff> addedFoodstuffs = new ArrayList<>(1);
        addedFoodstuffs.add(
                Foodstuff.withId(foodstuffsIds.get(0))
                        .withName(foodstuffs[0].getName())
                        .withNutrition(Nutrition.of100gramsOf(foodstuffs[0]))
                        .withWeight(123));
        onView(withId(R.id.history_fab)).perform(click());
        onView(allOf(
                withText(addedFoodstuffs.get(0).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))))
                .perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button1)).perform(click());

        // передумываем - добавляем продукт на сегодня вместо выбранной даты
        onView(withId(R.id.negative_button)).perform(click());

        // переключаем обратно на Историю
        onView(withId(R.id.menu_item_history)).perform(click());
        // проверим, что дата в прошлом уже не выбрана, а выбрано "Сегодня"
        onView(withText(anyDay.toString("dd.MM.yy"))).check(doesNotExist());
        onView(allOf(
                withText(R.string.today),
                isDescendantOfA(withId(R.id.title_layout))))
                .check(matches(isDisplayed()));
        // проверим, что продукт есть на экране
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(addedFoodstuffs.get(0).getName()))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void foodstuffsAddedOnCertainDate_ShownInHistory() {
        ArrayList<WeightedFoodstuff> addedFoodstuffs = new ArrayList<>(1);
        addedFoodstuffs.add(
                Foodstuff.withId(foodstuffsIds.get(0))
                        .withName(foodstuffs[0].getName())
                        .withNutrition(Nutrition.of100gramsOf(foodstuffs[0]))
                        .withWeight(200));
        LocalDate date = timeProvider.now().minusDays(25).toLocalDate();
        Intent intent = MainActivityController.createOpenHistoryAndAddFoodstuffsIntent(context, addedFoodstuffs, date);
        mActivityRule.launchActivity(intent);

        onView(withText(containsString(addedFoodstuffs.get(0).getName()))).check(matches(isDisplayed()));
        onView(withId(R.id.title_text)).check(matches(withText(date.toString("dd.MM.yy"))));
    }

    @Test
    public void mainScreenFoodstuffCard_addsFoodstuffToHistory() {
        mActivityRule.launchActivity(null);

        // Клик на продукт и ввод массы
        onView(allOf(
                withText(foodstuffs[0].getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));

        // Жмём на кнопку добавления в Историю
        onView(withId(R.id.button1)).perform(click());

        // Переходим в Историю и убеждаемся, что продукт там
        onView(withId(R.id.menu_item_history)).perform(click());
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[0].getName()))))
                .check(matches(isDisplayed()));

        // Делаем рестарт и проверяем ещё раз
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[0].getName()))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void canSwitchDates_byViewPager() throws InterruptedException {
        clearAllData();
        Foodstuff[] foodstuffs = new Foodstuff[] {
                Foodstuff.withName("f1").withNutrition(1, 2, 3, 4),
                Foodstuff.withName("f2").withNutrition(1, 2, 3, 4),
                Foodstuff.withName("f3").withNutrition(1, 2, 3, 4)
        };
        foodstuffs[0] = foodstuffsList.saveFoodstuff(foodstuffs[0]).blockingGet();
        foodstuffs[1] = foodstuffsList.saveFoodstuff(foodstuffs[1]).blockingGet();
        foodstuffs[2] = foodstuffsList.saveFoodstuff(foodstuffs[2]).blockingGet();

        Date today = timeProvider.now().toDate();
        Date yesterday = timeProvider.now().minusDays(1).toDate();
        Date tomorrow = timeProvider.now().plusDays(1).toDate();

        historyWorker.saveGroupOfFoodstuffsToHistory(new NewHistoryEntry[]{
                new NewHistoryEntry(foodstuffs[0].getId(), 123, today),
                new NewHistoryEntry(foodstuffs[1].getId(), 123, yesterday),
                new NewHistoryEntry(foodstuffs[2].getId(), 123, tomorrow),
        });

        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        // Today
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[0].getName()))))
                .check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[1].getName()))))
                .check(isNotDisplayed());
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[2].getName()))))
                .check(isNotDisplayed());

        // Yesterday
        onView(withId(R.id.history_view_pager)).perform(swipeRight());
        Thread.sleep(500);
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[0].getName()))))
                .check(isNotDisplayed());
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[1].getName()))))
                .check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[2].getName()))))
                .check(isNotDisplayed());

        // Tomorrow
        onView(withId(R.id.history_view_pager)).perform(swipeLeft());
        Thread.sleep(500);
        onView(withId(R.id.history_view_pager)).perform(swipeLeft());
        Thread.sleep(500);
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[0].getName()))))
                .check(isNotDisplayed());
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[1].getName()))))
                .check(isNotDisplayed());
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_history)),
                withText(containsString(foodstuffs[2].getName()))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void dateTitleChanges_whenResumedOnNewDate() {
        mActivityRule.launchActivity(null);
        onView(withId(R.id.menu_item_history)).perform(click());

        onView(withId(R.id.title_text)).check(matches(withText(R.string.today)));
        timeProvider.setTime(timeProvider.now().plusDays(1));
        onView(withId(R.id.title_text)).check(matches(withText(R.string.today)));

        callActivityAndFragmentsOnPause(mainThreadExecutor, mActivityRule.getActivity());
        callActivityAndFragmentsOnResume(mainThreadExecutor, mActivityRule.getActivity());
        onView(withId(R.id.title_text)).check(matches(withText(R.string.yesterday)));
    }

    @Test
    public void weightedFoodstuffReturnedByBucketListActivity_addedToHistory() {
        mActivityRule.launchActivity(null);

        Foodstuff notSavedFoodstuff = Foodstuff
                .withName("new_foodstuff_with_new_name")
                .withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff = databaseWorker.saveFoodstuff(notSavedFoodstuff).blockingGet();

        // Check history is not displayed yet
        onView(withId(R.id.fragment_history)).check(isNotDisplayed());

        // Tell everyone about the foodstuff intended for history
        List<Fragment> fragments = mActivityRule.getActivity().getSupportFragmentManager().getFragments();
        mainThreadExecutor.execute(() -> {
            for (Fragment f : fragments) {
                f.onActivityResult(RequestCodes.MAIN_SCREEN_BUCKET_LIST_OPEN_RECIPE,
                        Activity.RESULT_OK,
                        BucketListActivity.createAddToHistoryResultIntent(foodstuff.withWeight(100)));
            }
        });

        // Check history is now displayed
        onView(withId(R.id.fragment_history)).check(matches(isDisplayed()));

        // Check the foodstuff is added into history UI
        onView(allOf(
                withText(containsString(foodstuff.getName())),
                isDescendantOfA(withId(R.id.fragment_history))))
                .check(matches(isDisplayed()));

        // Check the foodstuff is added into history DB
        List<HistoryEntry> history =
                historyWorker.requestHistoryForPeriod(
                        timeProvider.now().withTimeAtStartOfDay().getMillis(),
                        timeProvider.now().plusDays(1).withTimeAtStartOfDay().getMillis())
                        .toList().blockingGet();
        boolean found = false;
        for (HistoryEntry entry : history) {
            if (entry.getFoodstuff().equals(foodstuff.withWeight(100))) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void weightedFoodstuffReturnedByBucketListActivity_addedToHistory_intoSelectedDay() throws InterruptedException {
        mActivityRule.launchActivity(null);

        // Change date to yesterday and get back to main screen
        onView(withId(R.id.menu_item_history)).perform(click());
        onView(withId(R.id.history_view_pager)).perform(swipeRight());
        Thread.sleep(500);
        onView(withText(R.string.yesterday)).check(matches(isDisplayed()));
        onView(withId(R.id.menu_item_foodstuffs)).perform(click());

        Foodstuff notSavedFoodstuff = Foodstuff
                .withName("new_foodstuff_with_new_name")
                .withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff = databaseWorker.saveFoodstuff(notSavedFoodstuff).blockingGet();

        // Check history is not displayed yet
        onView(withId(R.id.fragment_history)).check(isNotDisplayed());

        // Tell everyone about the foodstuff intended for history
        List<Fragment> fragments = mActivityRule.getActivity().getSupportFragmentManager().getFragments();
        mainThreadExecutor.execute(() -> {
            for (Fragment f : fragments) {
                f.onActivityResult(RequestCodes.MAIN_SCREEN_BUCKET_LIST_OPEN_RECIPE,
                        Activity.RESULT_OK,
                        BucketListActivity.createAddToHistoryResultIntent(foodstuff.withWeight(100)));
            }
        });

        // Check we're asked if we want to add the foodstuff into another day
        // And agree
        onView(withId(R.id.two_options_dialog_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.positive_button)).perform(click());

        // Check history is displayed
        onView(withId(R.id.fragment_history)).check(matches(isDisplayed()));

        // Check the foodstuff is added into history UI
        onView(allOf(
                withText(containsString(foodstuff.getName())),
                isDescendantOfA(withId(R.id.fragment_history))))
                .check(matches(isDisplayed()));

        // Check the foodstuff is added into history DB into yesterday
        List<HistoryEntry> history =
                historyWorker.requestHistoryForPeriod(
                        timeProvider.now().minusDays(1).withTimeAtStartOfDay().getMillis(),
                        timeProvider.now().withTimeAtStartOfDay().getMillis())
                        .toList().blockingGet();
        boolean found = false;
        for (HistoryEntry entry : history) {
            if (entry.getFoodstuff().equals(foodstuff.withWeight(100))) {
                found = true;
            }
        }
        assertTrue(found);
    }


    // https://stackoverflow.com/a/44840330
    public static Matcher<View> matchesDate(final int year, final int month, final int day) {
        return new BoundedMatcher<View, DatePicker>(DatePicker.class) {

            @Override
            public void describeTo(Description description) {
                description.appendText("matches date:");
            }

            @Override
            protected boolean matchesSafely(DatePicker item) {
                return (year == item.getYear() && month == item.getMonth() + 1 && day == item.getDayOfMonth());
            }
        };
    }

    private void addFoodstuffsToday() {
        NewHistoryEntry[] newEntries = new NewHistoryEntry[3];
        DateTime today = timeProvider.now();
        newEntries[0] = new NewHistoryEntry(foodstuffsIds.get(0), 100,
                new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 8, 0).toDate());
        newEntries[1] = new NewHistoryEntry(foodstuffsIds.get(5), 100,
                new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 9, 0).toDate());
        newEntries[2] = new NewHistoryEntry(foodstuffsIds.get(6), 100,
                new DateTime(today.year().get(), today.monthOfYear().get(), today.getDayOfMonth(), 10, 0).toDate());
        historyWorker.saveGroupOfFoodstuffsToHistory(newEntries);
    }
}
