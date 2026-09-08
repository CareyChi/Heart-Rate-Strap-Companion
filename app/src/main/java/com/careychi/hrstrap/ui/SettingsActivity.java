package com.careychi.hrstrap.ui;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.careychi.hrstrap.data.RecordingRecovery;

public final class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
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

        setContentView(root);
    }
}
