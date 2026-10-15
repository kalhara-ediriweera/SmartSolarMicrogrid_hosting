package com.smartsenergy.microgrid.prosumer;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;
import com.smartsenergy.microgrid.BaseActivity;
import com.smartsolar.microgrid.R;
import com.smartsenergy.microgrid.api.ApiClient;
import com.smartsenergy.microgrid.database.DatabaseHelper;
import com.smartsenergy.microgrid.utils.ApiUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends BaseActivity {

    private EditText etName, etEmail, etPhone, etAddress;
    private TextView tvNic;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = new DatabaseHelper(this);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        tvNic = findViewById(R.id.tvNic);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            JsonObject j = new JsonObject();
            j.addProperty("fullName", etName.getText().toString());
            j.addProperty("email",    etEmail.getText().toString());
            j.addProperty("phone",    etPhone.getText().toString());
            j.addProperty("address",  etAddress.getText().toString());
            updateProfile(j);
        });

        findViewById(R.id.btnDeactivate).setOnClickListener(v -> deactivate());

        setupBottomNav(R.id.nav_profile);
        loadProfile();
    }

    private void loadProfile() {
        tvNic.setText("NIC: " + session.nic());
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

                etName.setText(ApiUtils.str(x, "fullName"));
                etEmail.setText(ApiUtils.str(x, "email"));
                etPhone.setText(ApiUtils.str(x, "phone"));
                etAddress.setText(ApiUtils.str(x, "address"));
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
                toast("Profile updated successfully");
                finish();
            }
            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) { hideLoading(); fail(t); }
        });
    }

    private void deactivate() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Deactivation")
                .setMessage("Are you sure you want to request account deactivation?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm", (d, w) -> {
                    showLoading();
                    ApiClient.get().requestDeactivation(session.nic())
                            .enqueue(new Callback<JsonObject>() {
                                @Override
                                public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                                    hideLoading();
                                    if (r.isSuccessful()) {
                                        toast("Deactivation request submitted.");
                                        finish();
                                    } else {
                                        toast(errorMsg(r));
                                    }
                                }
                                @Override
                                public void onFailure(Call<JsonObject> c, Throwable t) {
                                    hideLoading(); fail(t);
                                }
                            });
                }).show();
    }
}
