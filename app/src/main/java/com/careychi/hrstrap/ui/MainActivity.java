package com.careychi.hrstrap.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.careychi.hrstrap.HeartRateState;
import com.careychi.hrstrap.service.HeartRateService;
import com.google.android.material.button.MaterialButton;
import java.util.*;

public final class MainActivity extends AppCompatActivity implements HeartRateState.Listener {
    private static final android.os.ParcelUuid HR_SERVICE = android.os.ParcelUuid.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private TripleDigitView bpmDigits;
    private TextView deviceName;
    private TextView status;
    private MaterialButton continuous;
    private BluetoothLeScanner scanner;
    private final LinkedHashMap<String, ScanResult> scanResults = new LinkedHashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> permissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {});

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        maybeShowFirstRunGuide();
        requestRuntimePermissions();
    }

    @Override protected void onStart() {
        super.onStart();
        HeartRateState.get().addListener(this);
    }

    @Override protected void onStop() {
        HeartRateState.get().removeListener(this);
        super.onStop();
    }

    private void buildUi() {
        getWindow().setStatusBarColor(Ui.BG);
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 22), Ui.dp(this, 18), Ui.dp(this, 22), Ui.dp(this, 24));

        LinearLayout top = Ui.row(this);
        TextView title = Ui.title(this, "心率带伴侣");
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView settings = iconButton("⚙");
        settings.setContentDescription("设置");
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        top.addView(settings, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        TextView history = iconButton("◷");
        history.setContentDescription("历史记录");
        history.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        top.addView(history, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        root.addView(top);

        Space s1 = new Space(this); root.addView(s1, new LinearLayout.LayoutParams(1, Ui.dp(this, 34)));

        LinearLayout deviceCard = Ui.row(this);
        deviceCard.setBackground(Ui.rounded(Ui.SURFACE, 20, this));
        Ui.pad(deviceCard, 16);
        TextView heart = Ui.text(this, "♥", 28, Ui.ACCENT);
        deviceCard.addView(heart, new LinearLayout.LayoutParams(Ui.dp(this, 50), Ui.dp(this, 64)));
        LinearLayout info = Ui.column(this);
        deviceName = Ui.text(this, "未连接心率带", 17, Ui.TEXT);
        status = Ui.text(this, "点击选择设备", 13, Ui.MUTED);
        info.addView(deviceName);
        info.addView(status);
        deviceCard.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        bpmDigits = new TripleDigitView(this, 24);
        bpmDigits.setUnavailable();
        deviceCard.addView(bpmDigits);
        TextView bpm = Ui.text(this, "bpm", 12, Ui.MUTED);
        deviceCard.addView(bpm);
        deviceCard.setOnClickListener(v -> scanForHeartRateDevices());
        root.addView(deviceCard, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 98)));

        LinearLayout center = Ui.column(this);
        center.setGravity(Gravity.CENTER);
        TextView hint = Ui.text(this, "连接心率带后开始记录", 14, Ui.MUTED);
        hint.setGravity(Gravity.CENTER);
        center.addView(hint, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 64)));

        MaterialButton riding = primaryButton("骑行记录 · 即将推出");
        riding.setEnabled(false);
        center.addView(riding, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 62)));
        Space gap = new Space(this); center.addView(gap, new LinearLayout.LayoutParams(1, Ui.dp(this, 24)));
        continuous = primaryButton("持续记录");
        continuous.setOnClickListener(v -> beginContinuousRecording());
        center.addView(continuous, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 62)));
        root.addView(center, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
    }

    private TextView iconButton(String symbol) {
        TextView v = Ui.text(this, symbol, 24, Ui.TEXT);
        v.setGravity(Gravity.CENTER);
        v.setBackground(Ui.rounded(Ui.SURFACE, 14, this));
        return v;
    }

    private MaterialButton primaryButton(String text) {
        MaterialButton b = new MaterialButton(this);
        b.setText(text);
        b.setTextSize(17);
        b.setTextColor(Ui.TEXT);
        b.setCornerRadius(Ui.dp(this, 18));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Ui.SURFACE_2));
        b.setStrokeColor(android.content.res.ColorStateList.valueOf(Ui.ACCENT));
        b.setStrokeWidth(Ui.dp(this, 1));
        return b;
    }

    private void maybeShowFirstRunGuide() {
        android.content.SharedPreferences prefs = getSharedPreferences("onboarding", MODE_PRIVATE);
        if (prefs.getBoolean("shown", false)) return;
        prefs.edit().putBoolean("shown", true).apply();
        new AlertDialog.Builder(this)
                .setTitle("建议允许后台持续运行")
                .setMessage("为了避免系统在记录期间清理应用，建议在最近任务或手机管家中给本应用加锁，并关闭电池优化。Android 没有统一的“应用加锁”授权接口，因此需要你在系统中手动完成。悬浮窗权限可在应用设置页单独开启。")
                .setNegativeButton("稍后处理", null)
                .setPositiveButton("打开应用设置", (d, w) -> startActivity(new Intent(this, SettingsActivity.class)))
                .show();
    }

    private void requestRuntimePermissions() {
        ArrayList<String> list = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 31) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) list.add(Manifest.permission.BLUETOOTH_SCAN);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) list.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            list.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) list.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!list.isEmpty()) permissions.launch(list.toArray(new String[0]));
    }

    @SuppressLint("MissingPermission")
    private void scanForHeartRateDevices() {
        if (!hasBlePermissions()) { requestRuntimePermissions(); return; }
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        if (manager == null || manager.getAdapter() == null || !manager.getAdapter().isEnabled()) {
            Toast.makeText(this, "请先开启蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        scanner = manager.getAdapter().getBluetoothLeScanner();
        if (scanner == null) return;
        scanResults.clear();
        status.setText("正在扫描心率带…");
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(HR_SERVICE).build();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
        handler.postDelayed(this::finishScanAndChoose, 6000);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override public void onScanResult(int callbackType, ScanResult result) {
            if (result == null || result.getDevice() == null) return;
            scanResults.put(result.getDevice().getAddress(), result);
        }
    };

    @SuppressLint("MissingPermission")
    private void finishScanAndChoose() {
        if (scanner != null) {
            try { scanner.stopScan(scanCallback); } catch (Exception ignored) {}
        }
        if (scanResults.isEmpty()) {
            status.setText("未发现标准 BLE 心率带，点击重试");
            return;
        }
        ArrayList<ScanResult> results = new ArrayList<>(scanResults.values());
        results.sort((a, b) -> Integer.compare(b.getRssi(), a.getRssi()));
        String[] names = new String[results.size()];
        for (int i = 0; i < results.size(); i++) {
            ScanResult r = results.get(i);
            String name = r.getDevice().getName();
            if (name == null || name.trim().isEmpty()) name = "未命名心率带";
            names[i] = name + "  ·  " + r.getRssi() + " dBm";
        }
        new AlertDialog.Builder(this).setTitle("选择心率带")
                .setItems(names, (d, which) -> connect(results.get(which)))
                .setNegativeButton("取消", null).show();
    }

    @SuppressLint("MissingPermission")
    private void connect(ScanResult result) {
        String name = result.getDevice().getName();
        if (name == null || name.trim().isEmpty()) name = "心率带";
        Intent service = new Intent(this, HeartRateService.class)
                .setAction(HeartRateService.ACTION_CONNECT)
                .putExtra(HeartRateService.EXTRA_ADDRESS, result.getDevice().getAddress())
                .putExtra(HeartRateService.EXTRA_NAME, name);
        ContextCompat.startForegroundService(this, service);
        status.setText("正在连接…");
    }

    private void beginContinuousRecording() {
        HeartRateState.State state = HeartRateState.get().snapshot();
        if (!state.connected()) {
            Toast.makeText(this, "请先连接心率带", Toast.LENGTH_SHORT).show();
            scanForHeartRateDevices();
            return;
        }
        ContextCompat.startForegroundService(this, new Intent(this, HeartRateService.class).setAction(HeartRateService.ACTION_START_RECORDING));
        startActivity(new Intent(this, ContinuousRecordingActivity.class));
    }

    private boolean hasBlePermissions() {
        if (Build.VERSION.SDK_INT >= 31) return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override public void onState(HeartRateState.State s) {
        deviceName.setText(s.deviceName());
        status.setText(s.connected() ? "已连接 · 点击切换设备" : "点击选择设备");
        if (s.latestBpm() > 0) bpmDigits.setValue(s.latestBpm()); else bpmDigits.setUnavailable();
        continuous.setEnabled(s.connected());
    }
}
