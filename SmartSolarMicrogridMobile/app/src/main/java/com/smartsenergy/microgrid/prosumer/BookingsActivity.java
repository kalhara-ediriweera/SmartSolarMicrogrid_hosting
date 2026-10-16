package com.smartsenergy.microgrid.prosumer;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.smartsenergy.microgrid.BaseActivity;
import com.smartsolar.microgrid.R;
import com.smartsenergy.microgrid.api.ApiClient;
import com.smartsenergy.microgrid.database.DatabaseHelper;
import com.smartsenergy.microgrid.utils.ApiUtils;
import android.graphics.Bitmap;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingsActivity extends BaseActivity {

    private LinearLayout bookingsContainer;
    private EditText etSearch;
    private Spinner spStatus;
    private DatabaseHelper db;
    private boolean offlineNoticeShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);

        db = new DatabaseHelper(this);
        bookingsContainer = findViewById(R.id.bookingsContainer);
        etSearch = findViewById(R.id.etSearch);
        spStatus = findViewById(R.id.spStatus);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_dropdown_item, 
            new String[]{"All", "Pending", "Approved", "Completed", "Cancelled"});
        spStatus.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { loadBookings(); }
        });

        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { loadBookings(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        
        setupBottomNav(R.id.nav_bookings);
        loadBookings();
    }

    private void loadBookings() {
        String query = etSearch.getText().toString();
        String status = spStatus.getSelectedItem().toString();
        if ("All".equals(status)) status = "";

        final String q = query, st = status;
        // showLoading(); // Silent load since it reloads on keystroke
        ApiClient.get().myReservations(session.nic(), query, status).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                // hideLoading();
                if (!r.isSuccessful() || r.body() == null) { return; }
                JsonArray a = r.body().getAsJsonArray();
                // Keep a local SQLite copy so bookings can still be viewed offline
                db.saveReservations(a, q.trim().isEmpty() && st.isEmpty());
                showBookings(a);
            }
            @Override
            public void onFailure(Call<JsonElement> c, Throwable t) {
                // No connection: show the bookings saved in SQLite
                if (!offlineNoticeShown) { toast("Offline - showing saved bookings"); offlineNoticeShown = true; }
                showBookings(db.getReservations(q, st));
            }
        });
    }

    private void showBookings(JsonArray a) {
        bookingsContainer.removeAllViews();

        if (a.size() == 0) {
            TextView tv = new TextView(BookingsActivity.this);
            tv.setText("No bookings found.");
            tv.setTextColor(Color.GRAY);
            bookingsContainer.addView(tv);
            return;
        }

        for (int i = 0; i < a.size(); i++) {
            addBookingCard(a.get(i).getAsJsonObject());
        }
    }

    private void addBookingCard(JsonObject r) {
        String status = ApiUtils.str(r, "status");

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);
        card.setCardElevation(3f);
        card.setRadius(16f);
        card.setUseCompatPadding(true);
        card.setOnClickListener(v -> showBookingDetails(r));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(32, 32, 32, 32);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(ApiUtils.str(r, "reservationCode") + "  ·  " + status);
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor("Pending".equals(status) ? Color.parseColor("#FFA000") : 
                             "Approved".equals(status) ? Color.parseColor("#4CAF50") : Color.GRAY);
        box.addView(tvTitle);

        TextView tvAmount = new TextView(this);
        tvAmount.setText(fmt(ApiUtils.num(r, "energyAmountKwh")) + " kWh");
        tvAmount.setTextSize(14f);
        tvAmount.setTextColor(Color.DKGRAY);
        tvAmount.setPadding(0, 8, 0, 0);
        box.addView(tvAmount);

        card.addView(box);
        bookingsContainer.addView(card);
    }
    
    private void showBookingDetails(JsonObject r) {
        String status = ApiUtils.str(r, "status");
                
        View view = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_booking_details, null);
        
        TextView tvCode = view.findViewById(R.id.tvCode);
        tvCode.setText(ApiUtils.str(r, "reservationCode"));
        
        TextView tvNode = view.findViewById(R.id.tvNode);
        tvNode.setText(ApiUtils.str(r, "nodeName"));
        
        TextView tvStatus = view.findViewById(R.id.tvStatus);
        tvStatus.setText(status);
        if ("Pending".equals(status)) {
            tvStatus.setTextColor(Color.parseColor("#FFA000"));
        } else if ("Approved".equals(status)) {
            tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else if ("Cancelled".equals(status)) {
            tvStatus.setTextColor(Color.parseColor("#D32F2F"));
        }
        
        TextView tvAmount = view.findViewById(R.id.tvAmount);
        tvAmount.setText(fmt(ApiUtils.num(r, "energyAmountKwh")) + " kWh");
        
        TextView tvDateTime = view.findViewById(R.id.tvDateTime);
        String date = ApiUtils.str(r, "reservationDate");
        tvDateTime.setText((date.length() >= 10 ? date.substring(0, 10) : date) + "  "
                + ApiUtils.str(r, "startTime") + " - " + ApiUtils.str(r, "endTime"));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setBackground(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                .show();

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        com.google.android.material.button.MaterialButton btnAction = view.findViewById(R.id.btnAction);

        if ("Approved".equals(status)) {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setText("View QR");
            btnAction.setOnClickListener(v -> {
                generateQr(ApiUtils.str(r, "id"));
                dialog.dismiss();
            });
        }

        // Pending and Approved bookings can be modified or cancelled if at least 12 hours notice remains
        if ("Pending".equals(status) || "Approved".equals(status)) {
            view.findViewById(R.id.manageRow).setVisibility(View.VISIBLE);
            boolean has12h = ApiUtils.has12HoursNotice(r);
            com.google.android.material.button.MaterialButton btnModify = view.findViewById(R.id.btnModify);
            com.google.android.material.button.MaterialButton btnCancelBooking = view.findViewById(R.id.btnCancelBooking);
            TextView tvNotice = view.findViewById(R.id.tvNotice);

            if (!has12h) {
                if (tvNotice != null) {
                    tvNotice.setText("🔒 Cannot modify or cancel within 12 hours of scheduled slot.");
                    tvNotice.setTextColor(Color.parseColor("#D32F2F"));
                    tvNotice.setVisibility(View.VISIBLE);
                }
                btnModify.setAlpha(0.45f);
                btnCancelBooking.setAlpha(0.45f);

                View.OnClickListener lockedListener = v -> {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Action Not Allowed")
                            .setMessage("Modifications and cancellations require at least 12 hours' notice before the scheduled slot time.\n\nThis reservation starts in less than 12 hours and can no longer be edited or cancelled.")
                            .setPositiveButton("OK", null)
                            .show();
                };
                btnModify.setOnClickListener(lockedListener);
                btnCancelBooking.setOnClickListener(lockedListener);
            } else {
                if (tvNotice != null) {
                    tvNotice.setText("ℹ️ Modifications & cancellations require at least 12h notice.");
                    tvNotice.setTextColor(Color.parseColor("#2E7D32"));
                    tvNotice.setVisibility(View.VISIBLE);
                }
                btnModify.setAlpha(1.0f);
                btnCancelBooking.setAlpha(1.0f);
                btnModify.setOnClickListener(v -> {
                    dialog.dismiss();
                    showModifyReservationDialog(r);
                });
                btnCancelBooking.setOnClickListener(v -> {
                    dialog.dismiss();
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Cancel Booking?")
                            .setMessage("Cancellation requires at least 12 hours notice.")
                            .setNegativeButton("No", null)
                            .setPositiveButton("Yes, Cancel", (d, w) -> cancelBooking(ApiUtils.str(r, "id")))
                            .show();
                });
            }
        }
    }

    // ── Modify: edit energy amount (kW) and optionally choose a new slot ──
    private void showModifyReservationDialog(JsonObject booking) {
        String resId = ApiUtils.str(booking, "id");
        String code = ApiUtils.str(booking, "reservationCode");
        String node = ApiUtils.str(booking, "nodeName");
        String originalDate = ApiUtils.str(booking, "reservationDate");
        String shortDate = originalDate.length() >= 10 ? originalDate.substring(0, 10) : originalDate;
        String originalStart = ApiUtils.str(booking, "startTime");
        String originalEnd = ApiUtils.str(booking, "endTime");
        String originalSlotId = ApiUtils.str(booking, "energySlotId");
        double originalKwh = ApiUtils.num(booking, "energyAmountKwh");

        final String[] selectedSlotId = new String[]{ originalSlotId };
        final String[] selectedDate = new String[]{ originalDate };
        final String[] selectedStart = new String[]{ originalStart };
        final String[] selectedEnd = new String[]{ originalEnd };

        View view = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_modify_reservation, null);

        TextView tvModifyCode = view.findViewById(R.id.tvModifyCode);
        tvModifyCode.setText(code + (node.isEmpty() ? "" : "  ·  " + node));

        TextView tvModifySlot = view.findViewById(R.id.tvModifySlot);
        tvModifySlot.setText("📅 " + shortDate + "   ⏱ " + originalStart + " - " + originalEnd);

        TextView tvModifyCapacity = view.findViewById(R.id.tvModifyCapacity);

        com.google.android.material.textfield.TextInputEditText etEnergyAmount = view.findViewById(R.id.etEnergyAmount);
        etEnergyAmount.setText(fmt(originalKwh));
        etEnergyAmount.selectAll();

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setBackground(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                .show();

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btnChangeSlot).setOnClickListener(v -> {
            showLoading();
            ApiClient.get().slots("", true).enqueue(new Callback<JsonElement>() {
                @Override
                public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                    hideLoading();
                    if (!r.isSuccessful() || r.body() == null) { toast(errorMsg(r)); return; }
                    JsonArray rawSlots = r.body().getAsJsonArray();
                    JsonArray slots = ApiUtils.filterBookableSlots(rawSlots);
                    if (slots.size() == 0) {
                        toast("No alternative future slots available within 7 days.");
                        return;
                    }

                    String[] labels = new String[slots.size()];
                    for (int i = 0; i < slots.size(); i++) {
                        JsonObject s = slots.get(i).getAsJsonObject();
                        String d = ApiUtils.str(s, "slotDate");
                        labels[i] = (d.length() >= 10 ? d.substring(0, 10) : d) + "   "
                                + ApiUtils.str(s, "startTime") + " - " + ApiUtils.str(s, "endTime")
                                + "   (" + fmt(ApiUtils.num(s, "availableCapacityKwh")) + " kWh avail)";
                    }
                    new MaterialAlertDialogBuilder(BookingsActivity.this)
                            .setTitle("Choose New Time Slot")
                            .setItems(labels, (d, w) -> {
                                JsonObject chosen = slots.get(w).getAsJsonObject();
                                selectedSlotId[0] = ApiUtils.str(chosen, "id");
                                selectedDate[0] = ApiUtils.str(chosen, "slotDate");
                                selectedStart[0] = ApiUtils.str(chosen, "startTime");
                                selectedEnd[0] = ApiUtils.str(chosen, "endTime");

                                String displayDate = selectedDate[0].length() >= 10 ? selectedDate[0].substring(0, 10) : selectedDate[0];
                                tvModifySlot.setText("📅 " + displayDate + "   ⏱ " + selectedStart[0] + " - " + selectedEnd[0]);

                                double avail = ApiUtils.num(chosen, "availableCapacityKwh");
                                tvModifyCapacity.setText("Available in selected slot: " + fmt(avail) + " kWh");
                                tvModifyCapacity.setVisibility(View.VISIBLE);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
                @Override
                public void onFailure(Call<JsonElement> c, Throwable t) { hideLoading(); fail(t); }
            });
        });

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String val = etEnergyAmount.getText() != null ? etEnergyAmount.getText().toString().trim() : "";
            double kwh;
            try {
                kwh = Double.parseDouble(val);
            } catch (Exception e) {
                toast("Please enter a valid energy amount");
                return;
            }
            if (kwh <= 0) {
                toast("Energy amount must be greater than 0");
                return;
            }

            dialog.dismiss();

            JsonObject body = new JsonObject();
            body.addProperty("energySlotId", selectedSlotId[0]);
            body.addProperty("energyAmountKwh", kwh);

            updateBooking(resId, body, selectedSlotId[0], selectedDate[0], selectedStart[0], selectedEnd[0], kwh);
        });
    }

    private void updateBooking(String id, JsonObject body, String slotId, String date, String start, String end, double kwh) {
        showLoading();
        ApiClient.get().updateReservation(id, session.nic(), body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (r.isSuccessful()) {
                    db.updateReservationFull(id, slotId, date, start, end, kwh, "Pending");
                    toast("Booking updated to " + fmt(kwh) + " kWh. It is now Pending approval.");
                    loadBookings();
                } else {
                    toast(errorMsg(r));
                }
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }
    
    private void cancelBooking(String id) {
        showLoading();
        ApiClient.get().cancelReservation(id, session.nic()).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (r.isSuccessful()) { db.updateReservationStatus(id, "Cancelled"); toast("Booking cancelled"); loadBookings(); }
                else toast(errorMsg(r));
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void generateQr(String resId) {
        showLoading();
        ApiClient.get().generateQr(resId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful() || r.body() == null) { toast(errorMsg(r)); return; }
                showQrDialog(r.body());
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void showQrDialog(JsonObject q) {
        String token = ApiUtils.str(q, "qrToken");
        if (token.isEmpty()) token = ApiUtils.str(q, "token");
        if (token.isEmpty()) token = ApiUtils.str(q, "qrData");

        String txCode = ApiUtils.str(q, "transactionCode");

        ImageView img = new ImageView(this);
        img.setPadding(32, 32, 32, 32);
        try {
            BitMatrix m = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 650, 650);
            Bitmap bmp  = Bitmap.createBitmap(650, 650, Bitmap.Config.RGB_565);
            for (int x = 0; x < 650; x++) {
                for (int y = 0; y < 650; y++) {
                    bmp.setPixel(x, y, m.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            img.setImageBitmap(bmp);
        } catch (Exception e) {
            toast("QR code rendering error: " + e.getMessage());
            return;
        }

        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle("Transaction QR Code")
                .setMessage("📋 Code: " + txCode + "\n\nShow this QR to the Grid Operator at the charging station to complete energy transfer.")
                .setView(img)
                .setPositiveButton("Done", null)
                .show();
    }

    private String fmt(double d) {
        return ApiUtils.fmt(d);
    }
}
