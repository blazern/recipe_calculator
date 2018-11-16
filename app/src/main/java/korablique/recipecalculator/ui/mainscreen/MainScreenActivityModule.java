package korablique.recipecalculator.ui.mainscreen;

import android.arch.lifecycle.Lifecycle;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import korablique.recipecalculator.base.ActivityCallbacks;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.database.FoodstuffsList;
import korablique.recipecalculator.model.TopList;

@Module
public abstract class MainScreenActivityModule {
    @ActivityScope
    @Provides
    static MainScreenActivityController provideController(
            MainScreenActivity activity,
            FoodstuffsList foodstuffsList,
            TopList topList,
            ActivityCallbacks callbacks,
            Lifecycle lifecycle) {
        return new MainScreenActivityController(activity, foodstuffsList, topList, callbacks, lifecycle);
    }

    @FragmentScope
    @ContributesAndroidInjector
    abstract SearchResultsFragment searchResultsFragmentInjector();

    @ActivityScope
    @Provides
    static BaseActivity provideBaseActivity(MainScreenActivity activity) {
        return activity;
    }
}
