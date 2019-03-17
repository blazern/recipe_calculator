package korablique.recipecalculator.ui.inputfilters;

import android.text.SpannedString;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.annotation.Nullable;
import korablique.recipecalculator.BuildConfig;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class NumericBoundsInputFilterTest {
    @Test
    public void works() {
        NumericBoundsInputFilter filter = NumericBoundsInputFilter.withBounds(-10, 10);

        Assert.assertEquals("", filterPastedText("-10.1", filter));
        Assert.assertNull(filterPastedText("-10", filter));
        Assert.assertNull(filterPastedText("10", filter));
        Assert.assertEquals("", filterPastedText("10.1", filter));
    }

    @Nullable
    private String filterPastedText(String text, NumericBoundsInputFilter filter) {
        CharSequence result = filter.filter(text, 0, text.length(), new SpannedString(""), 0, 0);
        if (result != null) {
            return result.toString();
        } else {
            return null;
        }
    }
}
