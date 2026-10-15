package com.smartsenergy.microgrid.prosumer;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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
    private TextView tvNic, tvHeaderName, tvHeaderUsername, tvRoleBadge, tvStatusBadge;
    private View dangerZoneLayout;
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
        tvHeaderName = findViewById(R.id.tvHeaderName);
        tvHeaderUsername = findViewById(R.id.tvHeaderUsername);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        dangerZoneLayout = findViewById(R.id.dangerZoneLayout);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        View btnLogoutProfile = findViewById(R.id.btnLogoutProfile);
        if (btnLogoutProfile != null) {
            btnLogoutProfile.setOnClickListener(v -> confirmLogout());
        }

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress.getText().toString().trim();

            if (name.isEmpty()) {
                toast("Please enter your full name");
                return;
            }

            JsonObject j = new JsonObject();
            j.addProperty("fullName", name);
            j.addProperty("email", email);
            j.addProperty("phone", phone);
            j.addProperty("address", address);
            updateProfile(j);
        });

        findViewById(R.id.btnDeactivate).setOnClickListener(v -> deactivate());

        setupBottomNav(R.id.nav_profile);
        loadProfile();
    }

    private void loadProfile() {
        String username = session.username();
        String role = session.role();
        String nic = session.nic();

        // 1. Initial immediate presentation from Session
        tvHeaderUsername.setText("@" + (username.isEmpty() ? "user" : username));
        tvRoleBadge.setText(role.isEmpty() ? "User" : role);
        tvStatusBadge.setText("Active");

        if (!nic.isEmpty()) {
            tvNic.setText("NIC: " + nic);
            tvHeaderName.setText(username.isEmpty() ? nic : username);
        } else {
            tvNic.setText("User: " + (username.isEmpty() ? "Account" : username));
            tvHeaderName.setText(username.isEmpty() ? "My Profile" : username);
        }

        // Check if role is GridOperator
        boolean isOperator = "GridOperator".equalsIgnoreCase(role) || "Operator".equalsIgnoreCase(role);
        if (isOperator) {
            if (dangerZoneLayout != null) dangerZoneLayout.setVisibility(View.GONE);
            tvRoleBadge.setText("Grid Operator");
            tvNic.setText("Account: Grid Operator");
        }

        // 2. Pre-fill from local SQLite cache if available
        try (Cursor c = db.getUser()) {
            if (c != null && c.moveToFirst()) {
                String cachedName = c.getString(c.getColumnIndexOrThrow("full_name"));
                String cachedEmail = c.getString(c.getColumnIndexOrThrow("email"));
                String cachedPhone = c.getString(c.getColumnIndexOrThrow("phone"));
                String cachedAddress = c.getString(c.getColumnIndexOrThrow("address"));
                String cachedNic = c.getString(c.getColumnIndexOrThrow("nic"));

                if (cachedName != null && !cachedName.isEmpty()) {
                    etName.setText(cachedName);
                    tvHeaderName.setText(cachedName);
                }
                if (cachedEmail != null && !cachedEmail.isEmpty()) etEmail.setText(cachedEmail);
                if (cachedPhone != null && !cachedPhone.isEmpty()) etPhone.setText(cachedPhone);
                if (cachedAddress != null && !cachedAddress.isEmpty()) etAddress.setText(cachedAddress);
                if (nic.isEmpty() && cachedNic != null && !cachedNic.isEmpty()) {
                    nic = cachedNic;
                    tvNic.setText("NIC: " + nic);
                }
            }
        } catch (Exception ignored) {}

        // If no NIC or user is operator, don't query prosumer endpoint
        if (nic == null || nic.trim().isEmpty() || isOperator) {
            return;
        }

        final String finalNic = nic;
        showLoading();
        ApiClient.get().getProsumer(finalNic).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful() || r.body() == null) {
                    return;
                }
                JsonObject x = r.body();
                String fullName = ApiUtils.str(x, "fullName");
                String email = ApiUtils.str(x, "email");
                String phone = ApiUtils.str(x, "phone");
                String address = ApiUtils.str(x, "address");
                String status = ApiUtils.str(x, "accountStatus");

                if (!status.isEmpty()) {
                    tvStatusBadge.setText(status);
                }

                if (!fullName.isEmpty()) {
                    tvHeaderName.setText(fullName);
                    etName.setText(fullName);
                }
                if (!email.isEmpty()) etEmail.setText(email);
                if (!phone.isEmpty()) etPhone.setText(phone);
                if (!address.isEmpty()) etAddress.setText(address);

                db.saveUser(finalNic, fullName, email, phone, address, session.username(), session.role());
            }

            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) {
                hideLoading();
                // If network fails, cached data is already visible
            }
        });
    }

    private void updateProfile(JsonObject body) {
        String nic = session.nic();
        if (nic.isEmpty()) {
            toast("Cannot update profile: NIC not found.");
            return;
        }

        showLoading();
        ApiClient.get().updateProsumer(nic, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) {
                    toast(errorMsg(r));
                    return;
                }
                
                String newName = ApiUtils.str(body, "fullName");
                String newEmail = ApiUtils.str(body, "email");
                String newPhone = ApiUtils.str(body, "phone");
                String newAddress = ApiUtils.str(body, "address");

                if (!newName.isEmpty()) tvHeaderName.setText(newName);
                db.saveUser(nic, newName, newEmail, newPhone, newAddress, session.username(), session.role());

                toast("Profile updated successfully");
            }

            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) {
                hideLoading();
                fail(t);
            }
        });
    }

    private void deactivate() {
        String nic = session.nic();
        if (nic.isEmpty()) {
            toast("Cannot request deactivation: NIC not found.");
            return;
        }

        new MaterialAlertDialogBuilder(this,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle("Confirm Deactivation")
                .setMessage("Are you sure you want to request account deactivation? This will remove your access to the microgrid trading system.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm Deactivate", (d, w) -> {
                    showLoading();
                    ApiClient.get().requestDeactivation(nic)
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
                                    hideLoading();
                                    fail(t);
                                }
                            });
                }).show();
    }
}
