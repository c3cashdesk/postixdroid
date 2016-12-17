package de.ccc.events.c6shdroid;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfig {
    public static final String PREFS_NAME = "c6shdroid";
    public static final String PREFS_KEY_API_URL = "pretix_api_url";
    public static final String PREFS_KEY_API_KEY = "pretix_api_key";
    public static final String PREFS_KEY_FLASHLIGHT = "flashlight";
    public static final String PREFS_KEY_AUTOFOCUS = "autofocus";
    private SharedPreferences prefs;

    public AppConfig(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isConfigured() {
        return prefs.contains(PREFS_KEY_API_URL);
    }

    public void setEventConfig(String url, String key) {
        prefs.edit()
                .putString(PREFS_KEY_API_URL, url)
                .putString(PREFS_KEY_API_KEY, key)
                .apply();
    }

    public void resetEventConfig() {
        prefs.edit()
                .remove(PREFS_KEY_API_URL)
                .remove(PREFS_KEY_API_KEY)
                .apply();
    }

    public String getApiUrl() {
        return prefs.getString(PREFS_KEY_API_URL, "");
    }

    public String getApiKey() {
        return prefs.getString(PREFS_KEY_API_KEY, "");
    }

    public boolean getFlashlight() {
        return prefs.getBoolean(PREFS_KEY_FLASHLIGHT, false);
    }

    public boolean getAutofocus() {
        return prefs.getBoolean(PREFS_KEY_AUTOFOCUS, true);
    }

    public void setFlashlight(boolean val) {
        prefs.edit().putBoolean(PREFS_KEY_FLASHLIGHT, val).apply();
    }

    public void setAutofocus(boolean val) {
        prefs.edit().putBoolean(PREFS_KEY_AUTOFOCUS, val).apply();
    }
}
