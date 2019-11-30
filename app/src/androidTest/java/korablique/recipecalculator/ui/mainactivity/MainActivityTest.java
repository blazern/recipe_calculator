package korablique.recipecalculator.ui.mainactivity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.github.mikephil.charting.charts.LineChart;

import junit.framework.Assert;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import korablique.recipecalculator.R;
import korablique.recipecalculator.RequestCodes;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.GoalCalculator;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.PopularProductsUtils;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.BundleMatchers.hasValue;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtras;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotContains;
import static korablique.recipecalculator.ui.DecimalUtils.toDecimalString;
import static korablique.recipecalculator.util.EspressoUtils.matches;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest extends MainActivityTestsBase {
    @Test
    public void topHeaderDoNotDisplayedIfHistoryIsEmpty() {
        databaseHolder.getDatabase().clearAllTables();
        mActivityRule.launchActivity(null);
        assertNotContains(mActivityRule.getActivity().getString(R.string.top_header));
    }

    @Test
    public void bothHeadersDisplayedIfHistoryIsNotEmpty() {
        mActivityRule.launchActivity(null);
        assertContains(mActivityRule.getActivity().getString(R.string.top_header));
        assertContains(mActivityRule.getActivity().getString(R.string.all_foodstuffs_header));
    }

    @Test
    public void topIsCorrect() {
        mActivityRule.launchActivity(null);

        List<Foodstuff> topFoodstuffs = extractFoodstuffsTopFromDB();

        // Рассчитываем, что в топе будет как минимум 3 фудстафа - как бы константа количества
        // фудстафов в топе не менялась, менее 3 её делать не стоит.
        for (int index = 0; index < 2; ++index) {
            Foodstuff foodstuff = topFoodstuffs.get(index);
            Foodstuff foodstuffBelow = topFoodstuffs.get(index + 1);

            // NOTE: оба Фудстафа мы фильтруем проверкой "completely above all_foodstuffs_header"
            // Это нужно из-за того, что одни и те же Фудстафы могут присутствовать в двух списках -
            // в топе Фудстафов и в списке всех Фудстафов. Когда Эспрессо просят найти вьюшку,
            // и под параметры поиска подпадают сразу несколько вьюшек, Эспрессо моментально паникует
            // и роняет тест.
            // В данном тесте мы проверяем только топ, весь список нам не нужен, поэтому явно говорим
            // Эспрессо, что нас интересуют только вьюшки выше заголовка all_foodstuffs_header.

            Matcher<View> foodstuffMatcher = allOf(
                    withText(foodstuff.getName()),
                    matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))));

            Matcher<View> foodstuffBelowMatcher = allOf(
                    withText(foodstuffBelow.getName()),
                    matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                    matches(isCompletelyBelow(foodstuffMatcher)));

            onView(foodstuffMatcher).check(matches(isDisplayed()));
            onView(foodstuffBelowMatcher).check(matches(isDisplayed()));
        }
    }

    @Test
    public void moreThanMonthOldFoodstuffs_dontGoToTop() {
        mActivityRule.launchActivity(null);

        Foodstuff newFoodstuff1 = Foodstuff.withName("newfoodstuff1").withNutrition(1, 2, 3, 4);
        Foodstuff newFoodstuff2 = Foodstuff.withName("newfoodstuff2").withNutrition(1, 2, 3, 4);
        long[] ids = new long[2];
        foodstuffsList.saveFoodstuff(newFoodstuff1, new FoodstuffsList.SaveFoodstuffCallback() {
            @Override
            public void onResult(Foodstuff addedFoodstuff) {
                ids[0] = addedFoodstuff.getId();
            }
            @Override public void onDuplication() {}
        });
        foodstuffsList.saveFoodstuff(newFoodstuff2, new FoodstuffsList.SaveFoodstuffCallback() {
            @Override
            public void onResult(Foodstuff addedFoodstuff) {
                ids[1] = addedFoodstuff.getId();
            }
            @Override public void onDuplication() {}
        });

        // newFoodstuff1 на сегодня
        for (int index = 0; index < 5; ++index) {
            addFoodstuffToDate(timeProvider.now(), ids[0]);
        }
        // newFoodstuff2 на 2 месяца в прошлом
        for (int index = 0; index < 5; ++index) {
            addFoodstuffToDate(timeProvider.now().minusMonths(2), ids[1]);
        }

        // newFoodstuff1 должен быть в топе
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_main_screen)),
                withText(newFoodstuff1.getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))))
                .check(matches(isDisplayed()));
        // newFoodstuff2 НЕ должен быть в топе
        onView(allOf(
                isDescendantOfA(withId(R.id.fragment_main_screen)),
                withText(newFoodstuff2.getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))))
                .check(doesNotExist());
    }

    @Test
    public void startsBucketListActivityWithSelectedFoodstuffs() {
        mActivityRule.launchActivity(null);

        List<Foodstuff> topFoodstuffs = extractFoodstuffsTopFromDB();

        ArrayList<Foodstuff> clickedFoodstuffs = new ArrayList<>();
        clickedFoodstuffs.add(topFoodstuffs.get(0));
        clickedFoodstuffs.add(topFoodstuffs.get(1));
        clickedFoodstuffs.add(topFoodstuffs.get(2));

        // Кликаем на первый, второй и третий продукт в топе.
        onView(allOf(
                withText(clickedFoodstuffs.get(0).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button2)).perform(click());

        onView(allOf(
                withText(clickedFoodstuffs.get(1).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button2)).perform(click());

        onView(allOf(
                withText(clickedFoodstuffs.get(2).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))))).perform(click());
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button2)).perform(click());

        // Кликаем на корзинку в снэкбаре
        onView(withId(R.id.basket)).perform(click());

        List<WeightedFoodstuff> clickedWeightedFoodstuffs = new ArrayList<>();
        for (Foodstuff foodstuff : clickedFoodstuffs) {
            clickedWeightedFoodstuffs.add(foodstuff.withWeight(123));
        }

        // Проверяем, что была попытка стартовать активити по интенту от BucketListActivity,
        // также что этот интент содержит информацию о кликнутых продуктах.
        Intent expectedIntent =
                BucketListActivity.createStartIntentFor(
                        mActivityRule.getActivity(),
                        clickedWeightedFoodstuffs,
                        timeProvider.now().toLocalDate());
        intended(allOf(
                hasAction(expectedIntent.getAction()),
                hasComponent(expectedIntent.getComponent()),
                hasExtras(hasValue(clickedWeightedFoodstuffs))));
    }

    @Test
    public void showsCardWhenBucketListActivityCreatesFoodstuff() {
        mActivityRule.launchActivity(null);

        // Проверяем, что сперва карточка не показана
        onView(withId(R.id.foodstuff_card_layout)).check(doesNotExist());

        // Создаём продукт и сообщаем фрагментам, что его создал бакетлист
        Foodstuff foodstuff = Foodstuff
                .withName("new_foodstuff_with_new_name")
                .withNutrition(1, 2, 3, 4);
        List<Fragment> fragments = mActivityRule.getActivity().getSupportFragmentManager().getFragments();
        for (Fragment f : fragments) {
            f.onActivityResult(RequestCodes.MAIN_SCREEN_BUCKET_LIST_CREATE_FOODSTUFF,
                    Activity.RESULT_OK,
                    BucketListActivity.createFoodstuffResultIntent(foodstuff));
        }
        // Убеждаемся, что показана карточка с новым продуктом
        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withText(containsString("new_foodstuff_with_new_name"))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void editedFoodstuffReplacesInBothTopAndAllFoodstuffs() {
        mActivityRule.launchActivity(null);
        List<Foodstuff> topFoodstuffs = extractFoodstuffsTopFromDB();

        Matcher<View> topMatcher = allOf(
                withText(topFoodstuffs.get(2).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header))));
        onView(topMatcher).perform(click());
        onView(withId(R.id.button_edit)).perform(click());

        // Редактируем
        String newName = topFoodstuffs.get(2).getName() + "1";
        onView(withId(R.id.foodstuff_name)).perform(replaceText(newName));
        onView(withId(R.id.save_button)).perform(click());

        // Закрываем карточку
        onView(withId(R.id.button_close)).perform(click());

        // Проверяем отредактированное
        topMatcher = allOf(
                withText(newName),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header))));
        onView(topMatcher).check(matches(isDisplayed()));

        Matcher<View> allFoodstuffsMatcher = allOf(
                withText(newName),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))));
        onView(allFoodstuffsMatcher).check(matches(isDisplayed()));
    }

    @Test
    public void deletingFoodstuffsWorks() {
        mActivityRule.launchActivity(null);

        Foodstuff deletingFoodstuff = Foodstuff
                .withId(foodstuffsIds.get(0))
                .withName(foodstuffs[0].getName())
                .withNutrition(Nutrition.of100gramsOf(foodstuffs[0]));
        onView(allOf(
                withText(deletingFoodstuff.getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header)))))
                .perform(click());
        onView(withId(R.id.button_edit)).perform(click());
        onView(withId(R.id.button_delete)).perform(click());
        onView(withId(R.id.positive_button)).perform(click());
        onView(withText(deletingFoodstuff.getName())).check(doesNotExist());

        List<Foodstuff> foodstuffsListAfterDeleting = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(100, foodstuffs -> {
            foodstuffsListAfterDeleting.addAll(foodstuffs);
        });
        Assert.assertFalse(foodstuffsListAfterDeleting.contains(deletingFoodstuff));

        // Recreate activity and check again
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(withText(deletingFoodstuff.getName())).check(doesNotExist());
    }


    @Test
    public void foodstuffDeletionCancellationWorks() {
        mActivityRule.launchActivity(null);

        Foodstuff deletingFoodstuff = Foodstuff
                .withId(foodstuffsIds.get(0))
                .withName(foodstuffs[0].getName())
                .withNutrition(Nutrition.of100gramsOf(foodstuffs[0]));
        onView(allOf(
                withText(deletingFoodstuff.getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header)))))
                .perform(click());
        onView(withId(R.id.button_edit)).perform(click());
        onView(withId(R.id.button_delete)).perform(click());

        // Cancel deletion!
        onView(withId(R.id.negative_button)).perform(click());
        // Return to main screen
        onView(isRoot()).perform(ViewActions.pressBack());

        onView(withText(deletingFoodstuff.getName())).check(matches(isDisplayed()));

        List<Foodstuff> foodstuffsListAfterDeleting = new ArrayList<>();
        databaseWorker.requestListedFoodstuffsFromDb(100, foodstuffs -> {
            foodstuffsListAfterDeleting.addAll(foodstuffs);
        });
        Assert.assertTrue(foodstuffsListAfterDeleting.contains(deletingFoodstuff));

        // Recreate activity and check again
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        onView(withText(deletingFoodstuff.getName())).check(matches(isDisplayed()));
    }

    @Test
    public void snackbarUpdatesAfterChangingBucketList() {
        // добавляем в bucket list продукты, запускаем активити, в снекбаре должно быть 3 фудстаффа
        WeightedFoodstuff wf0 = foodstuffs[0].withWeight(100);
        WeightedFoodstuff wf1 = foodstuffs[1].withWeight(100);
        WeightedFoodstuff wf2 = foodstuffs[2].withWeight(100);

        mainThreadExecutor.execute(() -> {
            bucketList.add(wf0);
            bucketList.add(wf1);
            bucketList.add(wf2);
        });

        mActivityRule.launchActivity(null);
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(withText("3")));

        // убираем один продукт, перезапускаем активити, в снекбаре должно быть 2 фудстаффа
        mainThreadExecutor.execute(() -> {
            bucketList.remove(foodstuffs[0].withWeight(100));
        });

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        instrumentation.waitForIdleSync();
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(isDisplayed()));
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(withText("2")));
        Assert.assertTrue(bucketList.getList().contains(wf1));
        Assert.assertTrue(bucketList.getList().contains(wf2));

        // убираем все продукты, перезапускаем активити, снекбара быть не должно
        mainThreadExecutor.execute(() -> {
            bucketList.clear();
        });
        instrumentation.runOnMainSync(() -> mActivityRule.getActivity().recreate());
        instrumentation.waitForIdleSync();
        onView(withId(R.id.selected_foodstuffs_counter)).check(matches(not(isDisplayed())));
    }

    @Test
    public void swipedOutSnackbar_cleansBuckerList() throws InterruptedException {
        // добавляем в bucket list продукты, запускаем активити
        WeightedFoodstuff wf0 = foodstuffs[0].withWeight(100);
        WeightedFoodstuff wf1 = foodstuffs[1].withWeight(100);

        mainThreadExecutor.execute(() -> {
            bucketList.add(wf0);
            bucketList.add(wf1);
            assertEquals(2, bucketList.getList().size());
        });

        mActivityRule.launchActivity(null);

        // "Высвайпываем" снекбар
        onView(withId(R.id.snackbar)).perform(swipeRight());
        Thread.sleep(500); // Ждем 0.5с, потому что SwipeDismissBehaviour криво уведомляет о высвайпывании

        // Убеждаемся, что в бакетлисте пусто и снекбар не показан
        onView(withId(R.id.snackbar)).check(matches(not(isDisplayed())));
        mainThreadExecutor.execute(() -> {
            assertTrue(bucketList.getList().isEmpty());
        });
    }

    @Test
    public void swipedOutSnackbar_canBeCanceled() throws InterruptedException {
        // добавляем в bucket list продукты, запускаем активити
        WeightedFoodstuff wf0 = foodstuffs[0].withWeight(100);
        WeightedFoodstuff wf1 = foodstuffs[1].withWeight(100);

        mainThreadExecutor.execute(() -> {
            bucketList.add(wf0);
            bucketList.add(wf1);
            assertEquals(2, bucketList.getList().size());
        });

        mActivityRule.launchActivity(null);

        // "Высвайпываем" снекбар
        onView(withId(R.id.snackbar)).perform(swipeRight());
        Thread.sleep(500); // Ждем 0.5с, потому что SwipeDismissBehaviour криво уведомляет о высвайпывании

        // Тут же отменяем "высвайпывание"
        onView(withText(R.string.undo)).perform(click());

        // Убеждаемся, что в бакетлисте по-прежнему есть продукты и снекбар показан
        onView(withId(R.id.snackbar)).check(matches(isDisplayed()));
        mainThreadExecutor.execute(() -> {
            assertEquals(2, bucketList.getList().size());
        });
    }

    @Test
    public void profileDisplaysCorrectUserParameters() {
        mActivityRule.launchActivity(null);
        onView(allOf(withText(R.string.profile), withEffectiveVisibility(VISIBLE)))
                .perform(click());
        // сейчас в UserParametersWorker'е только одни параметры.
        // проверяем, что они отображаются в профиле
        String ageString = String.valueOf(userParameters.getAge());
        onView(withId(R.id.age)).check(matches((withText(containsString(ageString)))));
        onView(withId(R.id.height)).check(matches(withText(String.valueOf(userParameters.getHeight()))));

        String targetWeightString = toDecimalString(userParameters.getTargetWeight());
        onView(withId(R.id.target_weight)).check(matches(withText(targetWeightString)));

        String currentWeightString = toDecimalString(userParameters.getWeight());
        onView(withId(R.id.current_weight_measurement_value)).check(matches(withText(currentWeightString)));

        onView(withId(R.id.user_name)).check(matches(withText(userNameProvider.getUserName().toString())));
        DateTime measurementsDate = new DateTime(userParameters.getMeasurementsTimestamp());
        String measurementsDateString = measurementsDate.toString(mActivityRule.getActivity().getString(R.string.date_format));
        onView(withId(R.id.last_measurement_date_measurement_value)).check(matches(withText(measurementsDateString)));

        // проверяем, что отображаются правильные нормы
        Rates rates = RateCalculator.calculate(userParameters);
        onView(withId(R.id.calorie_intake)).check(matches(withText(toDecimalString(rates.getCalories()))));
        onView(allOf(
                withEffectiveVisibility(VISIBLE),
                withParent(withId(R.id.protein_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(rates.getProtein()))));
        onView(allOf(
                withEffectiveVisibility(VISIBLE),
                withParent(withId(R.id.fats_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(rates.getFats()))));
        onView(allOf(
                withEffectiveVisibility(VISIBLE),
                withParent(withId(R.id.carbs_layout)),
                withId(R.id.nutrition_text_view)))
                .check(matches(withText(toDecimalString(rates.getCarbs()))));

        // проверяем процент достижения цели
        int percent = GoalCalculator.calculateProgressPercentage(
                userParameters.getWeight(), userParameters.getWeight(), userParameters.getTargetWeight());
        onView(withId(R.id.done_percent)).check(matches(withText(String.valueOf(percent))));
    }

    @Test
    public void measurementsCardDisplaysPreviousDate() {
        databaseHolder.getDatabase().clearAllTables();
        // сохраняем параметры на дату в прошлом 12.8+1.2019 12:00
        DateTime lastDate = new DateTime(2019, 8, 12, 12, 12, 0, DateTimeZone.UTC);
        UserParameters lastParams = new UserParameters(45, Gender.FEMALE, new LocalDate(1993, 9, 27),
                158, 49.6f, Lifestyle.ACTIVE_LIFESTYLE, Formula.HARRIS_BENEDICT, lastDate.getMillis());
        userParametersWorker.saveUserParameters(lastParams);

        mActivityRule.launchActivity(null);
        // переходим в профиль
        onView(allOf(withText(R.string.profile), withEffectiveVisibility(VISIBLE)))
                .perform(click());
        // открываем карточку
        onView(withId(R.id.set_current_weight)).perform(click());
        // сверяем, что дата прошлых измерений правильная
        String dateMustBe = lastDate.toString(mActivityRule.getActivity().getString(R.string.date_format));
        onView(withId(R.id.last_measurement_header)).check(matches(withText(containsString(dateMustBe))));
    }

    @Test
    public void measurementsCardDisplaysTodaysDate() {
        mActivityRule.launchActivity(null);
        // переходим в профиль
        onView(allOf(withText(R.string.profile), withEffectiveVisibility(VISIBLE)))
                .perform(click());
        // открываем карточку
        onView(withId(R.id.set_current_weight)).perform(click());
        // проверяем сегодняшнюю дату
        DateTime todaysDate = timeProvider.nowUtc();
        String dateMustBe = todaysDate.toString(mActivityRule.getActivity().getString(R.string.date_format));
        onView(withId(R.id.new_measurement_header)).check(matches(withText(containsString(dateMustBe))));
    }

    @Test
    public void canChangeMeasurementsPeriodsInProfileChart() {
        // Clear DB again (remove existing user parameters added in setUp).
        databaseHolder.getDatabase().clearAllTables();
        mainThreadExecutor.execute(() -> {
            bucketList.clear();
        });
        
        // Add 5 measurements to each month for last 2 years
        // Note that measurement time is NOW minus 1 minute - this is to avoid clashes
        // with time periods starts/ends. 
        DateTime measurementTime = timeProvider.now().minusMinutes(1);
        for (int monthIndex = 24; monthIndex >= 1; --monthIndex) {
            for (int measurementIndex = 0; measurementIndex < 5; ++measurementIndex) {
                UserParameters userParameters = new UserParameters(
                        65, Gender.MALE, new LocalDate(1993, 7, 20), 165, 65+monthIndex+measurementIndex,
                        Lifestyle.PROFESSIONAL_SPORTS, Formula.MIFFLIN_JEOR, measurementTime.getMillis());
                userParametersWorker.saveUserParameters(userParameters);
            }
            measurementTime = measurementTime.minusMonths(1);
        }
        mActivityRule.launchActivity(null);
        onView(allOf(withText(R.string.profile), withEffectiveVisibility(VISIBLE)))
                .perform(click());

        // Check that there're 120 dots when all the time is open
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            LineChart chart = mActivityRule.getActivity().findViewById(R.id.chart);
            Assert.assertEquals(120, chart.getLineData().getDataSetByIndex(0).getEntryCount());
        });

        // Check that there're 60 dots when year view is open
        onView(withId(R.id.measurements_period_spinner)).perform(click());
        onView(withText(R.string.user_measurements_period_array_year)).perform(click());
        instrumentation.runOnMainSync(() -> {
            LineChart chart = mActivityRule.getActivity().findViewById(R.id.chart);
            Assert.assertEquals(60, chart.getLineData().getDataSetByIndex(0).getEntryCount());
        });

        // Check that there're 30 dots when 6 months view is open
        onView(withId(R.id.measurements_period_spinner)).perform(click());
        onView(withText(R.string.user_measurements_period_array_6_months)).perform(click());
        instrumentation.runOnMainSync(() -> {
            LineChart chart = mActivityRule.getActivity().findViewById(R.id.chart);
            Assert.assertEquals(30, chart.getLineData().getDataSetByIndex(0).getEntryCount());
        });

        // Check that there're 5 dots when 1 month view is open
        onView(withId(R.id.measurements_period_spinner)).perform(click());
        onView(withText(R.string.user_measurements_period_array_month)).perform(click());
        instrumentation.runOnMainSync(() -> {
            LineChart chart = mActivityRule.getActivity().findViewById(R.id.chart);
            Assert.assertEquals(5, chart.getLineData().getDataSetByIndex(0).getEntryCount());
        });
    }

    // поиск

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
                matches(isCompletelyBelow(withId(R.id.add_new_foodstuff_button))))).perform(click());
        // удаляем его
        onView(withId(R.id.button_edit)).perform(click());
        onView(withId(R.id.button_delete)).perform(click());
        onView(withId(R.id.positive_button)).perform(click());
        // нужно проверять не только текст, но и родителя,
        // т к иначе в проверку попадут вьюшки из MainScreen
        onView(allOf(
                withText(searchingFoodstuff.getName()),
                isDescendantOfA(withId(R.id.search_results_recycler_view)),
                matches(isCompletelyBelow(withId(R.id.add_new_foodstuff_button)))))
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
                matches(isCompletelyBelow(withId(R.id.add_new_foodstuff_button))),
                isDescendantOfA(withId(R.id.search_results_recycler_view))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void onBackPressedLastSearchQueryReturns() {
        mActivityRule.launchActivity(null);

        // ввести название одного продукта
        onView(withId(R.id.search_bar_text)).perform(click());
        onView(withId(R.id.search_bar_text)).perform(replaceText(foodstuffs[0].getName()));
        onView(withId(R.id.search_bar_text)).perform(pressImeActionButton()); // enter

        // другого
        onView(withId(R.id.search_bar_text)).perform(click());
        onView(withId(R.id.search_bar_text)).perform(replaceText(foodstuffs[1].getName()));
        onView(withId(R.id.search_bar_text)).perform(pressImeActionButton());

        // нажать Назад
        onView(isRoot()).perform(ViewActions.pressBack()); // первое нажатие закрывает клавиатуру
        onView(isRoot()).perform(ViewActions.pressBack());

        // убедиться, что в searchView находится название первого продукта
        onView(withId(R.id.search_bar_text)).check(matches(withText(foodstuffs[0].getName())));
        onView(withId(R.id.search_results_layout)).check(matches(isDisplayed()));

        // нажать ещё раз назад и убедиться, что SearchResultsFragment закрылся
        onView(isRoot()).perform(ViewActions.pressBack());
        onView(withId(R.id.search_results_layout)).check(doesNotExist());
    }

    @Test
    public void onBackPressedInMainScreen() {
        mActivityRule.launchActivity(null);

        // главная разметка есть на экране
        onView(withId(R.id.fragment_main_screen)).check(matches(isDisplayed()));
        // бэк
        Espresso.pressBackUnconditionally();
        // мы мертвы
        assertTrue(mActivityRule.getActivity().isDestroyed());
    }

    @Test
    public void onBackPressedInHistory() {
        mActivityRule.launchActivity(null);
        // история
        onView(withId(R.id.menu_item_history)).perform(click());
        onView(withId(R.id.fragment_history)).check(matches(isDisplayed()));

        // бэк должен вернуть мейн-скрин
        Espresso.pressBackUnconditionally();
        onView(withId(R.id.fragment_history)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fragment_main_screen)).check(matches(isDisplayed()));

        // бэк и мы мертвы
        Espresso.pressBackUnconditionally();
        assertTrue(mActivityRule.getActivity().isDestroyed());
    }

    @Test
    public void onBackPressedInProfile() {
        mActivityRule.launchActivity(null);
        // профиль
        onView(withId(R.id.menu_item_profile)).perform(click());
        onView(withId(R.id.fragment_profile)).check(matches(isDisplayed()));

        // бэк должен вернуть мейн-скрин
        Espresso.pressBackUnconditionally();
        onView(withId(R.id.fragment_profile)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fragment_main_screen)).check(matches(isDisplayed()));

        // бэк и мы мертвы
        Espresso.pressBackUnconditionally();
        assertTrue(mActivityRule.getActivity().isDestroyed());
    }

    @Test
    public void onBackPressedIn_afterMultipleFragmentSwitches() {
        mActivityRule.launchActivity(null);
        // меняем несколько раз фрагменты туда-сюда
        for (int i = 0; i < 2; ++i) {
            onView(withId(R.id.menu_item_profile)).perform(click());
            onView(withId(R.id.menu_item_foodstuffs)).perform(click());
            onView(withId(R.id.menu_item_history)).perform(click());
        }
        // последней открывали историю
        onView(withId(R.id.menu_item_history)).check(matches(isDisplayed()));

        // бэк должен вернуть мейн-скрин
        Espresso.pressBackUnconditionally();
        onView(withId(R.id.fragment_history)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fragment_main_screen)).check(matches(isDisplayed()));

        // бэк и мы мертвы
        Espresso.pressBackUnconditionally();
        assertTrue(mActivityRule.getActivity().isDestroyed());
    }

    @Test
    public void mainScreenSeesTopFoodstuffsUpdate() {
        mActivityRule.launchActivity(null);

        // Добавим новый продукт в БД
        Foodstuff newFoodstuff = Foodstuff.withName("newfoodstuff").withNutrition(1, 2, 3, 4);
        AtomicLong newFoodstuffId = new AtomicLong();
        databaseWorker.saveFoodstuff(newFoodstuff, id -> newFoodstuffId.set(id));

        // Убедимся, что в начале теста продукта на экране нет
        onView(withText(newFoodstuff.getName())).check(doesNotExist());

        // "Съедим" его 100 раз сегодня
        NewHistoryEntry[] newEntries = new NewHistoryEntry[100];
        for (int index = 0; index < newEntries.length; ++index) {
            newEntries[index] = new NewHistoryEntry(newFoodstuffId.get(), 100, timeProvider.now().toDate());
        }
        historyWorker.saveGroupOfFoodstuffsToHistory(newEntries);

        // Убедимся, что продукт действительно попал в топ
        boolean found = false;
        List<Foodstuff> top = extractFoodstuffsTopFromDB();
        for (Foodstuff foodstuff : top) {
            if (foodstuff.getId() == newFoodstuffId.get()) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);

        // Продукт должен появиться на экране в топе
        onView(withText(newFoodstuff.getName())).check(matches(isDisplayed()));
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
    public void mainScreenFoodstuffCard_hidesDirectAdditionToHistory_whenBucketListNotEmpty() {
        mActivityRule.launchActivity(null);

        // Клик на продукт
        onView(allOf(
                withText(foodstuffs[0].getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))))).perform(click());

        // Обе кнопки в карточке должны быть видны
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1))).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2))).check(matches(isDisplayed()));

        // Жмём на кнопку создания блюда
        onView(withId(R.id.weight_edit_text)).perform(replaceText("123"));
        onView(withId(R.id.button2)).perform(click());

        // Снова клик на продукт
        onView(allOf(
                withText(foodstuffs[0].getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))))).perform(click());

        // Только кнопка добавления блюда должна быть видна
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2))).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1))).check(matches(not(isDisplayed())));

        // Закрываем карточку
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button_close))).perform(click());

        // Очищаем бакетлист
        mainThreadExecutor.execute(() -> {
            bucketList.clear();
        });

        // Снова клик на продукт
        onView(allOf(
                withText(foodstuffs[0].getName()),
                matches(isCompletelyBelow(withText(R.string.all_foodstuffs_header))))).perform(click());

        // Обе кнопки в карточке снова должны быть видны
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button1))).check(matches(isDisplayed()));
        onView(allOf(
                isDescendantOfA(withId(R.id.foodstuff_card_layout)),
                withId(R.id.button2))).check(matches(isDisplayed()));
    }

    @Test
    public void mainScreenDisplaysCard_afterFoodstuffEditing() {
        mActivityRule.launchActivity(null);
        List<Foodstuff> topFoodstuffs = extractFoodstuffsTopFromDB();

        Matcher<View> topMatcher = allOf(
                withText(topFoodstuffs.get(0).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header))));
        onView(topMatcher).perform(click());
        onView(withId(R.id.button_edit)).perform(click());

        String newName = topFoodstuffs.get(0).getName() + "1";
        onView(withId(R.id.foodstuff_name)).perform(replaceText(newName));
        onView(withId(R.id.save_button)).perform(click());

        onView(withId(R.id.foodstuff_card_layout)).check(matches(isDisplayed()));
    }

    @Test
    public void mainScreenDisplaysCard_afterFoodstuffCreation() {
        mActivityRule.launchActivity(null);

        String name = "111first_foodstuff";
        onView(withText(name)).check(doesNotExist());

        onView(withId(R.id.add_new_foodstuff)).perform(click());

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

    private void addFoodstuffsToday() {
        addFoodstuffToDate(timeProvider.now(),
                foodstuffsIds.get(0),
                foodstuffsIds.get(5),
                foodstuffsIds.get(6));
    }

    private void addFoodstuffToDate(DateTime date, long... foodstuffsIds) {
        NewHistoryEntry[] newEntries = new NewHistoryEntry[foodstuffsIds.length];
        for (int index = 0; index < foodstuffsIds.length; ++index) {
            int weight = 100;
            newEntries[index] =
                    new NewHistoryEntry(foodstuffsIds[index], weight, date.toDate());
            date = date.plusMinutes(1);
        }
        historyWorker.saveGroupOfFoodstuffsToHistory(newEntries);
    }

    private List<Foodstuff> extractFoodstuffsTopFromDB() {
        List<Foodstuff> top = new ArrayList<>();
        topList.getTopList(top::addAll);
        return top;
    }
}
