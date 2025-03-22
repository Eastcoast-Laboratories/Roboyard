package roboyard.eclabs;

import android.app.Activity;
import android.content.SharedPreferences;
import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;


public class Preferences {
    private static final String PREFS_NAME = "RoboYard";
    private SharedPreferences preferences;

    public void setPreferences(Activity activity, String key, String value){
        preferences = activity.getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor preferencesEditor = preferences.edit();

        Timber.d("[BOARD_SIZE_DEBUG] Preferences.setPreferences(): saving %s = %s to %s", key, value, PREFS_NAME);
        preferencesEditor.putString(key, value);
        preferencesEditor.apply();
    }

    public String getPreferenceValue(Activity activity, String key){
        SharedPreferences preferences = activity.getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String value = preferences.getString(key,"");
        Timber.d("[BOARD_SIZE_DEBUG] Preferences.getPreferenceValue(): reading %s = %s from %s", key, value, PREFS_NAME);
        return value;
    }
}
