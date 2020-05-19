package korablique.recipecalculator.ui.settings;

import dagger.Module;
import dagger.Provides;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.ActivityScope;

@Module
public abstract class SettingsActivityModule {
    @ActivityScope
    @Provides
    static BaseActivity provideBaseActivity(SettingsActivity activity) {
        return activity;
    }
}
