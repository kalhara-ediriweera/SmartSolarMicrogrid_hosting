package com.smartsenergy.microgrid.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.smartsenergy.microgrid.utils.ApiUtils;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB = "smart_solar.db";
    private static final int VER = 2;

    private static final String CREATE_RESERVATIONS =
            "CREATE TABLE IF NOT EXISTS local_reservations ("
                    + "id TEXT PRIMARY KEY, "
                    + "reservation_code TEXT, "
                    + "node_id TEXT, "
                    + "node_name TEXT, "
                    + "energy_slot_id TEXT, "
                    + "reservation_date TEXT, "
                    + "start_time TEXT, "
                    + "end_time TEXT, "
                    + "energy_amount_kwh REAL, "
                    + "status TEXT, "
                    + "last_sync TEXT"
                    + ")";

    public DatabaseHelper(Context c) {
        super(c, DB, null, VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE local_user ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nic TEXT UNIQUE, "
                + "full_name TEXT, "
                + "email TEXT, "
                + "phone TEXT, "
                + "address TEXT, "
                + "username TEXT, "
                + "role TEXT, "
                + "last_sync TEXT"
                + ")");

        db.execSQL("CREATE TABLE reference_nodes ("
                + "id TEXT PRIMARY KEY, "
                + "node_code TEXT, "
                + "node_name TEXT, "
                + "latitude REAL, "
                + "longitude REAL, "
                + "capacity_kw REAL, "
                + "battery_slots INTEGER, "
                + "status TEXT"
                + ")");

        db.execSQL(CREATE_RESERVATIONS);
    }

    // v1 -> v2 only adds the reservations table, so the saved user is kept.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL(CREATE_RESERVATIONS);
            return;
        }
        db.execSQL("DROP TABLE IF EXISTS local_user");
        db.execSQL("DROP TABLE IF EXISTS reference_nodes");
        db.execSQL("DROP TABLE IF EXISTS local_reservations");
        onCreate(db);
    }

    public void saveUser(String nic, String name, String email, String phone, String address, String username, String role) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("nic", nic);
        v.put("full_name", name);
        v.put("email", email);
        v.put("phone", phone);
        v.put("address", address);
        v.put("username", username);
        v.put("role", role);
        v.put("last_sync", String.valueOf(System.currentTimeMillis()));

        db.insertWithOnConflict("local_user", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor getUser() {
        return getReadableDatabase().rawQuery("SELECT * FROM local_user LIMIT 1", null);
    }

    // Caches reservations returned by the API. fullList=true means the API returned all of the prosumer's
    // reservations (no search/status filter), so rows that no longer exist on the server are removed.
    public void saveReservations(JsonArray list, boolean fullList) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (fullList) {
                db.delete("local_reservations", null, null);
            }
            String now = String.valueOf(System.currentTimeMillis());
            for (int i = 0; i < list.size(); i++) {
                JsonObject r = list.get(i).getAsJsonObject();
                ContentValues v = new ContentValues();
                v.put("id", ApiUtils.str(r, "id"));
                v.put("reservation_code", ApiUtils.str(r, "reservationCode"));
                v.put("node_id", ApiUtils.str(r, "nodeId"));
                v.put("node_name", ApiUtils.str(r, "nodeName"));
                v.put("energy_slot_id", ApiUtils.str(r, "energySlotId"));
                v.put("reservation_date", ApiUtils.str(r, "reservationDate"));
                v.put("start_time", ApiUtils.str(r, "startTime"));
                v.put("end_time", ApiUtils.str(r, "endTime"));
                v.put("energy_amount_kwh", ApiUtils.num(r, "energyAmountKwh"));
                v.put("status", ApiUtils.str(r, "status"));
                v.put("last_sync", now);

                db.insertWithOnConflict("local_reservations", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Reads cached reservations in the same JSON shape the API returns, so screens can show them offline.
    public JsonArray getReservations(String search, String status) {
        StringBuilder where = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            where.append(" AND (reservation_code LIKE ? OR node_name LIKE ?)");
            args.add("%" + search.trim() + "%");
            args.add("%" + search.trim() + "%");
        }
        if (status != null && !status.isEmpty()) {
            where.append(" AND status=?");
            args.add(status);
        }

        JsonArray out = new JsonArray();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM local_reservations WHERE " + where + " ORDER BY reservation_date DESC",
                args.toArray(new String[0]))) {

            while (c.moveToNext()) {
                JsonObject r = new JsonObject();
                r.addProperty("id", c.getString(c.getColumnIndexOrThrow("id")));
                r.addProperty("reservationCode", c.getString(c.getColumnIndexOrThrow("reservation_code")));
                r.addProperty("nodeId", c.getString(c.getColumnIndexOrThrow("node_id")));
                r.addProperty("nodeName", c.getString(c.getColumnIndexOrThrow("node_name")));
                r.addProperty("energySlotId", c.getString(c.getColumnIndexOrThrow("energy_slot_id")));
                r.addProperty("reservationDate", c.getString(c.getColumnIndexOrThrow("reservation_date")));
                r.addProperty("startTime", c.getString(c.getColumnIndexOrThrow("start_time")));
                r.addProperty("endTime", c.getString(c.getColumnIndexOrThrow("end_time")));
                r.addProperty("energyAmountKwh", c.getDouble(c.getColumnIndexOrThrow("energy_amount_kwh")));
                r.addProperty("status", c.getString(c.getColumnIndexOrThrow("status")));
                out.add(r);
            }
        }
        return out;
    }

    public void updateReservationStatus(String id, String status) {
        ContentValues v = new ContentValues();
        v.put("status", status);
        getWritableDatabase().update("local_reservations", v, "id=?", new String[]{id});
    }
}
