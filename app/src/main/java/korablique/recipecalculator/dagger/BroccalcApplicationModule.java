package korablique.recipecalculator.dagger;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import korablique.recipecalculator.base.MainThreadExecutor;
import korablique.recipecalculator.database.DatabaseThreadExecutor;
import korablique.recipecalculator.database.DatabaseWorker;
import korablique.recipecalculator.database.HistoryWorker;
import korablique.recipecalculator.database.UserParametersWorker;
import korablique.recipecalculator.ui.calculator.CalculatorActivity;
import korablique.recipecalculator.ui.foodstuffslist.ListOfFoodstuffsActivity;
import korablique.recipecalculator.ui.history.HistoryActivity;
import korablique.recipecalculator.ui.usergoal.UserGoalActivity;

@Module(includes = {AndroidSupportInjectionModule.class})
public abstract class BroccalcApplicationModule {
    @Provides
    @Singleton
    public static MainThreadExecutor provideMainThreadExecutor() {
        return new MainThreadExecutor();
    }

    @Provides
    @Singleton
    public static DatabaseThreadExecutor provideDatabaseThreadExecutor() {
        return new DatabaseThreadExecutor();
    }

    @Provides
    @Singleton
    public static DatabaseWorker provideDatabaseWorker(
            MainThreadExecutor mainThreadExecutor, DatabaseThreadExecutor databaseThreadExecutor) {
        return new DatabaseWorker(mainThreadExecutor, databaseThreadExecutor);
    }

    @Provides
    @Singleton
    public static HistoryWorker provideHistoryWorker(
            Context context,
            MainThreadExecutor mainThreadExecutor,
            DatabaseThreadExecutor databaseThreadExecutor) {
        return new HistoryWorker(context, mainThreadExecutor, databaseThreadExecutor);
    }

    @Provides
    @Singleton
    public static UserParametersWorker provideUserParametersWorker(
            Context context,
            MainThreadExecutor mainThreadExecutor,
            DatabaseThreadExecutor databaseThreadExecutor) {
        return new UserParametersWorker(context, mainThreadExecutor, databaseThreadExecutor);
    }

    @ActivityScope
    @ContributesAndroidInjector
    abstract CalculatorActivity contributeCalculatorActivityInjector();

    @ActivityScope
    @ContributesAndroidInjector
    abstract ListOfFoodstuffsActivity contributeListOfFoodstuffsActivityInjector();

    @ActivityScope
    @ContributesAndroidInjector
    abstract HistoryActivity contributeHistoryActivityInjector();

    @ActivityScope
    @ContributesAndroidInjector
    abstract UserGoalActivity contributeUserGoalActivityInjector();
}
