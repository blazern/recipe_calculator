package korablique.recipecalculator.dagger;

import android.content.Context;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import korablique.recipecalculator.BroccalcApplication;
import korablique.recipecalculator.ui.mainscreen.MainScreenActivityModule;

@Singleton
@Component(modules = {
        BroccalcApplicationModule.class })
public interface BroccalcApplicationComponent extends AndroidInjector<BroccalcApplication> {
    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder context(Context context);
        BroccalcApplicationComponent build();
    }

    void inject(BroccalcApplication app);
}
