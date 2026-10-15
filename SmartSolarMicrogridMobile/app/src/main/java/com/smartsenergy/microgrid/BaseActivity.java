package com.smartsenergy.microgrid;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smartsolar.microgrid.R;
import com.smartsenergy.microgrid.utils.SessionManager;
import retrofit2.Response;

public abstract class BaseActivity extends AppCompatActivity {

    protected SessionManager session;
    protected ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager.init(this);
        session = SessionManager.getInstance();
        int targetMode = session.getThemeMode();
        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != targetMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode);
        }
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupThemeToggle();
        applySystemBarInsets();
    }

    // Android 15+ draws apps edge-to-edge, so the header sat under the status bar and its buttons
    // (e.g. logout) could not be tapped. Push the header below the status bar and the screen above the nav bar.
    private void applySystemBarInsets() {
        android.view.ViewGroup root = findViewById(android.R.id.content);
        if (root == null || root.getChildCount() == 0) return;
        View main = root.getChildAt(0);
        View header = null;
        if (main instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) main;
            for (int i = 0; i < vg.getChildCount(); i++) {
                if (vg.getChildAt(i) instanceof RelativeLayout) { header = vg.getChildAt(i); break; }
            }
        }
        // Headers center their icons with RelativeLayout rules that ignore padding, so instead of padding the
        // header, a spacer with the header's background is placed above it to fill the status bar area.
        View spacer = null;
        if (header != null && main instanceof LinearLayout
                && ((LinearLayout) main).getOrientation() == LinearLayout.VERTICAL) {
            spacer = new View(this);
            android.graphics.drawable.Drawable bg = header.getBackground();
            if (bg != null && bg.getConstantState() != null) spacer.setBackground(bg.getConstantState().newDrawable().mutate());
            else spacer.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
            spacer.setElevation(header.getElevation());
            ((LinearLayout) main).addView(spacer, ((LinearLayout) main).indexOfChild(header),
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0));
        }
        final View statusSpacer = spacer;
        final int mainTopPad = main.getPaddingTop(), mainBottomPad = main.getPaddingBottom();
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            int topInset = bars.top;
            if (statusSpacer != null) {
                statusSpacer.getLayoutParams().height = bars.top;
                statusSpacer.requestLayout();
                topInset = 0;
            }
            main.setPadding(main.getPaddingLeft(), mainTopPad + topInset, main.getPaddingRight(), mainBottomPad + bars.bottom);
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(main);
    }

    private void setupThemeToggle() {
        android.view.ViewGroup root = findViewById(android.R.id.content);
        if (root != null && root.getChildCount() > 0) {
            View mainLayout = root.getChildAt(0);
            if (mainLayout instanceof android.view.ViewGroup) {
                android.view.ViewGroup vg = (android.view.ViewGroup) mainLayout;
                // Find the header RelativeLayout (usually the first child)
                RelativeLayout header = null;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    if (child instanceof RelativeLayout) {
                        header = (RelativeLayout) child;
                        break;
                    }
                }
                
                if (header != null) {
                    int currentMode = session.getThemeMode();
                    boolean isDark = (currentMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);

                    float density = getResources().getDisplayMetrics().density;
                    int size40dp = (int) (40 * density);
                    int pad8dp = (int) (8 * density);
                    int icon20dp = (int) (20 * density);

                    FrameLayout toggleBtn = new FrameLayout(this);
                    toggleBtn.setBackgroundResource(R.drawable.bg_logo_circle);
                    toggleBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#33FFFFFF")));

                    ImageView ivIcon = new ImageView(this);
                    FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(icon20dp, icon20dp);
                    ivLp.gravity = android.view.Gravity.CENTER;
                    ivIcon.setLayoutParams(ivLp);
                    ivIcon.setImageResource(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);
                    toggleBtn.addView(ivIcon);

                    toggleBtn.setOnClickListener(v -> {
                        int newMode = isDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                                             : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
                        session.setThemeMode(newMode);
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(newMode);
                    });

                    // Search for an existing icon container (LinearLayout aligned to end)
                    LinearLayout iconContainer = null;
                    for (int j = 0; j < header.getChildCount(); j++) {
                        View c = header.getChildAt(j);
                        if (c instanceof LinearLayout && c.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) c.getLayoutParams();
                            int[] rules = lp.getRules();
                            if (rules[RelativeLayout.ALIGN_PARENT_END] == RelativeLayout.TRUE || 
                                rules[RelativeLayout.ALIGN_PARENT_RIGHT] == RelativeLayout.TRUE) {
                                iconContainer = (LinearLayout) c;
                                break;
                            }
                        }
                    }

                    if (iconContainer != null) {
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size40dp, size40dp);
                        lp.setMarginEnd(pad8dp);
                        lp.gravity = android.view.Gravity.CENTER_VERTICAL;
                        toggleBtn.setLayoutParams(lp);
                        iconContainer.addView(toggleBtn, 0); // Add as first child so it appears left of bell/logout
                    } else {
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size40dp, size40dp);
                        params.addRule(RelativeLayout.ALIGN_PARENT_END);
                        params.addRule(RelativeLayout.CENTER_VERTICAL);
                        params.setMarginEnd(pad8dp);
                        toggleBtn.setLayoutParams(params);
                        header.addView(toggleBtn);
                    }
                }
            }
        }
    }

    // ── Loading dialog ──────────────────────────────────────
    protected void showLoading() {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage("Please wait…");
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
        }
        if (!loadingDialog.isShowing() && !isFinishing()) {
            loadingDialog.show();
        }
    }

    protected void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        hideLoading();
        super.onDestroy();
    }

    // ── Feedback helpers ────────────────────────────────────
    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    protected void snack(View root, String msg) {
        Snackbar.make(root, msg, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.primary))
                .setTextColor(0xFFFFFFFF)
                .show();
    }

    protected void fail(Throwable t) {
        String msg = t.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = "Network request failed. Please check server connection.";
        }
        toast(msg);
    }

    // ── Text field helper ───────────────────────────────────
    protected String val(TextInputLayout til) {
        TextInputEditText et = (TextInputEditText) til.getEditText();
        return et == null ? "" : et.getText().toString().trim();
    }

    // ── Error body helper ───────────────────────────────────
    protected String errorMsg(Response<?> r) {
        if (r == null) return "Network error";
        if (r.errorBody() != null) {
            try {
                String body = r.errorBody().string();
                // API errors look like {"error":"...","status":409}; show only the message
                try {
                    com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    if (o.has("error") && !o.get("error").isJsonNull()) return o.get("error").getAsString();
                    if (o.has("title") && !o.get("title").isJsonNull()) return o.get("title").getAsString();
                } catch (Exception notJson) { /* fall through to the raw text */ }
                if (!body.trim().isEmpty()) return body;
            } catch (Exception ignored) {}
        }
        if (r.code() == 401) return "Session expired. Please log in again.";
        return "Request failed (" + r.code() + ")";
    }

    // ── Dashboard action card builder ───────────────────────
    protected void addActionCard(LinearLayout container, String title,
                                  int iconRes, View.OnClickListener click) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_action_card, container, false);
        ((TextView) card.findViewById(R.id.title)).setText(title);
        ImageView icon = card.findViewById(R.id.icon);
        icon.setImageResource(iconRes);
        card.setOnClickListener(click);

        MaterialCardView mcv = (MaterialCardView) card;
        mcv.setClickable(true);
        mcv.setFocusable(true);
        container.addView(card);
    }

    // ── Logout confirmation & execution ─────────────────────
    public void confirmLogout() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of your account?")
                .setIcon(android.R.drawable.ic_lock_power_off)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Yes, Log Out", (dialog, which) -> doLogout())
                .show();
    }

    protected void doLogout() {
        session.clear();
        android.content.Intent i = new android.content.Intent(this, com.smartsolar.microgrid.ui.LoginActivity.class);
        i.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    protected void setupBottomNav(int selectedItemId) {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        if (nav != null) {
            nav.setSelectedItemId(selectedItemId);
            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == selectedItemId) return true;
                
                android.content.Intent i = null;
                if (id == R.id.nav_home) {
                    if ("Operator".equalsIgnoreCase(session.role()) || "GridOperator".equalsIgnoreCase(session.role())) {
                        // Use the activity classes declared in AndroidManifest.xml
                        i = new android.content.Intent(this, com.smartsolar.microgrid.ui.OperatorActivity.class);
                    } else {
                        i = new android.content.Intent(this, com.smartsolar.microgrid.ui.ProsumerActivity.class);
                    }
                } else if (id == R.id.nav_bookings) {
                    if ("Operator".equalsIgnoreCase(session.role()) || "GridOperator".equalsIgnoreCase(session.role())) {
                        i = new android.content.Intent(this, com.smartsenergy.microgrid.operator.OperatorReservationsActivity.class);
                        i.putExtra("status", "All");
                    } else {
                        i = new android.content.Intent(this, com.smartsenergy.microgrid.prosumer.BookingsActivity.class);
                    }
                } else if (id == R.id.nav_profile) {
                    i = new android.content.Intent(this, com.smartsenergy.microgrid.prosumer.ProfileActivity.class);
                }
                
                if (i != null) {
                    i.setFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(i);
                    overridePendingTransition(0, 0); // Disable transition animation
                }
                return true;
            });
        }
    }
}
