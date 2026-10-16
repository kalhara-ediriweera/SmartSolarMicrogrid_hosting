package com.smartsenergy.microgrid.prosumer;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.gson.*;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import android.graphics.*;
import com.smartsenergy.microgrid.BaseActivity;
import com.smartsolar.microgrid.R;
import com.smartsenergy.microgrid.api.ApiClient;
import com.smartsenergy.microgrid.database.DatabaseHelper;
import com.smartsenergy.microgrid.maps.NearbyStationsActivity;
import com.smartsenergy.microgrid.utils.ApiUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProsumerDashboardActivity extends BaseActivity {

    private LinearLayout contentLayout;
    private TextView tvStatActive, tvStatPending, tvStatCompleted, tvUsername;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (session.token().isEmpty() || !"Prosumer".equalsIgnoreCase(session.role())) {
            startActivity(new Intent(this, com.smartsolar.microgrid.ui.LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_prosumer_dashboard);

        contentLayout    = findViewById(R.id.contentLayout);
        tvStatActive     = findViewById(R.id.tvStatActive);
        tvStatPending    = findViewById(R.id.tvStatPending);
        tvStatCompleted  = findViewById(R.id.tvStatCompleted);
        tvUsername       = findViewById(R.id.tvUsername);
        db               = new DatabaseHelper(this);

        tvUsername.setText(session.username() + " · " + session.nic());

        android.view.View btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> confirmLogout());

        // Map real features to new UI grid and scroll cards
        findViewById(R.id.btnBookSlot).setOnClickListener(v -> startActivity(new Intent(this, BookSlotActivity.class)));
        findViewById(R.id.btnMyBookings).setOnClickListener(v -> startActivity(new Intent(this, BookingsActivity.class)));
        findViewById(R.id.btnGridMap).setOnClickListener(v -> startActivity(new Intent(this, com.smartsolar.microgrid.ui.MapActivity.class)));
        findViewById(R.id.btnMyProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btnQrCode).setOnClickListener(v -> openQrFlow());
        // btnDeactivate removed from dashboard layout

        setupBottomNav(R.id.nav_home);
        loadDashboard();
    }

    // Build actions logic replaced by direct XML mapping.


    private void loadDashboard() {
        showLoading();
        ApiClient.get().prosumerDashboard(session.nic()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                // Saved token has expired: send the user back to login instead of showing an empty dashboard
                if (r.code() == 401) { toast("Session expired. Please log in again."); doLogout(); return; }
                if (r.isSuccessful() && r.body() != null) {
                    JsonObject x = r.body();
                    tvStatActive.setText(String.valueOf(
                            x.has("activeReservations") ? x.get("activeReservations").getAsInt() : 0));
                    tvStatPending.setText(String.valueOf(
                            x.has("pendingReservations") ? x.get("pendingReservations").getAsInt() : 0));
                    tvStatCompleted.setText(String.valueOf(
                            x.has("historyCount") ? x.get("historyCount").getAsInt() : 0));
                }
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); }
        });
    }

    // ── Book a slot ────────────────────────────────────────
    private void book() {
        showLoading();
        ApiClient.get().slots("", true).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                hideLoading();
                if (!r.isSuccessful() || r.body() == null) { toast(errorMsg(r)); return; }
                JsonArray raw = r.body().getAsJsonArray();
                JsonArray a = ApiUtils.filterBookableSlots(raw);
                if (a.size() == 0) { toast("No upcoming slots found in the next 7 days."); return; }

                String[] labels = new String[a.size()];
                for (int i = 0; i < a.size(); i++) {
                    JsonObject s = a.get(i).getAsJsonObject();
                    labels[i] = "📅 " + shortDate(ApiUtils.str(s, "slotDate"))
                            + "  ⏱ " + ApiUtils.str(s, "startTime") + "–" + ApiUtils.str(s, "endTime")
                            + "  ⚡ " + fmt(ApiUtils.num(s, "availableCapacityKwh")) + " kWh";
                }
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(ProsumerDashboardActivity.this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                        .setTitle("Available Energy Slots")
                        .setItems(labels, (d, w) -> energyDialog(a.get(w).getAsJsonObject()))
                        .show();
            }
            @Override
            public void onFailure(Call<JsonElement> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void energyDialog(JsonObject slot) {
        EditText et = new EditText(this);
        et.setHint("Energy amount (kWh)");
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setTextSize(14f);
        et.setPadding(40, 36, 40, 36);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(android.graphics.Color.parseColor("#F5F5F5"));
        bg.setCornerRadius(24f);
        bg.setStroke(2, android.graphics.Color.parseColor("#E0E0E0"));
        et.setBackground(bg);
        
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(64, 24, 64, 0);
        box.addView(et);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle("Reserve Slot")
                .setMessage("📅 " + shortDate(ApiUtils.str(slot, "slotDate"))
                        + "\n⏱ " + ApiUtils.str(slot, "startTime")
                        + " – " + ApiUtils.str(slot, "endTime")
                        + "\n⚡ Available: " + fmt(ApiUtils.num(slot, "availableCapacityKwh")) + " kWh\n")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm", (d, w) ->
                        createReservation(ApiUtils.str(slot, "id"), et.getText().toString()))
                .show();
    }

    private void createReservation(String slotId, String amount) {
        try {
            double k = Double.parseDouble(amount);
            if (k <= 0) { toast("Enter a valid energy amount"); return; }
            JsonObject body = new JsonObject();
            body.addProperty("energySlotId", slotId);
            body.addProperty("energyAmountKwh", k);
            showLoading();
            ApiClient.get().createReservation(session.nic(), body).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                    hideLoading();
                    if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                    showSummary("✅ Booking Created", r.body());
                    loadDashboard();
                }
                @Override
                public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
            });
        } catch (Exception e) { toast("Enter a valid number"); }
    }

    // ── Filter / search bookings ───────────────────────────
    private void filterBookings() {
        android.view.View view = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_search_bookings, null);

        com.google.android.material.textfield.TextInputEditText etSearch = view.findViewById(R.id.etSearch);
        android.widget.Spinner spStatus = view.findViewById(R.id.spStatus);

        String[] opts = {"All", "Pending", "Approved", "Cancelled", "Completed"};
        spStatus.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, opts));

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(view)
                .setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .show();

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String st = spStatus.getSelectedItem().toString();
            loadBookings(etSearch.getText().toString().trim(),
                    "All".equals(st) ? "" : st);
            dialog.dismiss();
        });
    }

    private void loadBookings(String searchText, String statusFilter) {
        showLoading();
        ApiClient.get().myReservations(session.nic(), searchText, statusFilter)
                .enqueue(new Callback<JsonElement>() {
                    @Override
                    public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                        hideLoading();
                        if (!r.isSuccessful() || r.body() == null) { toast(errorMsg(r)); return; }
                        JsonArray a = r.body().getAsJsonArray();
                        if (a.size() == 0) { toast("No bookings found"); return; }

                        String[] labels = new String[a.size()];
                        for (int i = 0; i < a.size(); i++) {
                            JsonObject x = a.get(i).getAsJsonObject();
                            labels[i] = statusIcon(ApiUtils.str(x, "status"))
                                    + " " + ApiUtils.str(x, "reservationCode")
                                    + "  ·  " + shortDate(ApiUtils.str(x, "reservationDate"));
                        }
                        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(ProsumerDashboardActivity.this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                                .setTitle("My Bookings")
                                .setItems(labels, (d, w) ->
                                        bookingActions(a.get(w).getAsJsonObject()))
                                .create();
                        dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog);
                        dialog.show();
                    }
                    @Override
                    public void onFailure(Call<JsonElement> c, Throwable t) { hideLoading(); fail(t); }
                });
    }

    private void bookingActions(JsonObject r) {
        String status = ApiUtils.str(r, "status");
        android.view.View view = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_booking_actions, null);

        TextView tvResCode = view.findViewById(R.id.tvResCode);
        tvResCode.setText(ApiUtils.str(r, "reservationCode"));

        TextView tvStatus = view.findViewById(R.id.tvStatus);
        tvStatus.setText(statusIcon(status) + " " + status);
        if ("Pending".equals(status)) {
            tvStatus.setTextColor(android.graphics.Color.parseColor("#FFA000"));
        } else if ("Approved".equals(status)) {
            tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
        } else if ("Cancelled".equals(status)) {
            tvStatus.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
        }

        TextView tvDate = view.findViewById(R.id.tvDate);
        tvDate.setText("📅 " + shortDate(ApiUtils.str(r, "reservationDate")));

        TextView tvTime = view.findViewById(R.id.tvTime);
        tvTime.setText("⏱ " + ApiUtils.str(r, "startTime") + " – " + ApiUtils.str(r, "endTime"));

        TextView tvEnergy = view.findViewById(R.id.tvEnergy);
        tvEnergy.setText("⚡ " + fmt(ApiUtils.num(r, "energyAmountKwh")) + " kWh");

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(view)
                .setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .show();

        com.google.android.material.button.MaterialButton btnGetQr = view.findViewById(R.id.btnGetQr);
        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnModify = view.findViewById(R.id.btnModify);
        com.google.android.material.button.MaterialButton btnClose = view.findViewById(R.id.btnClose);

        if ("Approved".equalsIgnoreCase(status)) {
            btnGetQr.setVisibility(android.view.View.VISIBLE);
            btnGetQr.setOnClickListener(v -> { generateQr(ApiUtils.str(r, "id")); dialog.dismiss(); });
        }
        
        if ("Pending".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(status)) {
            btnModify.setVisibility(android.view.View.VISIBLE);
            btnModify.setOnClickListener(v -> { modifyDialog(r); dialog.dismiss(); });
            
            btnCancel.setVisibility(android.view.View.VISIBLE);
            btnCancel.setOnClickListener(v -> { confirmCancel(ApiUtils.str(r, "id")); dialog.dismiss(); });
        } else {
            btnClose.setVisibility(android.view.View.VISIBLE);
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
    }

    private void modifyDialog(JsonObject old) {
        android.view.View view = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_modify_reservation, null);
        com.google.android.material.textfield.TextInputEditText etSlotId = view.findViewById(R.id.etSlotId);
        com.google.android.material.textfield.TextInputEditText etEnergyAmount = view.findViewById(R.id.etEnergyAmount);

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(view)
                .setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .show();

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            try {
                JsonObject j = new JsonObject();
                j.addProperty("energySlotId", etSlotId.getText().toString().trim());
                j.addProperty("energyAmountKwh", Double.parseDouble(etEnergyAmount.getText().toString()));
                updateReservation(ApiUtils.str(old, "id"), j);
                dialog.dismiss();
            } catch (Exception e) { toast("Enter valid values"); }
        });
    }

    private void updateReservation(String id, JsonObject body) {
        showLoading();
        ApiClient.get().updateReservation(id, session.nic(), body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                showSummary("✅ Booking Updated", r.body());
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void confirmCancel(String id) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle("Cancel Booking?")
                .setMessage("Cancellation requires at least 12 hours notice.")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes, Cancel", (d, w) -> cancelReservation(id))
                .show();
    }

    private void cancelReservation(String id) {
        showLoading();
        ApiClient.get().cancelReservation(id, session.nic()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                showSummary("Booking Cancelled", r.body());
                loadDashboard();
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    // ── QR ─────────────────────────────────────────────────
    private void openQrFlow() {
        showLoading();
        ApiClient.get().myReservations(session.nic(), "", "Approved").enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                hideLoading();
                if (!r.isSuccessful() || r.body() == null) {
                    toast(errorMsg(r));
                    return;
                }
                JsonArray arr = r.body().getAsJsonArray();
                if (arr.size() == 0) {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(ProsumerDashboardActivity.this,
                            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                            .setTitle("No Approved Bookings")
                            .setMessage("You don't have any approved reservations yet. QR codes are generated after your reservation is approved by the backoffice.")
                            .setPositiveButton("View All Bookings", (d, w) -> startActivity(new Intent(ProsumerDashboardActivity.this, BookingsActivity.class)))
                            .setNegativeButton("Close", null)
                            .show();
                    return;
                }

                if (arr.size() == 1) {
                    JsonObject res = arr.get(0).getAsJsonObject();
                    generateQr(ApiUtils.str(res, "id"));
                } else {
                    String[] items = new String[arr.size()];
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject obj = arr.get(i).getAsJsonObject();
                        items[i] = "🔖 " + ApiUtils.str(obj, "reservationCode") + " · "
                                + shortDate(ApiUtils.str(obj, "reservationDate")) + " ("
                                + fmt(ApiUtils.num(obj, "energyAmountKwh")) + " kWh)";
                    }
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(ProsumerDashboardActivity.this,
                            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                            .setTitle("Select Approved Booking")
                            .setItems(items, (d, w) -> {
                                JsonObject chosen = arr.get(w).getAsJsonObject();
                                generateQr(ApiUtils.str(chosen, "id"));
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<JsonElement> c, Throwable t) {
                hideLoading();
                fail(t);
            }
        });
    }

    private void generateQr(String reservationId) {
        showLoading();
        ApiClient.get().generateQr(reservationId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful() || r.body() == null) { toast(errorMsg(r)); return; }
                showQr(r.body());
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void showQr(JsonObject q) {
        String token = ApiUtils.str(q, "qrToken");
        if (token.isEmpty()) token = ApiUtils.str(q, "token");
        if (token.isEmpty()) token = ApiUtils.str(q, "qrData");

        String txCode = ApiUtils.str(q, "transactionCode");

        ImageView img = new ImageView(this);
        img.setPadding(32, 32, 32, 32);
        try {
            BitMatrix m = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 650, 650);
            Bitmap bmp  = Bitmap.createBitmap(650, 650, Bitmap.Config.RGB_565);
            for (int x = 0; x < 650; x++)
                for (int y = 0; y < 650; y++)
                    bmp.setPixel(x, y, m.get(x, y) ? Color.BLACK : Color.WHITE);
            img.setImageBitmap(bmp);
        } catch (Exception e) { toast("QR generation error: " + e.getMessage()); return; }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle("Transaction QR Code")
                .setMessage("📋 Code: " + txCode
                        + "\n\nShow this QR to the Grid Operator at the charging station to complete energy transfer.")
                .setView(img)
                .setPositiveButton("Done", null)
                .show();
    }

    // ── Profile ────────────────────────────────────────────
    private void profile() {
        showLoading();
        ApiClient.get().getProsumer(session.nic()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                JsonObject x = r.body();
                db.saveUser(session.nic(), ApiUtils.str(x, "fullName"),
                        ApiUtils.str(x, "email"), ApiUtils.str(x, "phone"),
                        ApiUtils.str(x, "address"), session.username(), session.role());

                EditText etName = makeField(ApiUtils.str(x, "fullName"));
                EditText etEmail = makeField(ApiUtils.str(x, "email"));
                EditText etPhone = makeField(ApiUtils.str(x, "phone"));
                EditText etAddr  = makeField(ApiUtils.str(x, "address"));

                LinearLayout box = new LinearLayout(ProsumerDashboardActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setPadding(32, 8, 32, 0);
                addLabeled(box, "Full Name", etName);
                addLabeled(box, "Email", etEmail);
                addLabeled(box, "Phone", etPhone);
                addLabeled(box, "Address", etAddr);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(ProsumerDashboardActivity.this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                        .setTitle("My Profile  ·  NIC: " + session.nic())
                        .setView(box)
                        .setNegativeButton("Close", null)
                        .setPositiveButton("Save Changes", (d, w) -> {
                            JsonObject j = new JsonObject();
                            j.addProperty("fullName", etName.getText().toString());
                            j.addProperty("email",    etEmail.getText().toString());
                            j.addProperty("phone",    etPhone.getText().toString());
                            j.addProperty("address",  etAddr.getText().toString());
                            updateProfile(j);
                        }).show();
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void updateProfile(JsonObject body) {
        showLoading();
        ApiClient.get().updateProsumer(session.nic(), body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                toast("✅ Profile updated successfully");
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    // ── Deactivation ───────────────────────────────────────
    private void deactivate() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle("Request Account Deactivation")
                .setMessage("This will send a deactivation request to Backoffice for review.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send Request", (d, w) -> {
                    showLoading();
                    ApiClient.get().requestDeactivation(session.nic())
                            .enqueue(new Callback<JsonObject>() {
                                @Override
                                public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                                    hideLoading();
                                    if (r.isSuccessful()) toast("Deactivation request submitted.");
                                    else toast(errorMsg(r));
                                }
                                @Override
                                public void onFailure(Call<JsonObject> c, Throwable t) {
                                    hideLoading(); fail(t);
                                }
                            });
                }).show();
    }

    // ── UI utility helpers ─────────────────────────────────
    private void showSummary(String title, JsonObject r) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle(title)
                .setMessage("📋 " + ApiUtils.str(r, "reservationCode")
                        + "\n🔖 Status: " + ApiUtils.str(r, "status")
                        + "\n📅 Date: " + shortDate(ApiUtils.str(r, "reservationDate"))
                        + "\n⚡ Energy: " + fmt(ApiUtils.num(r, "energyAmountKwh")) + " kWh")
                .setPositiveButton("OK", null)
                .show();
    }

    private EditText makeField(String value) {
        EditText et = new EditText(this);
        et.setText(value);
        et.setTextSize(14f);
        et.setPadding(40, 36, 40, 36);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(android.graphics.Color.parseColor("#F5F5F5"));
        bg.setCornerRadius(24f);
        bg.setStroke(2, android.graphics.Color.parseColor("#E0E0E0"));
        et.setBackground(bg);
        return et;
    }

    private void addLabeled(LinearLayout parent, String label, EditText et) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFF1B6B4A);
        tv.setTextSize(12f);
        tv.setPadding(0, 12, 0, 2);
        parent.addView(tv);
        parent.addView(et);
    }

    private String shortDate(String s) {
        return (s != null && s.length() >= 10) ? s.substring(0, 10) : s;
    }

    private String fmt(double d) {
        return ApiUtils.fmt(d);
    }

    private String statusIcon(String status) {
        if ("Approved".equalsIgnoreCase(status))   return "✅";
        if ("Pending".equalsIgnoreCase(status))    return "⏳";
        if ("Completed".equalsIgnoreCase(status))  return "🏁";
        if ("Cancelled".equalsIgnoreCase(status))  return "❌";
        return "•";
    }
}
