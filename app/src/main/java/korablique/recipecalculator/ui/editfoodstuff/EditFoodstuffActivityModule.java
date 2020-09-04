package korablique.recipecalculator.ui.editfoodstuff;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.ActivityScope;
import korablique.recipecalculator.dagger.FragmentScope;
import korablique.recipecalculator.ui.TwoOptionsDialog;
import korablique.recipecalculator.ui.mainactivity.MainActivity;

@Module
public abstract class EditFoodstuffActivityModule {
    @ActivityScope
    @Provides
    static BaseActivity provideBaseActivity(MainActivity activity) {
        return activity;
    }

    @FragmentScope
    @ContributesAndroidInjector
    abstract TwoOptionsDialog twoOptionsDialogInjector();
}
