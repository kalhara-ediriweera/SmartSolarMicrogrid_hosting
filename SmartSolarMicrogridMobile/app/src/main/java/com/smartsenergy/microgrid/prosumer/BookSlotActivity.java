package com.smartsenergy.microgrid.prosumer;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.smartsenergy.microgrid.BaseActivity;
import com.smartsolar.microgrid.R;
import com.smartsenergy.microgrid.api.ApiClient;
import com.smartsenergy.microgrid.utils.ApiUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookSlotActivity extends BaseActivity {

    private LinearLayout slotsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_slot);

        slotsContainer = findViewById(R.id.slotsContainer);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupBottomNav(R.id.nav_home);
        loadSlots();
    }

    private void loadSlots() {
        showLoading();
        ApiClient.get().slots("", true).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                hideLoading();
                if (!r.isSuccessful() || r.body() == null) { toast(errorMsg(r)); return; }
                JsonArray a = r.body().getAsJsonArray();
                slotsContainer.removeAllViews();
                
                if (a.size() == 0) {
                    TextView tv = new TextView(BookSlotActivity.this);
                    tv.setText("No available slots found at this time.");
                    tv.setTextColor(Color.GRAY);
                    slotsContainer.addView(tv);
                    return;
                }

                for (int i = 0; i < a.size(); i++) {
                    JsonObject s = a.get(i).getAsJsonObject();
                    addSlotCard(s);
                }
            }
            @Override
            public void onFailure(Call<JsonElement> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void addSlotCard(JsonObject slot) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);
        card.setCardElevation(4f);
        card.setRadius(20f);
        card.setUseCompatPadding(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(40, 40, 40, 40);

        TextView tvDate = new TextView(this);
        tvDate.setText(shortDate(ApiUtils.str(slot, "slotDate")));
        tvDate.setTextSize(16f);
        tvDate.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDate.setTextColor(Color.BLACK);
        box.addView(tvDate);

        TextView tvTime = new TextView(this);
        tvTime.setText("Time: " + ApiUtils.str(slot, "startTime") + " - " + ApiUtils.str(slot, "endTime"));
        tvTime.setTextSize(14f);
        tvTime.setTextColor(Color.DKGRAY);
        tvTime.setPadding(0, 8, 0, 0);
        box.addView(tvTime);

        TextView tvCap = new TextView(this);
        tvCap.setText("Available: " + fmt(ApiUtils.num(slot, "availableCapacityKwh")) + " kWh");
        tvCap.setTextSize(14f);
        tvCap.setTextColor(Color.parseColor("#4CAF50")); // Green
        tvCap.setPadding(0, 4, 0, 24);
        box.addView(tvCap);

        MaterialButton btnBook = new MaterialButton(this);
        btnBook.setText("Reserve this Slot");
        btnBook.setCornerRadius(12);
        btnBook.setOnClickListener(v -> showEnergyDialog(slot));
        box.addView(btnBook);

        card.addView(box);
        slotsContainer.addView(card);
    }

    private void showEnergyDialog(JsonObject slot) {
        android.view.View view = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_reserve_slot, null);
        com.google.android.material.textfield.TextInputEditText etCapacity = view.findViewById(R.id.etCapacity);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setBackground(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                .show();

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            createReservation(ApiUtils.str(slot, "id"), etCapacity.getText().toString());
            dialog.dismiss();
        });
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
                    if (r.isSuccessful()) {
                        toast("Slot reserved successfully!");
                        finish();
                    } else {
                        toast(errorMsg(r));
                    }
                }
                @Override
                public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
            });
        } catch (Exception e) { toast("Invalid amount"); }
    }

    private String shortDate(String d) {
        try {
            Date dt = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d);
            return new SimpleDateFormat("MMM dd, yyyy", Locale.US).format(dt);
        } catch (Exception e) { return d; }
    }

    private String fmt(double d) {
        if (d == (long) d) return String.format(Locale.US, "%d", (long) d);
        else return String.format(Locale.US, "%.1f", d);
    }
}
