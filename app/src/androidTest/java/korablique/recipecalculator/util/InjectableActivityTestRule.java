package korablique.recipecalculator.util;

import android.content.Intent;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import androidx.annotation.Nullable;
import androidx.test.espresso.intent.Intents;
import androidx.test.rule.ActivityTestRule;
import korablique.recipecalculator.base.BaseActivity;
import korablique.recipecalculator.dagger.InjectorHolder;
import korablique.recipecalculator.util.TestingInjector.ActivitiesInjectionSource;
import korablique.recipecalculator.util.TestingInjector.FragmentInjectionSource;
import korablique.recipecalculator.util.TestingInjector.SingletonInjectionsSource;


/**
 * Rule для Espresso, которое необходимо использовать вместо ActivityTestRule, если Активити
 * содержит какие-либо @Inject-поля, которые требуется проинициилизировать самостоятельно,
 * мимо Даггера.
 */
public class InjectableActivityTestRule<T extends BaseActivity> extends ActivityTestRule<T> {
    private static boolean intentsLibInitialized;
    @Nullable
    private final SingletonInjectionsSource singletonInjectionsSource;
    @Nullable
    private final ActivitiesInjectionSource activitiesInjectionSource;
    @Nullable
    private final FragmentInjectionSource fragmentInjectionSource;

    public static <BT extends BaseActivity> Builder<BT> forActivity(Class<BT> activityClass) {
        return new Builder<>(activityClass);
    }

    private InjectableActivityTestRule(Builder<T> builder) {
        super(builder.activityClass, false /* initialTouchMode */, builder.shouldStartImmediately);
        this.singletonInjectionsSource = builder.singletonInjectionsSource;
        this.activitiesInjectionSource = builder.activitiesInjectionSource;
        this.fragmentInjectionSource = builder.fragmentInjectionSource;
    }

    private void onTestStarted() {
        // Initializing espresso-intents
        if (intentsLibInitialized) {
            Intents.release();
        }
        Intents.init();
        intentsLibInitialized = true;
        InjectorHolder.setInjector(
                new TestingInjector(singletonInjectionsSource, activitiesInjectionSource, fragmentInjectionSource));
    }

    private void onTestEnded() {
        Intents.release();
        intentsLibInitialized = false;
        InjectorHolder.setInjector(null);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new MyStatement(super.apply(base, description));
    }

    // Паттерн 'Builder'.
    public static class Builder<BT extends BaseActivity> {
        private final Class<BT> activityClass;
        @Nullable
        private SingletonInjectionsSource singletonInjectionsSource;
        @Nullable
        private ActivitiesInjectionSource activitiesInjectionSource;
        @Nullable
        private FragmentInjectionSource fragmentInjectionSource;
        private boolean shouldStartImmediately = true;

        private Builder(Class<BT> activityClass) {
            this.activityClass = activityClass;
        }

        /**
         * Устанавливаем форсированный ручной старт - активити не стартует сама до вызова
         * {@link ActivityTestRule#launchActivity(Intent)}.
         */
        public Builder<BT> withManualStart() {
            this.shouldStartImmediately = false;
            return this;
        }

        /**
         * Устанавливаем источник синглтонов.
         */
        public Builder<BT> withSingletones(SingletonInjectionsSource source) {
            this.singletonInjectionsSource = source;
            return this;
        }

        /**
         * Устанавливаем источник ActivityScoped зависимостей.
         */
        public Builder<BT> withActivityScoped(ActivitiesInjectionSource source) {
            this.activitiesInjectionSource = source;
            return this;
        }

        public Builder<BT> withFragmentScoped(FragmentInjectionSource source) {
            this.fragmentInjectionSource = source;
            return this;
        }

        public InjectableActivityTestRule<BT> build() {
            return new InjectableActivityTestRule<>(this);
        }
    }

    private class MyStatement extends Statement {
        private final Statement base;
        MyStatement(Statement base) {
            this.base = base;
        }
        @Override
        public void evaluate() throws Throwable {
            onTestStarted();
            try {
                base.evaluate();
            } finally {
                onTestEnded();
            }
        }
    }
}
