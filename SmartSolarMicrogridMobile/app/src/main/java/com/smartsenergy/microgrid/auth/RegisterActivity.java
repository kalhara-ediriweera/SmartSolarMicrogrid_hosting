package com.smartsenergy.microgrid.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.smartsenergy.microgrid.BaseActivity;
import com.smartsolar.microgrid.R;
import com.smartsenergy.microgrid.api.ApiClient;
import com.smartsenergy.microgrid.utils.ApiUtils;
import com.smartsenergy.microgrid.utils.NicValidator;
import java.io.ByteArrayOutputStream;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends BaseActivity {

    private TextInputLayout tilNic, tilName, tilEmail, tilPhone, tilAddress, tilUsername, tilPassword;
    private ImageView ivNicFront, ivNicBack;
    private String base64NicFront = "";
    private String base64NicBack = "";
    private boolean isPickingFront = true;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                        byte[] imageBytes = baos.toByteArray();
                        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                        if (isPickingFront) {
                            ivNicFront.setImageBitmap(bitmap);
                            base64NicFront = base64Image;
                        } else {
                            ivNicBack.setImageBitmap(bitmap);
                            base64NicBack = base64Image;
                        }
                    } catch (Exception e) {
                        toast("Failed to process image");
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tilNic      = findViewById(R.id.tilNic);
        tilName     = findViewById(R.id.tilName);
        tilEmail    = findViewById(R.id.tilEmail);
        tilPhone    = findViewById(R.id.tilPhone);
        tilAddress  = findViewById(R.id.tilAddress);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);

        ivNicFront  = findViewById(R.id.ivNicFront);
        ivNicBack   = findViewById(R.id.ivNicBack);

        MaterialButton btnRegister = findViewById(R.id.btnRegister);
        MaterialButton btnBack     = findViewById(R.id.btnBack);
        MaterialButton btnPickNicFront = findViewById(R.id.btnPickNicFront);
        MaterialButton btnPickNicBack = findViewById(R.id.btnPickNicBack);

        setupNicValidation();

        btnPickNicFront.setOnClickListener(v -> pickImage(true));
        btnPickNicBack.setOnClickListener(v -> pickImage(false));

        btnRegister.setOnClickListener(v -> register());
        btnBack.setOnClickListener(v -> finish());
    }

    private void pickImage(boolean isFront) {
        isPickingFront = isFront;
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void setupNicValidation() {
        TextInputEditText etNic = (TextInputEditText) tilNic.getEditText();
        if (etNic == null) return;

        etNic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s == null) return;
                String input = s.toString().trim();

                if (input.isEmpty()) {
                    tilNic.setError(null);
                    tilNic.setHelperText("Old (981234567V) or New (199812345678)");
                    return;
                }

                // If user typed 9 pure digits, prompt for V/X suffix
                if (input.length() == 9 && input.matches("^[0-9]+$")) {
                    tilNic.setError("Old 9-digit NIC must end with 'V' or 'X' (e.g. " + input + "V)");
                    return;
                }

                // If user entered complete length (10 or 12)
                if (input.length() == 10 || input.length() == 12) {
                    String error = NicValidator.validate(input);
                    if (error != null) {
                        tilNic.setError(error);
                    } else {
                        tilNic.setError(null);
                        NicValidator.NicDetails details = NicValidator.getDetails(input);
                        tilNic.setHelperText("✓ " + details.formatType + " · " + details.gender + " · " + details.dateOfBirth + " (Age " + details.age + ")");
                    }
                } else {
                    tilNic.setError(null);
                    tilNic.setHelperText("Old (981234567V) or New (199812345678)");
                }
            }
        });
    }

    private void register() {
        String rawNic = val(tilNic);
        String name   = val(tilName);
        String user   = val(tilUsername);
        String pass   = val(tilPassword);

        // Validate NIC specifically
        String nicError = NicValidator.validate(rawNic);
        if (nicError != null) {
            tilNic.setError(nicError);
            if (tilNic.getEditText() != null) tilNic.getEditText().requestFocus();
            toast(nicError);
            return;
        } else {
            tilNic.setError(null);
        }

        if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            toast("Full name, username and password are required");
            return;
        }

        if (base64NicFront.isEmpty() || base64NicBack.isEmpty()) {
            toast("Please upload both Front and Back images of your NIC");
            return;
        }

        final String formattedNic = NicValidator.format(rawNic);

        JsonObject body = new JsonObject();
        body.addProperty("NIC",      formattedNic);
        body.addProperty("FullName", name);
        body.addProperty("Email",    val(tilEmail));
        body.addProperty("Phone",    val(tilPhone));
        body.addProperty("Address",  val(tilAddress));
        body.addProperty("Username", user);
        body.addProperty("Password", pass);
        body.addProperty("NicFrontImageBase64", base64NicFront);
        body.addProperty("NicBackImageBase64", base64NicBack);

        showLoading();

        ApiClient.get().register(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                hideLoading();
                if (!r.isSuccessful()) {
                    String msg = errorMsg(r);
                    if (msg.toLowerCase().contains("nic") || msg.toLowerCase().contains("drp")) {
                        tilNic.setError(msg);
                        if (tilNic.getEditText() != null) tilNic.getEditText().requestFocus();
                    } else if (msg.toLowerCase().contains("username")) {
                        tilUsername.setError(msg);
                        if (tilUsername.getEditText() != null) tilUsername.getEditText().requestFocus();
                    }
                    toast(msg);
                    return;
                }
                // Cache NIC for post-approval login hint
                getSharedPreferences("login_meta", 0)
                        .edit().putString("nic", formattedNic).apply();
                toast("Registration submitted! Backoffice approval is required before you can sign in.");
                finish();
            }

            @Override
            public void onFailure(Call<JsonObject> c, Throwable t) {
                hideLoading();
                fail(t);
            }
        });
    }
}
