package com.smartsenergy.microgrid.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Response;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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

    /**
     * Formats an energy amount / capacity (kWh / kW):
     * - If whole number: integer format without decimals (e.g. 50 -> "50")
     * - If decimal: maximum of 3 decimal places, removing unnecessary trailing zeros (e.g. 22.5 -> "22.5", 3.125 -> "3.125")
     */
    public static String fmt(double d) {
        if (d == (long) d) {
            return String.format(Locale.US, "%d", (long) d);
        }
        DecimalFormat df = new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US));
        return df.format(d);
    }

    /**
     * Checks if a slot is eligible for booking or modification:
     * 1. Cannot be in the past (before current date & time)
     * 2. Must be within the upcoming 7 days from now
     * 3. Must have available capacity > 0 and Status is Available
     */
    public static boolean isSlotBookable(JsonObject slot) {
        if (slot == null) return false;
        if (num(slot, "availableCapacityKwh") <= 0) return false;

        String status = str(slot, "status");
        if (!status.isEmpty() && !"Available".equalsIgnoreCase(status)) return false;

        String dateStr = str(slot, "slotDate").trim();
        String timeStr = str(slot, "startTime").trim();
        if (dateStr.isEmpty() || timeStr.isEmpty()) return false;

        try {
            if (dateStr.length() >= 10) {
                dateStr = dateStr.substring(0, 10);
            }
            if (timeStr.length() == 4 && timeStr.charAt(1) == ':') {
                timeStr = "0" + timeStr;
            } else if (timeStr.length() > 5) {
                timeStr = timeStr.substring(0, 5);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            Date slotStart = sdf.parse(dateStr + " " + timeStr);
            if (slotStart == null) return false;

            long now = System.currentTimeMillis();
            // 1. Cannot show/book past slots (prior to current date & time)
            if (slotStart.getTime() <= now) {
                return false;
            }

            // 2. Only slots within upcoming 7 days from today can be booked
            Calendar maxCal = Calendar.getInstance();
            maxCal.setTimeInMillis(now);
            maxCal.add(Calendar.DAY_OF_YEAR, 7);
            maxCal.set(Calendar.HOUR_OF_DAY, 23);
            maxCal.set(Calendar.MINUTE, 59);
            maxCal.set(Calendar.SECOND, 59);
            maxCal.set(Calendar.MILLISECOND, 999);

            return slotStart.getTime() <= maxCal.getTimeInMillis();
        } catch (Exception e) {
            return false;
        }
    }

    public static com.google.gson.JsonArray filterBookableSlots(com.google.gson.JsonArray slots) {
        com.google.gson.JsonArray filtered = new com.google.gson.JsonArray();
        if (slots == null) return filtered;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isJsonObject()) {
                JsonObject s = slots.get(i).getAsJsonObject();
                if (isSlotBookable(s)) {
                    filtered.add(s);
                }
            }
        }
        return filtered;
    }
}
