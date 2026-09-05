package com.careychi.hrstrap.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Fixed-width three-slot BPM display. Each digit owns a stable TextView. */
public final class TripleDigitView extends LinearLayout {
    private final TextView[] digits = new TextView[3];

    public TripleDigitView(Context context, float textSp) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < 3; i++) {
            TextView d = Ui.text(context, " ", textSp, Ui.TEXT);
            d.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            d.setGravity(Gravity.CENTER);
            int width = Ui.dp(context, textSp * 0.74f);
            addView(d, new LayoutParams(width, LayoutParams.WRAP_CONTENT));
            digits[i] = d;
        }
    }

    public void setValue(int bpm) {
        if (bpm < 0 || bpm > 999) {
            setUnavailable();
            return;
        }
        String s = String.format(java.util.Locale.US, "%3d", bpm);
        for (int i = 0; i < 3; i++) digits[i].setText(String.valueOf(s.charAt(i)));
    }

    public void setUnavailable() {
        for (TextView digit : digits) digit.setText("-");
    }

    public void setDigitColor(int color) {
        for (TextView digit : digits) digit.setTextColor(color);
    }
}
