package com.smartsenergy.microgrid.operator;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.gson.*;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.smartsenergy.microgrid.BaseActivity;
import com.smartsolar.microgrid.R;
import com.smartsenergy.microgrid.api.ApiClient;
import com.smartsenergy.microgrid.utils.ApiUtils;
import androidx.activity.result.ActivityResultLauncher;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OperatorDashboardActivity extends BaseActivity {

    private LinearLayout contentLayout;
    private TextView tvStatPending, tvStatApproved, tvStatToday;

    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) verifyQr(result.getContents());
                else toast("Scan cancelled");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (session.token().isEmpty() || !"GridOperator".equalsIgnoreCase(session.role())) {
            startActivity(new Intent(this, com.smartsolar.microgrid.ui.LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_operator_dashboard);

        contentLayout  = findViewById(R.id.contentLayout);
        tvStatPending  = findViewById(R.id.tvStatPending);
        tvStatApproved = findViewById(R.id.tvStatApproved);
        tvStatToday    = findViewById(R.id.tvStatToday);
        
        TextView tvUsername = findViewById(R.id.tvUsername);
        if (tvUsername != null) {
            tvUsername.setText(session.username() + " · " + session.nic());
        }

        android.view.View btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> doLogout());

        // Map real features to new UI grid and scroll cards
        findViewById(R.id.btnScanQr).setOnClickListener(v -> startScan());
        findViewById(R.id.btnPendingBookings).setOnClickListener(v -> showReservations("Pending"));
        findViewById(R.id.btnApprovedBookings).setOnClickListener(v -> showReservations("Approved"));
        findViewById(R.id.btnAvailableSlots).setOnClickListener(v -> showSlots(true));
        findViewById(R.id.btnUpdateSlot).setOnClickListener(v -> showSlots(false));

        setupBottomNav(R.id.nav_home);
        loadDashboard();
    }

    // Build actions logic replaced by direct XML mapping.


    private void loadDashboard() {
        showLoading();
        ApiClient.get().operatorDashboard().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (r.isSuccessful() && r.body() != null) {
                    JsonObject x = r.body();
                    tvStatPending.setText(String.valueOf(
                            x.has("pendingReservations") ? x.get("pendingReservations").getAsInt() : 0));
                    tvStatApproved.setText(String.valueOf(
                            x.has("approvedReservations") ? x.get("approvedReservations").getAsInt() : 0));
                    tvStatToday.setText(String.valueOf(
                            x.has("todayBookings") ? x.get("todayBookings").getAsInt() : 0));
                }
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); }
        });
    }

    // ── QR Scanner ────────────────────────────────────────
    private void startScan() {
        ScanOptions opts = new ScanOptions();
        opts.setPrompt("Scan Prosumer Transaction QR");
        opts.setBeepEnabled(true);
        opts.setOrientationLocked(false);
        opts.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanLauncher.launch(opts);
    }

    private void verifyQr(String qrToken) {
        JsonObject body = new JsonObject();
        body.addProperty("qrToken", qrToken);
        showLoading();
        ApiClient.get().verifyQr(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                JsonObject x   = r.body();
                JsonObject res = x.has("reservation") && x.get("reservation").isJsonObject()
                        ? x.getAsJsonObject("reservation") : new JsonObject();
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(OperatorDashboardActivity.this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                        .setTitle("✅ QR Verified")
                        .setMessage("📋 Transaction: " + ApiUtils.str(x, "transactionCode")
                                + "\n🔖 Reservation: " + ApiUtils.str(res, "reservationCode")
                                + "\n⚡ Energy: " + fmt(ApiUtils.num(res, "energyAmountKwh")) + " kWh"
                                + "\n🔖 Status: " + ApiUtils.str(res, "status"))
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Finalize Transfer", (d, w) -> completeQr(qrToken))
                        .show();
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void completeQr(String qrToken) {
        JsonObject body = new JsonObject();
        body.addProperty("qrToken", qrToken);
        showLoading();
        ApiClient.get().completeQr(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) { toast(errorMsg(r)); return; }
                toast("✅ Energy transfer completed successfully!");
                loadDashboard();
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    // ── Reservations ──────────────────────────────────────
    private void showReservations(String status) {
        Intent intent = new Intent(this, OperatorReservationsActivity.class);
        intent.putExtra("status", status);
        startActivity(intent);
    }

    private void reservationDetail(JsonObject x) {
        MaterialAlertDialogBuilder b = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle(ApiUtils.str(x, "reservationCode"))
                .setMessage("👤 Prosumer: " + ApiUtils.str(x, "prosumerId")
                        + "\n🗺 Node: " + ApiUtils.str(x, "nodeId")
                        + "\n📅 Date: " + shortDate(ApiUtils.str(x, "reservationDate"))
                        + "\n⏱ " + ApiUtils.str(x, "startTime")
                        + " – " + ApiUtils.str(x, "endTime")
                        + "\n⚡ Energy: " + fmt(ApiUtils.num(x, "energyAmountKwh")) + " kWh"
                        + "\n🔖 Status: " + ApiUtils.str(x, "status"));
        
        if ("Pending".equalsIgnoreCase(ApiUtils.str(x, "status"))) {
            b.setPositiveButton("Approve", (d, w) -> approveReservation(ApiUtils.str(x, "id")));
            b.setNegativeButton("Close", null);
        } else {
            b.setPositiveButton("Close", null);
        }
        b.show();
    }

    private void approveReservation(String id) {
        showLoading();
        ApiClient.get().approveReservation(id).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (r.isSuccessful()) {
                    toast("✅ Reservation Approved!");
                    showReservations("Pending");
                    loadDashboard();
                } else {
                    toast(errorMsg(r));
                }
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    // ── Energy Slots ──────────────────────────────────────
    private void showSlots(boolean availableOnly) {
        Intent intent = new Intent(this, OperatorSlotsActivity.class);
        intent.putExtra("availableOnly", availableOnly);
        startActivity(intent);
    }

    private void capacityDialog(JsonObject slot) {
        EditText et = new EditText(this);
        et.setHint("New capacity (kWh)");
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(fmt(ApiUtils.num(slot, "availableCapacityKwh")));
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
                .setTitle("Update Slot Availability")
                .setMessage("Slot ID: " + ApiUtils.str(slot, "id"))
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    try { updateSlot(ApiUtils.str(slot, "id"),
                            Double.parseDouble(et.getText().toString()));
                    } catch (Exception e) { toast("Enter a valid number"); }
                }).show();
    }

    private void updateSlot(String id, double capacity) {
        showLoading();
        ApiClient.get().updateAvailability(id, capacity).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (r.isSuccessful()) toast("✅ Slot availability updated");
                else toast(errorMsg(r));
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    // ── Helpers ────────────────────────────────────────────
    private String shortDate(String s) {
        return (s != null && s.length() >= 10) ? s.substring(0, 10) : s;
    }

    private String fmt(double d) {
        return d == (long) d ? String.valueOf((long) d) : String.valueOf(d);
    }

    private String statusIcon(String status) {
        if ("Approved".equalsIgnoreCase(status))  return "✅";
        if ("Pending".equalsIgnoreCase(status))   return "⏳";
        if ("Completed".equalsIgnoreCase(status)) return "🏁";
        if ("Cancelled".equalsIgnoreCase(status)) return "❌";
        return "•";
    }
}
