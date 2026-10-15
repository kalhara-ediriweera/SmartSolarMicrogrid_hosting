package com.smartsenergy.microgrid.operator;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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

public class OperatorReservationsActivity extends BaseActivity {

    private LinearLayout reservationsContainer;
    private String statusFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_reservations);

        reservationsContainer = findViewById(R.id.reservationsContainer);
        statusFilter = getIntent().getStringExtra("status");
        if (statusFilter == null) statusFilter = "Approved";

        TextView tvTitle = findViewById(R.id.tvTitle);
        if ("All".equalsIgnoreCase(statusFilter)) {
            tvTitle.setText("All Reservations");
            findViewById(R.id.btnBack).setVisibility(View.GONE);
            setupBottomNav(R.id.nav_bookings);
        } else {
            tvTitle.setText(statusFilter + " Reservations");
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
            findViewById(R.id.bottomNav).setVisibility(View.GONE);
        }

        loadReservations();
    }

    private void loadReservations() {
        showLoading();
        reservationsContainer.removeAllViews();
        
        Call<JsonElement> call = "All".equalsIgnoreCase(statusFilter) 
            ? ApiClient.get().allReservations() 
            : ApiClient.get().reservationsByStatus(statusFilter);

        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                hideLoading();
                if (!r.isSuccessful() || r.body() == null) {
                    toast(errorMsg(r));
                    TextView tv = new TextView(OperatorReservationsActivity.this);
                    tv.setText("API Error:\n" + errorMsg(r));
                    tv.setTextSize(16f);
                    tv.setTextColor(android.graphics.Color.RED);
                    tv.setPadding(32, 64, 32, 32);
                    tv.setGravity(android.view.Gravity.CENTER);
                    reservationsContainer.addView(tv);
                    return;
                }
                JsonArray a = r.body().getAsJsonArray();
                if (a.size() == 0) {
                    TextView tv = new TextView(OperatorReservationsActivity.this);
                    tv.setText("No " + statusFilter.toLowerCase() + " reservations found.");
                    tv.setTextSize(16f);
                    tv.setTextColor(android.graphics.Color.GRAY);
                    tv.setPadding(32, 64, 32, 32);
                    tv.setGravity(android.view.Gravity.CENTER);
                    reservationsContainer.addView(tv);
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(OperatorReservationsActivity.this);
                for (int i = 0; i < a.size(); i++) {
                    JsonObject x = a.get(i).getAsJsonObject();
                    View card = inflater.inflate(R.layout.item_operator_reservation, reservationsContainer, false);

                    TextView tvResCode = card.findViewById(R.id.tvResCode);
                    TextView tvStatus = card.findViewById(R.id.tvStatus);
                    TextView tvProsumer = card.findViewById(R.id.tvProsumer);
                    TextView tvNode = card.findViewById(R.id.tvNode);
                    TextView tvDateTime = card.findViewById(R.id.tvDateTime);
                    TextView tvEnergy = card.findViewById(R.id.tvEnergy);
                    View btnAction = card.findViewById(R.id.btnAction);

                    tvResCode.setText(ApiUtils.str(x, "reservationCode"));
                    
                    String pName = ApiUtils.str(x, "prosumerName");
                    tvProsumer.setText(pName != null && !pName.isEmpty() ? pName : ApiUtils.str(x, "prosumerId"));
                    
                    String nName = ApiUtils.str(x, "nodeName");
                    tvNode.setText(nName != null && !nName.isEmpty() ? nName : ApiUtils.str(x, "nodeId"));
                    
                    String date = shortDate(ApiUtils.str(x, "reservationDate"));
                    tvDateTime.setText(date + " • " + ApiUtils.str(x, "startTime") + "-" + ApiUtils.str(x, "endTime"));
                    
                    tvEnergy.setText("⚡ " + fmt(ApiUtils.num(x, "energyAmountKwh")) + " kWh");

                    String status = ApiUtils.str(x, "status");
                    tvStatus.setText(status);
                    
                    if ("Pending".equalsIgnoreCase(status)) {
                        tvStatus.setTextColor(Color.parseColor("#F57C00"));
                        btnAction.setVisibility(View.VISIBLE);
                        btnAction.setOnClickListener(v -> approveReservation(ApiUtils.str(x, "id")));
                    } else if ("Approved".equalsIgnoreCase(status)) {
                        tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                    } else if ("Cancelled".equalsIgnoreCase(status)) {
                        tvStatus.setTextColor(Color.parseColor("#D32F2F"));
                    }

                    reservationsContainer.addView(card);
                }
            }

            @Override
            public void onFailure(Call<JsonElement> c, Throwable t) {
                hideLoading();
                fail(t);
                TextView tv = new TextView(OperatorReservationsActivity.this);
                tv.setText("Failed to load data. Is the backend running?");
                tv.setTextSize(16f);
                tv.setTextColor(android.graphics.Color.RED);
                tv.setPadding(32, 64, 32, 32);
                tv.setGravity(android.view.Gravity.CENTER);
                reservationsContainer.addView(tv);
            }
        });
    }

    private void approveReservation(String id) {
        showLoading();
        ApiClient.get().approveReservation(id).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (r.isSuccessful()) {
                    toast("✅ Reservation Approved!");
                    loadReservations(); // Reload list
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
