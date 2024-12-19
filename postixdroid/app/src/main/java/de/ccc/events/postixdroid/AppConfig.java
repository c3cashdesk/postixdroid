package de.ccc.events.postixdroid;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

public class AppConfig {
    public static final String PREFS_NAME = "postixdroid";
    public static final String PREFS_KEY_API_URL = "postix_api_url";
    public static final String PREFS_KEY_API_KEY = "postix_api_key";
    public static final String PREFS_KEY_FLASHLIGHT = "flashlight";
    public static final String PREFS_KEY_AUTOFOCUS = "autofocus";
    public static final String PREFS_PLAY_AUDIO = "playaudio";
    public static final String PREFS_DHL = "dhl";
    public static final String PREFS_KEY_CAMERA = "camera";
    public static final String PREFS_WIFI_SSID = "wifi_ssid";
    public static final String PREFS_WIFI_USER = "wifi_user";
    public static final String PREFS_WIFI_PASS = "wifi_pass";
    private SharedPreferences prefs;

    public AppConfig(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isConfigured() {
        return prefs.contains(PREFS_KEY_API_URL);
    }

    public void setSessionConfig(String url, String key) {
        setSessionConfig(url, key, new JSONObject());
    }
    public void setSessionConfig(String url, String key, JSONObject wifi) {
        prefs.edit()
                .putString(PREFS_KEY_API_URL, url)
                .putString(PREFS_KEY_API_KEY, key)
                .apply();

        if (wifi.has("ssid") && wifi.has("user") && wifi.has("pass")) {
            prefs.edit()
                    .putString(PREFS_WIFI_SSID, wifi.optString("ssid"))
                    .putString(PREFS_WIFI_USER, wifi.optString("user"))
                    .putString(PREFS_WIFI_PASS, wifi.optString("pass"))
                    .apply();
        }
    }

    public void resetSessionConfig() {
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

    public String getWiFiSSID() {
        return prefs.getString(PREFS_WIFI_SSID, "");
    }

    public String getWiFiUser() {
        return prefs.getString(PREFS_WIFI_USER, "");
    }

    public String getWiFiPass() {
        return prefs.getString(PREFS_WIFI_PASS, "");
    }

    public boolean getFlashlight() {
        return prefs.getBoolean(PREFS_KEY_FLASHLIGHT, false);
    }

    public boolean getAutofocus() {
        return prefs.getBoolean(PREFS_KEY_AUTOFOCUS, true);
    }

    public boolean getSoundEnabled() {
        return prefs.getBoolean(PREFS_PLAY_AUDIO, true);
    }

    public boolean getDHLEnabled() {
        return prefs.getBoolean(PREFS_DHL, false);
    }

    public void setFlashlight(boolean val) {
        prefs.edit().putBoolean(PREFS_KEY_FLASHLIGHT, val).apply();
    }

    public void setAutofocus(boolean val) {
        prefs.edit().putBoolean(PREFS_KEY_AUTOFOCUS, val).apply();
    }

    public void setSoundEnabled(boolean val) {
        prefs.edit().putBoolean(PREFS_PLAY_AUDIO, val).apply();
    }

    public void setDHLEnabled(boolean val) {
        prefs.edit().putBoolean(PREFS_DHL, val).apply();
    }

    public boolean getCamera() {
        return prefs.getBoolean(PREFS_KEY_CAMERA, true);
    }

    public void setCamera(boolean val) {
        prefs.edit().putBoolean(PREFS_KEY_CAMERA, val).apply();
    }
}
