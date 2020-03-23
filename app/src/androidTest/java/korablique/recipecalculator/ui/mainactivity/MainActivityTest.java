package korablique.recipecalculator.ui.mainactivity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.github.mikephil.charting.charts.LineChart;

import junit.framework.Assert;

import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import korablique.recipecalculator.R;
import korablique.recipecalculator.RequestCodes;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.GoalCalculator;
import korablique.recipecalculator.model.HistoryEntry;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.NewHistoryEntry;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.Rates;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.model.WeightedFoodstuff;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenController;
import korablique.recipecalculator.util.EspressoUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
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
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest extends MainActivityTestsBase {
    @Test
    public void topHeaderDoNotDisplayedIfHistoryIsEmpty() {
        historyWorker.requestAllHistoryFromDb(historyEntries -> {
            for (HistoryEntry entry : historyEntries) {
                historyWorker.deleteEntryFromHistory(entry);
            }
        });
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
    public void addedToHistoryFoodstuffs_appearInTop() {
        clearAllData();
        mActivityRule.launchActivity(null);
        onView(withText(R.string.top_header)).check(doesNotExist());

        // Сохраним продукт в Историю
        AtomicReference<Foodstuff> newFoodstuff = new AtomicReference<>(
                Foodstuff.withName("new foodstuff").withNutrition(1, 2, 3, 4));
        foodstuffsList.saveFoodstuff(newFoodstuff.get(), new FoodstuffsList.SaveFoodstuffCallback() {
            @Override
            public void onResult(Foodstuff addedFoodstuff) {
                newFoodstuff.set(addedFoodstuff);
            }
            @Override public void onDuplication() {}
        });
        historyWorker.saveFoodstuffToHistory(timeProvider.now().toDate(), newFoodstuff.get().getId(), 123);

        // Проверим, что появился топ и продукт в нём.
        onView(withText(R.string.top_header)).check(matches(isDisplayed()));
        onView(allOf(
                withText(newFoodstuff.get().getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header)))
        )).check(matches(isDisplayed()));
    }

    @Test
    public void topDisappears_whenHistoryIsCleaned() {
        mActivityRule.launchActivity(null);

        List<Foodstuff> topFoodstuffs = extractFoodstuffsTopFromDB();
        // Проверим, что заголовок топа есть
        onView(withText(R.string.top_header)).check(matches(isDisplayed()));
        // Проверим, что продукт есть в топе
        onView(allOf(
                withText(topFoodstuffs.get(0).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header)))
        )).check(matches(isDisplayed()));

        // Очистим историю
        historyWorker.requestAllHistoryFromDb((allHistory)->{
            for (HistoryEntry entry : allHistory) {
                historyWorker.deleteEntryFromHistory(entry);
            }
        });

        // Проверим, что щаголовок топа пропал
        onView(withText(R.string.top_header)).check(doesNotExist());
        // Проверим, что продукта в топе больше нет
        onView(allOf(
                withText(topFoodstuffs.get(0).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header)))
        )).check(doesNotExist());
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
        // также что BucketList содержит все необходимые продукты.
        Intent expectedIntent =
                BucketListActivity.createIntent(mActivityRule.getActivity());
        intended(allOf(
                hasAction(expectedIntent.getAction()),
                hasComponent(expectedIntent.getComponent())));

        ArrayList<WeightedFoodstuff> weightedFoodstuffs = new ArrayList<>();
        weightedFoodstuffs.add(clickedFoodstuffs.get(0).withWeight(123));
        weightedFoodstuffs.add(clickedFoodstuffs.get(1).withWeight(123));
        weightedFoodstuffs.add(clickedFoodstuffs.get(2).withWeight(123));
        mainThreadExecutor.execute(() -> {
            Assert.assertEquals(weightedFoodstuffs, bucketList.getList());
        });
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
                withText(topFoodstuffs.get(1).getName()),
                matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))),
                matches(isCompletelyBelow(withText(R.string.top_header))));
        onView(topMatcher).perform(click());
        onView(withId(R.id.button_edit)).perform(click());

        // Редактируем
        String newName = topFoodstuffs.get(1).getName() + "1";
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
    public void topProducts_takenFromWeeklyTop() {
        clearAllData();
        mActivityRule.launchActivity(null);

        Foodstuff foodstuff1 = Foodstuff.withName("apple1").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff2 = Foodstuff.withName("apple2").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff3 = Foodstuff.withName("apple3").withNutrition(1, 2, 3, 4);
        Foodstuff foodstuff4 = Foodstuff.withName("apple4").withNutrition(1, 2, 3, 4);
        foodstuff1 = foodstuffsList.saveFoodstuff(foodstuff1).blockingGet();
        foodstuff2 = foodstuffsList.saveFoodstuff(foodstuff2).blockingGet();
        foodstuffsList.saveFoodstuff(foodstuff3);
        foodstuffsList.saveFoodstuff(foodstuff4);

        historyWorker.saveFoodstuffToHistory(
                timeProvider.now().minusDays(3).toDate(),
                foodstuff1.getId(),
                123);
        historyWorker.saveFoodstuffToHistory(
                timeProvider.now().minusWeeks(2).toDate(),
                foodstuff2.getId(),
                123);

        Matcher<View> topHeaderMatcher = withText(R.string.top_header);
        Matcher<View> allHeaderMatcher = withText(R.string.all_foodstuffs_header);
        onView(topHeaderMatcher).check(matches(isDisplayed()));
        onView(allHeaderMatcher).check(matches(isDisplayed()));

        // foodstuff1 в истории менее недели в прошлом
        onView(allOf(
                withText(foodstuff1.getName()),
                EspressoUtils.matches(isCompletelyBelow(topHeaderMatcher)),
                EspressoUtils.matches(isCompletelyAbove(allHeaderMatcher))
        )).check(matches(isDisplayed()));

        // foodstuff2 в истории более недели в прошлом
        onView(allOf(
                withText(foodstuff2.getName()),
                isDescendantOfA(withId(R.id.search_results_layout)),
                EspressoUtils.matches(isCompletelyBelow(topHeaderMatcher)),
                EspressoUtils.matches(isCompletelyAbove(allHeaderMatcher))
        )).check(doesNotExist());

        // foodstuff3 не в истории
        onView(allOf(
                withText(foodstuff3.getName()),
                isDescendantOfA(withId(R.id.search_results_layout)),
                EspressoUtils.matches(isCompletelyBelow(topHeaderMatcher)),
                EspressoUtils.matches(isCompletelyAbove(allHeaderMatcher))
        )).check(doesNotExist());

        // foodstuff4 не в истории
        onView(allOf(
                withText(foodstuff4.getName()),
                isDescendantOfA(withId(R.id.search_results_layout)),
                EspressoUtils.matches(isCompletelyBelow(topHeaderMatcher)),
                EspressoUtils.matches(isCompletelyAbove(allHeaderMatcher))
        )).check(doesNotExist());
    }

    @Test
    public void topProducts_orderAndLimit() {
        clearAllData();
        mActivityRule.launchActivity(null);

        List<Foodstuff> foodstuffs = new ArrayList<>();
        int eatenTwiceFoodstuffIndex = 2;

        // TOP_ITEMS_MAX_COUNT x2 products to history
        for (int index = 0; index < MainScreenController.TOP_ITEMS_MAX_COUNT + 2; ++index) {
            Foodstuff foodstuff = Foodstuff.withName("apple" + index).withNutrition(1, 2, 3, 4);
            foodstuff = foodstuffsList.saveFoodstuff(foodstuff).blockingGet();
            foodstuffs.add(foodstuff);

            historyWorker.saveFoodstuffToHistory(
                    timeProvider.now().minusSeconds(index).toDate(),
                    foodstuff.getId(),
                    123);
            // One of the foodstuffs is eaten twice
            if (index == eatenTwiceFoodstuffIndex) {
                historyWorker.saveFoodstuffToHistory(
                        timeProvider.now().minusSeconds(index).toDate(),
                        foodstuff.getId(),
                        123);
            }
        }

        // eatenTwiceFoodstuffIndex is first
        onView(allOf(
                withText(foodstuffs.get(eatenTwiceFoodstuffIndex).getName()),
                EspressoUtils.matches(isCompletelyBelow(withText(R.string.top_header))),
                EspressoUtils.matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))
        )).check(matches(isDisplayed()));

        // Other foodstuffs are below one by one
        for (int index = 0; index < MainScreenController.TOP_ITEMS_MAX_COUNT; ++index) {
            if (index == eatenTwiceFoodstuffIndex) {
                continue;
            }
            int foodstuffAboveIndex;
            if (index == 0) {
                foodstuffAboveIndex = eatenTwiceFoodstuffIndex;
            } else {
                foodstuffAboveIndex = index - 1;
            }
            Matcher<View> foodstuffAbove = allOf(
                    withText(foodstuffs.get(foodstuffAboveIndex).getName()),
                    EspressoUtils.matches(isCompletelyBelow(withText(R.string.top_header))),
                    EspressoUtils.matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header))));

            onView(allOf(
                    withText(foodstuffs.get(index).getName()),
                    EspressoUtils.matches(isCompletelyBelow(foodstuffAbove)),
                    EspressoUtils.matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))
            )).check(matches(isDisplayed()));
        }

        // Only TOP_ITEMS_MAX_COUNT foodstuffs should exist, others - shouldn't
        for (int index = MainScreenController.TOP_ITEMS_MAX_COUNT; index < foodstuffs.size(); ++index) {
            onView(allOf(
                    withText(foodstuffs.get(index).getName()),
                    EspressoUtils.matches(isCompletelyBelow(withText(R.string.top_header))),
                    EspressoUtils.matches(isCompletelyAbove(withText(R.string.all_foodstuffs_header)))
            )).check(doesNotExist());
        }
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
        return topList.getMonthTop().blockingGet();
    }
}
