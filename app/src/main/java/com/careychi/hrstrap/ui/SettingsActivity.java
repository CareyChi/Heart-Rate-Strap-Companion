package com.careychi.hrstrap.ui;

import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public final class SettingsActivity extends AppCompatActivity {
    private TextView overlayStatus;
    private TextView batteryStatus;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStates();
    }

    private void buildUi() {
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 18), Ui.dp(this, 20), Ui.dp(this, 24));
        LinearLayout top = Ui.row(this);
        TextView back = Ui.text(this, "‹", 36, Ui.TEXT);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 48)));
        top.addView(Ui.title(this, "应用设置"));
        root.addView(top);

        root.addView(section("悬浮窗权限", "记录期间最小化应用后显示可拖动的实时心率悬浮窗。", true));
        root.addView(section("后台无限制", "建议关闭电池优化，降低长时间记录被系统中止的概率。", false));

        LinearLayout lock = Ui.column(this);
        lock.setBackground(Ui.rounded(Ui.SURFACE, 20, this));
        Ui.pad(lock, 16);
        lock.addView(Ui.text(this, "应用加锁 / 自启动", 18, Ui.TEXT));
        TextView note = Ui.text(this, "Android 没有统一的“最近任务加锁”接口。请在最近任务或手机管家中手动加锁，并在厂商后台管理中允许自启动/后台运行。", 14, Ui.MUTED);
        note.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 12));
        lock.addView(note);
        MaterialButton open = button("打开应用系统详情");
        open.setOnClickListener(v -> openAppDetails());
        lock.addView(open);
        LinearLayout.LayoutParams lockLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lockLp.topMargin = Ui.dp(this, 14);
        root.addView(lock, lockLp);
        setContentView(root);
    }

    private View section(String title, String description, boolean overlay) {
        LinearLayout card = Ui.column(this);
        card.setBackground(Ui.rounded(Ui.SURFACE, 20, this));
        Ui.pad(card, 16);
        card.addView(Ui.text(this, title, 18, Ui.TEXT));
        TextView desc = Ui.text(this, description, 14, Ui.MUTED);
        desc.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        card.addView(desc);
        TextView state = Ui.text(this, "检测中", 13, Ui.MUTED);
        card.addView(state);
        MaterialButton b = button("打开系统设置");
        b.setOnClickListener(v -> {
            if (overlay) openOverlay(); else openBatteryOptimization();
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52));
        blp.topMargin = Ui.dp(this, 10);
        card.addView(b, blp);
        if (overlay) overlayStatus = state; else batteryStatus = state;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 14);
        card.setLayoutParams(lp);
        return card;
    }

    private MaterialButton button(String text) {
        MaterialButton b = new MaterialButton(this);
        b.setText(text);
        b.setTextColor(Ui.TEXT);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Ui.SURFACE_2));
        b.setCornerRadius(Ui.dp(this, 14));
        return b;
    }

    private void refreshStates() {
        if (overlayStatus != null) {
            boolean granted = Settings.canDrawOverlays(this);
            overlayStatus.setText(granted ? "当前：已允许" : "当前：未允许");
            overlayStatus.setTextColor(granted ? Ui.ACCENT : Ui.MUTED);
        }
        if (batteryStatus != null) {
            android.os.PowerManager pm = getSystemService(android.os.PowerManager.class);
            boolean ignored = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
            batteryStatus.setText(ignored ? "当前：不受电池优化限制" : "当前：受电池优化限制");
            batteryStatus.setTextColor(ignored ? Ui.ACCENT : Ui.MUTED);
        }
    }

    private void openOverlay() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
        } catch (Exception e) { openAppDetails(); }
    }

    private void openBatteryOptimization() {
        try {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (Exception e) { openAppDetails(); }
    }

    private void openAppDetails() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
    }
}
