package korablique.recipecalculator.util;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import junit.framework.AssertionFailedError;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.util.HumanReadables;

import java.util.Objects;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

public class EspressoUtils {
    private EspressoUtils() {
    }

    public static Matcher<View> matches(ViewAssertion assertion) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                try {
                    assertion.check(item, null);
                } catch (AssertionFailedError e) {
                    return false;
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(assertion.getClass().getCanonicalName());
            }
        };
    }

    /**
     * Recursively checks whether a Bundle has a given value somewhere inside of it, possibly deep
     * into subbundles (Bundle can contain another Bundle as an extra, creating a tree of Bundles).
     */
    public static <T> Matcher<Bundle> hasValueRecursive(T targetValue) {
        return new BaseMatcher<Bundle>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Matcher hasValueRecursive for value: " + targetValue.toString());
            }

            @Override
            public boolean matches(Object item) {
                Bundle bundle = (Bundle) item;
                if (areEqual(bundle, targetValue)) {
                    return true;
                }
                for (String key : bundle.keySet()) {
                    Object value = bundle.get(key);
                    if (areEqual(value, targetValue)) {
                        return true;
                    } else if (value instanceof Bundle && matches(value)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Compares given bundles field by field with each other
     * (the Bundle class doesn't override the equals() method).
     */
    private static boolean areBundlesEqual(Bundle lhs, Bundle rhs) {
        if (!lhs.keySet().equals(rhs.keySet())) {
            // Keys differ - bundles are not equal.
            return false;
        }
        for (String key : lhs.keySet()) {
            if (!areEqual(lhs.get(key), rhs.get(key))) {
                return false;
            }
        }
        return true;
    }

    private static boolean areEqual(Object lhs, Object rhs) {
        if (lhs instanceof Bundle && rhs instanceof Bundle) {
            return areBundlesEqual((Bundle)lhs, (Bundle)rhs);
        } else {
            return lhs.equals(rhs);
        }
    }

    public static ViewAssertion hasProgress(int value) {
        return (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw new AssertionError("View not found", noViewFoundException);
            }
            if (!(view instanceof ProgressBar)) {
                throw new AssertionError(
                        "Provided view is not ProgressBar: " + view.getClass().getName());
            }
            ProgressBar progressBar = (ProgressBar) view;
            if (progressBar.getProgress() != value) {
                throw new AssertionError(
                        "ProgressBar's value differs from expected: "
                                + progressBar.getProgress() + " vs " + value);
            }
        };
    }

    public static ViewAssertion hasMaxProgress(int value) {
        return (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw new AssertionError("View not found", noViewFoundException);
            }
            if (!(view instanceof ProgressBar)) {
                throw new AssertionError(
                        "Provided view is not ProgressBar: " + view.getClass().getName());
            }
            ProgressBar progressBar = (ProgressBar) view;
            if (progressBar.getMax() != value) {
                throw new AssertionError(
                        "ProgressBar's max value differs from expected: "
                                + progressBar.getMax() + " vs " + value);
            }
        };
    }

    public static ViewAssertion isNotDisplayed() {
        return new ViewAssertion() {
            @Override
            public void check(View view, NoMatchingViewException noView) {
                if (view != null && isDisplayed().matches(view)) {
                    throw new AssertionError("View is present in the hierarchy and Displayed: "
                            + HumanReadables.describe(view));
                }
            }
        };
    }
}
