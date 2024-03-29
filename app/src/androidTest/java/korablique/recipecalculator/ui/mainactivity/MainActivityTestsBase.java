package korablique.recipecalculator.ui.mainactivity;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import korablique.recipecalculator.FakeHttpClient;
import korablique.recipecalculator.FakeNetworkStateDispatcher;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.base.BaseFragment;
import korablique.recipecalculator.base.CurrentActivityProvider;
import korablique.recipecalculator.base.FragmentCallbacks;
import korablique.recipecalculator.base.RxActivitySubscriptions;
import korablique.recipecalculator.base.RxFragmentSubscriptions;
import korablique.recipecalculator.base.RxGlobalSubscriptions;
import korablique.recipecalculator.base.SoftKeyboardStateWatcher;
import korablique.recipecalculator.base.executors.ComputationThreadsExecutor;
import korablique.recipecalculator.base.executors.IOExecutor;
import korablique.recipecalculator.base.executors.MainThreadExecutor;
import korablique.recipecalculator.base.prefs.PrefsCleaningHelper;
import korablique.recipecalculator.base.prefs.SharedPrefsManager;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.RecipeDatabaseWorker;
import korablique.recipecalculator.database.RecipeDatabaseWorkerImpl;
import korablique.recipecalculator.database.RecipesRepository;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.database.room.DatabaseHolder;
import korablique.recipecalculator.model.Foodstuff;
import korablique.recipecalculator.model.FoodstuffsTopList;
import korablique.recipecalculator.model.Formula;
import korablique.recipecalculator.model.Gender;
import korablique.recipecalculator.model.Lifestyle;
import korablique.recipecalculator.model.Nutrition;
import korablique.recipecalculator.model.RateCalculator;
import korablique.recipecalculator.model.UserParameters;
import korablique.recipecalculator.outside.fcm.FCMManager;
import korablique.recipecalculator.outside.http.BroccalcHttpContext;
import korablique.recipecalculator.outside.partners.PartnersRegistry;
import korablique.recipecalculator.outside.partners.direct.DirectMsgsManager;
import korablique.recipecalculator.outside.partners.direct.FoodstuffsCorrespondenceManager;
import korablique.recipecalculator.outside.userparams.InteractiveServerUserParamsObtainer;
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry;
import korablique.recipecalculator.search.FoodstuffsSearchEngine;
import korablique.recipecalculator.session.SessionController;
import korablique.recipecalculator.ui.TwoOptionsDialog;
import korablique.recipecalculator.ui.bucketlist.BucketList;
import korablique.recipecalculator.ui.bucketlist.BucketListActivity;
import korablique.recipecalculator.ui.bucketlist.BucketListActivityController;
import korablique.recipecalculator.ui.calckeyboard.CalcKeyboardController;
import korablique.recipecalculator.ui.card.CardDialog;
import korablique.recipecalculator.ui.mainactivity.history.HistoryController;
import korablique.recipecalculator.ui.mainactivity.history.HistoryFragment;
import korablique.recipecalculator.ui.mainactivity.history.pages.HistoryPageController;
import korablique.recipecalculator.ui.mainactivity.history.pages.HistoryPageFragment;
import korablique.recipecalculator.ui.mainactivity.history.pages.HistoryViewHoldersPool;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenCardController;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenController;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenFragment;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenHistoryAdditionController;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenReadinessDispatcher;
import korablique.recipecalculator.ui.mainactivity.mainscreen.MainScreenSearchController;
import korablique.recipecalculator.ui.mainactivity.mainscreen.SearchResultsFragment;
import korablique.recipecalculator.ui.mainactivity.mainscreen.UpFABController;
import korablique.recipecalculator.ui.mainactivity.mainscreen.modes.MainScreenModesController;
import korablique.recipecalculator.ui.mainactivity.mainscreen.modes.MainScreenModesMenuController;
import korablique.recipecalculator.ui.mainactivity.partners.PartnersListFragment;
import korablique.recipecalculator.ui.mainactivity.partners.PartnersListFragmentController;
import korablique.recipecalculator.ui.mainactivity.partners.pairing.PairingFragment;
import korablique.recipecalculator.ui.mainactivity.partners.pairing.PairingFragmentController;
import korablique.recipecalculator.ui.mainactivity.profile.NewMeasurementsDialog;
import korablique.recipecalculator.ui.mainactivity.profile.ProfileController;
import korablique.recipecalculator.ui.mainactivity.profile.ProfileFragment;
import korablique.recipecalculator.ui.netsnack.NetworkSnackbarControllersFactory;
import korablique.recipecalculator.util.DBTestingUtils;
import korablique.recipecalculator.FakeFCMTokenProvider;
import korablique.recipecalculator.util.InjectableActivityTestRule;
import korablique.recipecalculator.InstantComputationsThreadsExecutor;
import korablique.recipecalculator.InstantDatabaseThreadExecutor;
import korablique.recipecalculator.InstantIOExecutor;
import korablique.recipecalculator.util.SyncMainThreadExecutor;
import korablique.recipecalculator.util.FakeGPAuthorizer;
import korablique.recipecalculator.util.TestingTimeProvider;

public class MainActivityTestsBase {
    protected DatabaseThreadExecutor databaseThreadExecutor = new InstantDatabaseThreadExecutor();
    protected IOExecutor ioExecutor = new InstantIOExecutor();
    protected ComputationThreadsExecutor computationThreadsExecutor =
            new InstantComputationsThreadsExecutor();
    protected MainThreadExecutor mainThreadExecutor = new SyncMainThreadExecutor();
    protected DatabaseHolder databaseHolder;
    protected DatabaseWorker databaseWorker;
    protected HistoryWorker historyWorker;
    protected UserParametersWorker userParametersWorker;
    protected FoodstuffsList foodstuffsList;
    protected FoodstuffsTopList topList;
    protected Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    protected Foodstuff[] foodstuffs;
    protected List<Long> foodstuffsIds = new ArrayList<>();
    protected BucketList bucketList;
    protected UserParameters userParameters;
    protected TestingTimeProvider timeProvider;
    protected MainActivityFragmentsController fragmentsController;
    protected MainActivitySelectedDateStorage mainActivitySelectedDateStorage;
    protected RxActivitySubscriptions activitySubscriptions;
    protected CurrentActivityProvider currentActivityProvider;
    protected SessionController sessionController;
    protected MainScreenCardController mainScreenCardController;
    protected MainScreenModesController mainScreenModesController;
    protected MainScreenModesMenuController mainScreenModesMenuController;
    protected SharedPrefsManager prefsManager;
    protected SoftKeyboardStateWatcher softKeyboardStateWatcher;
    protected FoodstuffsSearchEngine foodstuffsSearchEngine;
    protected ServerUserParamsRegistry serverUserParamsRegistry;
    protected FakeHttpClient fakeHttpClient;
    protected BroccalcHttpContext httpContext;
    protected FakeGPAuthorizer fakeGPAuthorizer;
    protected HistoryViewHoldersPool historyViewHoldersPool;
    protected PartnersRegistry partnersRegistry;
    protected FakeNetworkStateDispatcher fakeNetworkStateDispatcher;
    protected FCMManager fcmManager;
    protected InteractiveServerUserParamsObtainer interactiveServerUserParamsObtainer;
    protected FoodstuffsCorrespondenceManager foodstuffsCorrespondenceManager;
    protected DirectMsgsManager directMsgsManager;
    protected NetworkSnackbarControllersFactory networkSnackbarControllersFactory;
    protected RecipeDatabaseWorker recipeDatabaseWorker;
    protected RecipesRepository recipesRepository;
    protected CalcKeyboardController calcKeyboardController;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            InjectableActivityTestRule.forActivity(MainActivity.class)
                    .withManualStart()
                    .withSingletones(() -> {
                        PrefsCleaningHelper.INSTANCE.cleanAllPrefs(context);
                        prefsManager = new SharedPrefsManager(context);
                        timeProvider = new TestingTimeProvider();
                        databaseHolder = new DatabaseHolder(context, timeProvider, databaseThreadExecutor);
                        databaseWorker = new DatabaseWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor);
                        historyWorker = new HistoryWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor,
                                timeProvider);
                        userParametersWorker = new UserParametersWorker(
                                databaseHolder, mainThreadExecutor, databaseThreadExecutor,
                                new RxGlobalSubscriptions(), timeProvider);
                        foodstuffsList = new FoodstuffsList(
                                databaseWorker,
                                () -> recipesRepository,
                                mainThreadExecutor,
                                computationThreadsExecutor,
                                new RxGlobalSubscriptions());
                        topList = new FoodstuffsTopList(historyWorker, foodstuffsList, timeProvider);
                        currentActivityProvider = new CurrentActivityProvider();
                        sessionController = new SessionController(context, timeProvider, currentActivityProvider);
                        bucketList = new BucketList(prefsManager);
                        foodstuffsSearchEngine = new FoodstuffsSearchEngine(
                                foodstuffsList, topList, computationThreadsExecutor,
                                mainThreadExecutor);

                        fakeHttpClient = new FakeHttpClient();
                        httpContext = new BroccalcHttpContext(fakeHttpClient);
                        fakeGPAuthorizer = new FakeGPAuthorizer();
                        serverUserParamsRegistry =
                                new ServerUserParamsRegistry(
                                        context,
                                        mainThreadExecutor, ioExecutor, fakeGPAuthorizer,
                                        userParametersWorker, httpContext, prefsManager);

                        fakeNetworkStateDispatcher = new FakeNetworkStateDispatcher();
                        fakeNetworkStateDispatcher.setNetworkAvailable(true);

                        fcmManager =
                                new FCMManager(
                                        context, mainThreadExecutor,
                                        fakeNetworkStateDispatcher, httpContext, serverUserParamsRegistry,
                                        new FakeFCMTokenProvider(() -> "fcmtoken"));

                        partnersRegistry =
                                new PartnersRegistry(
                                        context, mainThreadExecutor, fakeNetworkStateDispatcher,
                                        serverUserParamsRegistry, httpContext, fcmManager);

                        directMsgsManager =
                                new DirectMsgsManager(
                                        context, fcmManager, serverUserParamsRegistry, httpContext);
                        foodstuffsCorrespondenceManager =
                                new FoodstuffsCorrespondenceManager(
                                        directMsgsManager, foodstuffsList, currentActivityProvider,
                                        new RxGlobalSubscriptions());

                        networkSnackbarControllersFactory = new NetworkSnackbarControllersFactory(
                                context, fakeNetworkStateDispatcher);

                        recipeDatabaseWorker = new RecipeDatabaseWorkerImpl(
                                ioExecutor, databaseHolder, databaseWorker);
                        recipesRepository = new RecipesRepository(
                                recipeDatabaseWorker, foodstuffsList, mainThreadExecutor);

                        calcKeyboardController = new CalcKeyboardController(context, prefsManager);

                        return Arrays.asList(databaseWorker, historyWorker, userParametersWorker,
                                foodstuffsList, databaseHolder,
                                timeProvider, currentActivityProvider, sessionController,
                                calcKeyboardController, bucketList, foodstuffsSearchEngine,
                                serverUserParamsRegistry, httpContext, recipesRepository,
                                mainThreadExecutor, prefsManager);
                    })
                    .withActivityScoped((injectionTarget) -> {
                        if (!(injectionTarget instanceof MainActivity)) {
                            if (injectionTarget instanceof BucketListActivity) {
                                BucketListActivity activity = (BucketListActivity) injectionTarget;
                                BucketListActivityController bucketListActivityController =
                                        new BucketListActivityController(
                                                activity, databaseWorker, recipesRepository, bucketList,
                                                mainThreadExecutor, calcKeyboardController, timeProvider);
                                return Arrays.asList(bucketListActivityController);
                            }
                            return Collections.emptyList();
                        }
                        MainActivity activity = (MainActivity) injectionTarget;
                        ActivityCallbacks activityCallbacks = activity.getActivityCallbacks();
                        mainActivitySelectedDateStorage = new MainActivitySelectedDateStorage(
                                activity, activityCallbacks, sessionController, timeProvider);
                        fragmentsController = new MainActivityFragmentsController(
                                activity, sessionController, activityCallbacks);
                        softKeyboardStateWatcher = new SoftKeyboardStateWatcher(activity, mainThreadExecutor);
                        MainActivityController controller = new MainActivityController(
                                activity, activityCallbacks, fragmentsController);
                        activitySubscriptions = new RxActivitySubscriptions(activityCallbacks);

                        historyViewHoldersPool = new HistoryViewHoldersPool(
                                computationThreadsExecutor, mainThreadExecutor, activity);

                        interactiveServerUserParamsObtainer =
                                new InteractiveServerUserParamsObtainer(
                                        activity, activityCallbacks, serverUserParamsRegistry,
                                        userParametersWorker);

                        return Arrays.asList(activity, controller, interactiveServerUserParamsObtainer);
                    })
                    .withFragmentScoped((injectionTarget -> {
                        if (injectionTarget instanceof NewMeasurementsDialog
                                || injectionTarget instanceof CardDialog
                                || injectionTarget instanceof TwoOptionsDialog) {
                            return Collections.singletonList(prefsManager);
                        }

                        BaseFragment fragment = (BaseFragment) injectionTarget;
                        FragmentCallbacks fragmentCallbacks = fragment.getFragmentCallbacks();
                        RxFragmentSubscriptions subscriptions = new RxFragmentSubscriptions(fragmentCallbacks);
                        BaseActivity activity = (BaseActivity) fragment.getActivity();
                        Lifecycle lifecycle = activity.getLifecycle();

                        if (fragment instanceof MainScreenFragment) {
                            MainScreenReadinessDispatcher readinessDispatcher =
                                    new MainScreenReadinessDispatcher();

                            mainScreenModesController = new MainScreenModesController(
                                    fragment, fragmentCallbacks, bucketList, recipesRepository,
                                    foodstuffsCorrespondenceManager);
                            mainScreenModesMenuController = new MainScreenModesMenuController(
                                    activity, fragment, fragmentCallbacks, activity.getActivityCallbacks(),
                                    mainScreenModesController, bucketList, recipesRepository,
                                    serverUserParamsRegistry, foodstuffsCorrespondenceManager);
                            MainScreenHistoryAdditionController historyAdditionController =
                                    new MainScreenHistoryAdditionController(
                                            activity, timeProvider, mainActivitySelectedDateStorage,
                                            historyWorker, fragmentCallbacks);

                            mainScreenCardController = new MainScreenCardController(
                                    activity, fragment, fragmentCallbacks, lifecycle,
                                    historyWorker, timeProvider,
                                    mainActivitySelectedDateStorage, recipesRepository,
                                    foodstuffsList, subscriptions, mainScreenModesController,
                                    historyAdditionController, bucketList);

                            MainScreenSearchController searchController = new MainScreenSearchController(
                                    mainThreadExecutor, bucketList, foodstuffsList, foodstuffsSearchEngine,
                                    (MainScreenFragment) fragment, activity.getActivityCallbacks(),
                                    fragmentCallbacks, mainScreenCardController, readinessDispatcher,
                                    subscriptions, softKeyboardStateWatcher, fragmentsController);

                            UpFABController upFABController = new UpFABController(
                                    fragmentCallbacks, readinessDispatcher);

                            MainScreenController mainScreenController = new MainScreenController(
                                    activity, fragment, fragmentCallbacks,
                                    activity.getActivityCallbacks(), bucketList, topList,
                                    foodstuffsList, mainActivitySelectedDateStorage,
                                    mainScreenCardController, readinessDispatcher,
                                    subscriptions, fragmentsController, mainScreenModesController,
                                    historyAdditionController);
                            return Arrays.asList(subscriptions, mainScreenController,
                                    upFABController, mainScreenCardController, searchController,
                                    mainScreenModesController, mainScreenModesMenuController,
                                    historyAdditionController);

                        } else if (fragment instanceof ProfileFragment) {
                            ProfileController profileController = new ProfileController(
                                    activity, fragment, fragmentCallbacks, userParametersWorker, subscriptions,
                                    timeProvider);
                            return Arrays.asList(subscriptions, profileController);
                        } else if (fragment instanceof HistoryFragment) {
                            HistoryController historyController = new HistoryController(
                                    activity, fragment, fragmentCallbacks, historyWorker,
                                    timeProvider, fragmentsController, mainActivitySelectedDateStorage);
                            return Arrays.asList(subscriptions, historyController);
                        } else if (fragment instanceof SearchResultsFragment) {
                            return Arrays.asList(databaseWorker, lifecycle, activity,
                                    foodstuffsList, subscriptions, mainScreenCardController);
                        } else if (fragment instanceof HistoryPageFragment) {
                            HistoryPageController pageController =
                                    new HistoryPageController(
                                            (HistoryPageFragment) fragment, historyWorker,
                                            userParametersWorker, mainActivitySelectedDateStorage,
                                            fragment.getFragmentCallbacks(),
                                            new RxFragmentSubscriptions(fragment.getFragmentCallbacks()),
                                            historyViewHoldersPool, computationThreadsExecutor,
                                            mainThreadExecutor);
                            return Arrays.asList(pageController);
                        } else if (fragment instanceof PartnersListFragment) {
                            PartnersListFragmentController controller =
                                    new PartnersListFragmentController(
                                            mainThreadExecutor, activity, (PartnersListFragment) fragment,
                                            fragment.getFragmentCallbacks(), serverUserParamsRegistry,
                                            partnersRegistry, interactiveServerUserParamsObtainer,
                                            foodstuffsCorrespondenceManager,
                                            networkSnackbarControllersFactory);
                            return Arrays.asList(controller);
                        } else if (fragment instanceof PairingFragment) {
                            PairingFragmentController controller =
                                    new PairingFragmentController(
                                            fragment.getFragmentCallbacks(), (PairingFragment) fragment,
                                            httpContext, serverUserParamsRegistry,
                                            partnersRegistry, timeProvider, mainThreadExecutor);
                            return Arrays.asList(controller);
                        } else {
                            throw new IllegalStateException("Unhandled fragment: " + fragment.getClass().getName());
                        }
                    }))
                    .build();

    @Before
    public void setUp() {
        clearAllData();

        mainThreadExecutor.execute(() -> {
            bucketList.clear();
        });

        foodstuffs = new Foodstuff[7];
        foodstuffs[0] = Foodstuff.withName("apple").withNutrition(1, 1, 1, 1);
        foodstuffs[1] = Foodstuff.withName("pineapple").withNutrition(1, 1, 1, 1);
        foodstuffs[2] = Foodstuff.withName("plum").withNutrition(1, 1, 1, 1);
        foodstuffs[3] = Foodstuff.withName("water").withNutrition(1, 1, 1, 1);
        foodstuffs[4] = Foodstuff.withName("soup").withNutrition(1, 1, 1, 1);
        foodstuffs[5] = Foodstuff.withName("bread").withNutrition(1, 1, 1, 1);
        foodstuffs[6] = Foodstuff.withName("banana").withNutrition(1, 1, 1, 1);

        for (int index = 0; index < foodstuffs.length; ++index) {
            final int finalIndex = index;
            foodstuffsList.saveFoodstuff(foodstuffs[index], new FoodstuffsList.SaveFoodstuffCallback() {
                @Override
                public void onResult(Foodstuff addedFoodstuff) {
                    foodstuffsIds.add(addedFoodstuff.getId());
                    foodstuffs[finalIndex] = addedFoodstuff;
                }
                @Override public void onDuplication() {}
            });
        }


        // 1 day: apple, bread, banana
        historyWorker.saveFoodstuffToHistory(timeProvider.now().minusYears(10).toDate(), foodstuffsIds.get(0), 100);
        historyWorker.saveFoodstuffToHistory(timeProvider.now().minusYears(10).toDate(), foodstuffsIds.get(5), 100);
        historyWorker.saveFoodstuffToHistory(timeProvider.now().minusYears(10).toDate(), foodstuffsIds.get(6), 100);
        // 2 day: apple, water
        historyWorker.saveFoodstuffToHistory(timeProvider.now().minusYears(9).toDate(), foodstuffsIds.get(0), 100);
        historyWorker.saveFoodstuffToHistory(timeProvider.now().minusYears(9).toDate(), foodstuffsIds.get(3), 100);
        // 3 day: bread, soup
        historyWorker.saveFoodstuffToHistory(timeProvider.now().minusYears(8).toDate(), foodstuffsIds.get(5), 100);
        historyWorker.saveFoodstuffToHistory(timeProvider.now().minusYears(8).toDate(), foodstuffsIds.get(4), 100);
        // 4 day: apple, pineapple, water
        historyWorker.saveFoodstuffToHistory(timeProvider.now().minusDays(7).toDate(), foodstuffsIds.get(0), 100);
        historyWorker.saveFoodstuffToHistory(timeProvider.now().minusDays(7).toDate(), foodstuffsIds.get(1), 100);
        historyWorker.saveFoodstuffToHistory(timeProvider.now().minusDays(7).toDate(), foodstuffsIds.get(3), 100);

        // сохраняем userParameters в БД
        userParameters = new UserParameters(
                "John Doe", 45, Gender.FEMALE, new LocalDate(1993, 9, 27),
                158, 48, Lifestyle.PASSIVE_LIFESTYLE, Formula.HARRIS_BENEDICT,
                Nutrition.withValues(100, 130, 200, 1700),
                timeProvider.nowUtc().getMillis());
        userParametersWorker.saveUserParameters(userParameters);

        // каждый тест должен сам сделать launchActivity()
    }

    protected void clearAllData() {
        DBTestingUtils.clearAllData(foodstuffsList, historyWorker, databaseHolder);
    }
}
