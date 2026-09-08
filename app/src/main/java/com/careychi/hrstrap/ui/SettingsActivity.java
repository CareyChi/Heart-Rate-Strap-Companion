package com.careychi.hrstrap.ui;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.careychi.hrstrap.OledBurnInProtection;
import com.careychi.hrstrap.PureBlackBackground;
import com.careychi.hrstrap.data.RecordingRecovery;

public final class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 36), Ui.dp(this, 20), Ui.dp(this, 24));

        LinearLayout top = Ui.row(this);
        TextView back = Ui.text(this, "‹", 36, Ui.TEXT);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 48)));
        top.addView(Ui.title(this, "应用设置"));
        root.addView(top);

        LinearLayout background = Ui.row(this);
        background.setGravity(Gravity.CENTER_VERTICAL);
        background.setBackground(Ui.rounded(Ui.SURFACE, 20, this));
        Ui.pad(background, 16);
        LinearLayout bgText = Ui.column(this);
        bgText.addView(Ui.text(this, "后台运行", 18, Ui.TEXT));
        TextView bgDesc = Ui.text(this, "悬浮窗、电池优化、应用加锁与自启动", 13, Ui.MUTED);
        bgDesc.setPadding(0, Ui.dp(this, 5), 0, 0);
        bgText.addView(bgDesc);
        background.addView(bgText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView arrow = Ui.text(this, "›", 30, Ui.MUTED);
        arrow.setGravity(Gravity.CENTER);
        background.addView(arrow, new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 48)));
        background.setOnClickListener(v -> startActivity(new Intent(this, BackgroundSettingsActivity.class)));
        LinearLayout.LayoutParams bgLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bgLp.topMargin = Ui.dp(this, 14);
        root.addView(background, bgLp);

        LinearLayout recovery = Ui.column(this);
        recovery.setBackground(Ui.rounded(Ui.SURFACE, 20, this));
        Ui.pad(recovery, 16);
        LinearLayout switchRow = Ui.row(this);
        TextView title = Ui.text(this, "防止意外中断", 18, Ui.TEXT);
        switchRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(RecordingRecovery.isEnabled(this));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> RecordingRecovery.setEnabled(this, isChecked));
        switchRow.addView(toggle);
        recovery.addView(switchRow);
        TextView note = Ui.text(this, "在意外退出时保存已记录数据", 12, Ui.MUTED);
        note.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        note.setAlpha(0.82f);
        note.setPadding(0, Ui.dp(this, 6), 0, 0);
        recovery.addView(note);
        LinearLayout.LayoutParams recoveryLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        recoveryLp.topMargin = Ui.dp(this, 14);
        root.addView(recovery, recoveryLp);

        LinearLayout pureBlack = Ui.column(this);
        pureBlack.setBackground(Ui.rounded(Ui.SURFACE, 20, this));
        Ui.pad(pureBlack, 16);
        LinearLayout pureBlackRow = Ui.row(this);
        TextView pureBlackTitle = Ui.text(this, "使用纯黑背景", 18, Ui.TEXT);
        pureBlackRow.addView(pureBlackTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch pureBlackToggle = new Switch(this);
        pureBlackToggle.setChecked(PureBlackBackground.isEnabled(this));
        pureBlackToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PureBlackBackground.setEnabled(this, isChecked);
            recreate();
        });
        pureBlackRow.addView(pureBlackToggle);
        pureBlack.addView(pureBlackRow);
        TextView pureBlackNote = Ui.text(this, "OLED用户请无视，仅推荐LCD屏开启，以获得更加沉浸的体验", 12, Ui.MUTED);
        pureBlackNote.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        pureBlackNote.setAlpha(0.82f);
        pureBlackNote.setPadding(0, Ui.dp(this, 6), 0, 0);
        pureBlack.addView(pureBlackNote);
        LinearLayout.LayoutParams pureBlackLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pureBlackLp.topMargin = Ui.dp(this, 14);
        root.addView(pureBlack, pureBlackLp);

        LinearLayout oled = Ui.column(this);
        oled.setBackground(Ui.rounded(Ui.SURFACE, 20, this));
        Ui.pad(oled, 16);
        LinearLayout oledRow = Ui.row(this);
        TextView oledTitle = Ui.text(this, "OLED防烧屏", 18, Ui.TEXT);
        oledRow.addView(oledTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch oledToggle = new Switch(this);
        oledToggle.setChecked(OledBurnInProtection.isEnabled(this));
        oledToggle.setOnCheckedChangeListener((buttonView, isChecked) -> OledBurnInProtection.setEnabled(this, isChecked));
        oledRow.addView(oledToggle);
        oled.addView(oledRow);
        TextView oledNote = Ui.text(this, "定期位移界面，改善长期显示静止内容导致的烧屏现象", 12, Ui.MUTED);
        oledNote.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        oledNote.setAlpha(0.82f);
        oledNote.setPadding(0, Ui.dp(this, 6), 0, 0);
        oled.addView(oledNote);
        LinearLayout.LayoutParams oledLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        oledLp.topMargin = Ui.dp(this, 14);
        root.addView(oled, oledLp);

        setContentView(root);
    }
}
