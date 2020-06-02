package korablique.recipecalculator.database.room.legacy;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.VisibleForTesting;

public class LegacyUserNameProvider {
    private static final String USER_FIRST_NAME = "USER_FIRST_NAME";
    private static final String USER_LAST_SURNAME = "USER_LAST_SURNAME";
    private Context context;
    public LegacyUserNameProvider(Context context) {
        this.context = context;
    }

    @VisibleForTesting
    public void saveUserName(LegacyFullName fullName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USER_FIRST_NAME, fullName.getFirstName());
        editor.putString(USER_LAST_SURNAME, fullName.getLastName());
        editor.apply();
    }

    public LegacyFullName getUserName() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String userFirstName = prefs.getString(USER_FIRST_NAME, "");
        String userLastName = prefs.getString(USER_LAST_SURNAME, "");
        return new LegacyFullName(userFirstName, userLastName);
    }
}
