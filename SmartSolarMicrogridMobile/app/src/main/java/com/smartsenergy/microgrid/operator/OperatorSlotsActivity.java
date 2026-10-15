package com.smartsenergy.microgrid.operator;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.smartsenergy.microgrid.BaseActivity;
import com.smartsolar.microgrid.R;
import com.smartsenergy.microgrid.api.ApiClient;
import com.smartsenergy.microgrid.utils.ApiUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OperatorSlotsActivity extends BaseActivity {

    private LinearLayout slotsContainer;
    private boolean availableOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_slots);

        slotsContainer = findViewById(R.id.slotsContainer);
        availableOnly = getIntent().getBooleanExtra("availableOnly", true);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(availableOnly ? "Available Slots" : "Update Slots");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadSlots();
    }

    private void loadSlots() {
        showLoading();
        slotsContainer.removeAllViews();

        ApiClient.get().operatorSlots("", availableOnly).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                hideLoading();
                if (!r.isSuccessful() || r.body() == null) {
                    toast(errorMsg(r));
                    TextView tv = new TextView(OperatorSlotsActivity.this);
                    tv.setText("API Error:\n" + errorMsg(r));
                    tv.setTextSize(16f);
                    tv.setTextColor(android.graphics.Color.RED);
                    tv.setPadding(32, 64, 32, 32);
                    tv.setGravity(android.view.Gravity.CENTER);
                    slotsContainer.addView(tv);
                    return;
                }
                JsonArray a = r.body().getAsJsonArray();
                if (a.size() == 0) {
                    TextView tv = new TextView(OperatorSlotsActivity.this);
                    tv.setText("No slots found.");
                    tv.setTextSize(16f);
                    tv.setTextColor(android.graphics.Color.GRAY);
                    tv.setPadding(32, 64, 32, 32);
                    tv.setGravity(android.view.Gravity.CENTER);
                    slotsContainer.addView(tv);
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(OperatorSlotsActivity.this);
                LinearLayout row = null;

                for (int i = 0; i < a.size(); i++) {
                    if (i % 2 == 0) {
                        row = new LinearLayout(OperatorSlotsActivity.this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setWeightSum(2);
                        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                        slotsContainer.addView(row, rowParams);
                    }

                    JsonObject x = a.get(i).getAsJsonObject();
                    View card = inflater.inflate(R.layout.item_operator_slot, row, false);
                    
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    p.setMargins(12, 12, 12, 12);
                    card.setLayoutParams(p);

                    TextView tvDate = card.findViewById(R.id.tvDate);
                    TextView tvTime = card.findViewById(R.id.tvTime);
                    TextView tvEnergy = card.findViewById(R.id.tvEnergy);
                    View btnUpdate = card.findViewById(R.id.btnUpdate);

                    tvDate.setText("📅 " + shortDate(ApiUtils.str(x, "slotDate")));
                    tvTime.setText("⏱ " + ApiUtils.str(x, "startTime"));
                    tvEnergy.setText(fmt(ApiUtils.num(x, "availableCapacityKwh")) + " kWh");

                    if (!availableOnly) {
                        btnUpdate.setVisibility(View.VISIBLE);
                        btnUpdate.setOnClickListener(v -> capacityDialog(x));
                    }

                    row.addView(card);
                }
                
                // If odd number of items, add a dummy view to the last row to maintain width
                if (a.size() % 2 != 0 && row != null) {
                    View dummy = new View(OperatorSlotsActivity.this);
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 0, 1f);
                    p.setMargins(12, 12, 12, 12);
                    dummy.setLayoutParams(p);
                    row.addView(dummy);
                }
            }

            @Override
            public void onFailure(Call<JsonElement> c, Throwable t) {
                hideLoading();
                fail(t);
                TextView tv = new TextView(OperatorSlotsActivity.this);
                tv.setText("Failed to load data. Is the backend running?");
                tv.setTextSize(16f);
                tv.setTextColor(android.graphics.Color.RED);
                tv.setPadding(32, 64, 32, 32);
                tv.setGravity(android.view.Gravity.CENTER);
                slotsContainer.addView(tv);
            }
        });
    }

    private void capacityDialog(JsonObject slot) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_update_capacity, null);
        
        TextView tvSubtitle = view.findViewById(R.id.tvDialogSubtitle);
        tvSubtitle.setText("Date: " + shortDate(ApiUtils.str(slot, "slotDate")) + "\nTime: " + ApiUtils.str(slot, "startTime"));
        
        com.google.android.material.textfield.TextInputEditText etCapacity = view.findViewById(R.id.etCapacity);
        etCapacity.setText(fmt(ApiUtils.num(slot, "availableCapacityKwh")));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .show();

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            try {
                updateSlot(ApiUtils.str(slot, "id"), Double.parseDouble(etCapacity.getText().toString()));
                dialog.dismiss();
            } catch (Exception e) {
                toast("Enter a valid number");
            }
        });
    }

    private void updateSlot(String id, double capacity) {
        showLoading();
        ApiClient.get().updateAvailability(id, capacity).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (r.isSuccessful()) {
                    toast("✅ Slot availability updated");
                    loadSlots();
                } else {
                    toast(errorMsg(r));
                }
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) {
                hideLoading();
                fail(t);
            }
        });
    }

    private String shortDate(String s) {
        return (s != null && s.length() >= 10) ? s.substring(0, 10) : s;
    }

    private String fmt(double d) {
        return d == (long) d ? String.valueOf((long) d) : String.valueOf(d);
    }
}
