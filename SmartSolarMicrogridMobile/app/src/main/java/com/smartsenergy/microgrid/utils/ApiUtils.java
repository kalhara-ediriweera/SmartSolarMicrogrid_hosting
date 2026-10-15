package com.smartsenergy.microgrid.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Response;

public final class ApiUtils {
    private ApiUtils() {}

    public static String error(Response<?> r) {
        if (r == null) return "Network error";
        if (r.errorBody() != null) {
            try { return r.errorBody().string(); } catch (Exception ignored) {}
        }
        return "Request failed (" + r.code() + ")";
    }

    public static String str(JsonObject o, String key) {
        if (o == null) return "";
        String capKey = key.substring(0, 1).toUpperCase() + key.substring(1);
        String actualKey = o.has(key) ? key : (o.has(capKey) ? capKey : null);
        if (actualKey == null || o.get(actualKey).isJsonNull()) return "";
        try {
            return o.get(actualKey).getAsString();
        } catch (Exception e) {
            return o.get(actualKey).toString();
        }
    }

    public static double num(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return 0.0;
        try {
            return o.get(key).getAsDouble();
        } catch (Exception e) {
            try {
                return Double.parseDouble(o.get(key).getAsString());
            } catch (Exception ignored) {
                return 0.0;
            }
        }
    }

    public static int integer(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return 0;
        try {
            return o.get(key).getAsInt();
        } catch (Exception e) {
            try {
                return Integer.parseInt(o.get(key).getAsString());
            } catch (Exception ignored) {
                return 0;
            }
        }
    }
}
