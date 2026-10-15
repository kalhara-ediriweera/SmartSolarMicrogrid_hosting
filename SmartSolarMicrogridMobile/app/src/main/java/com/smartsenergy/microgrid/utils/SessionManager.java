package com.smartsenergy.microgrid.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.smartsenergy.microgrid.api.ApiConfig;

public final class SessionManager {
    public static final String BASE_URL = ApiConfig.BASE_URL;
    private static SessionManager instance;
    private final SharedPreferences sp;

    private SessionManager(Context c) {
        sp = c.getSharedPreferences("session", Context.MODE_PRIVATE);
    }

    public static void init(Context c) {
        if (instance == null) instance = new SessionManager(c.getApplicationContext());
    }

    public static SessionManager getInstance() {
        if (instance == null) throw new IllegalStateException("SessionManager not initialized");
        return instance;
    }

    public String getBaseUrl() {
        // Drop a URL saved against an older default so changing ApiConfig.BASE_URL takes effect
        if (!BASE_URL.equals(sp.getString("base_url_default", null))) {
            sp.edit().remove("base_url").putString("base_url_default", BASE_URL).apply();
        }
        String url = sp.getString("base_url", BASE_URL);
        if (url == null || url.trim().isEmpty()) {
            url = BASE_URL;
        }
        url = url.trim();
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    public void setBaseUrl(String url) {
        if (url != null && !url.trim().isEmpty()) {
            String clean = url.trim();
            if (!clean.endsWith("/")) clean += "/";
            sp.edit().putString("base_url", clean).putString("base_url_default", BASE_URL).apply();
        }
    }

    public void save(String token, String role, String username, String nic) {
        sp.edit().putString("token", token)
                .putString("role", role)
                .putString("username", username)
                .putString("nic", nic)
                .apply();
    }

    public String token() { return sp.getString("token", ""); }
    public String role() { return sp.getString("role", ""); }
    public String username() { return sp.getString("username", ""); }
    public String nic() { return sp.getString("nic", ""); }

    public void clear() {
        String savedUrl = getBaseUrl();
        int savedTheme = getThemeMode();
        sp.edit().clear()
                .putString("base_url", savedUrl)
                .putString("base_url_default", BASE_URL)
                .putInt("theme_mode", savedTheme)
                .apply();
    }

    public int getThemeMode() {
        return sp.getInt("theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
    }

    public void setThemeMode(int mode) {
        sp.edit().putInt("theme_mode", mode).apply();
    }
}
