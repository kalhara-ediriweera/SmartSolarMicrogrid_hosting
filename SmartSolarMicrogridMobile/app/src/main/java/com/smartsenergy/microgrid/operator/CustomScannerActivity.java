package com.smartsenergy.microgrid.operator;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.smartsolar.microgrid.R;
import com.smartsenergy.microgrid.BaseActivity;

public class CustomScannerActivity extends BaseActivity {

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private boolean isTorchOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_scanner);

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);

        // Hide default small prompt inside the view since our layout has top & bottom guides
        if (barcodeScannerView != null && barcodeScannerView.getStatusView() != null) {
            barcodeScannerView.getStatusView().setVisibility(View.GONE);
        }

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        // Top Back Button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Bottom Cancel & Go Back Button
        View btnCancelScan = findViewById(R.id.btnCancelScan);
        if (btnCancelScan != null) {
            btnCancelScan.setOnClickListener(v -> finish());
        }

        // Flashlight / Torch toggle
        ImageView btnTorch = findViewById(R.id.btnTorch);
        if (btnTorch != null && barcodeScannerView != null) {
            btnTorch.setOnClickListener(v -> {
                if (isTorchOn) {
                    barcodeScannerView.setTorchOff();
                    btnTorch.setAlpha(0.6f);
                    isTorchOn = false;
                } else {
                    barcodeScannerView.setTorchOn();
                    btnTorch.setAlpha(1.0f);
                    isTorchOn = true;
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (capture != null) {
            capture.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (capture != null) {
            capture.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (capture != null) {
            capture.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (capture != null) {
            capture.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (capture != null) {
            capture.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return (barcodeScannerView != null && barcodeScannerView.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
    }
}
