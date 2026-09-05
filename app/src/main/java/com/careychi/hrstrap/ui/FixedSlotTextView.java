package com.careychi.hrstrap.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Fixed character slots for dates, times and durations so digits never shift laterally. */
public final class FixedSlotTextView extends LinearLayout {
    private final float textSp;
    private int color = Ui.TEXT;

    public FixedSlotTextView(Context context, float textSp) {
        super(context);
        this.textSp = textSp;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
    }

    public void setColor(int color) {
        this.color = color;
        for (int i = 0; i < getChildCount(); i++) ((TextView) getChildAt(i)).setTextColor(color);
    }

    public void setValue(String value) {
        if (value == null) value = "";
        while (getChildCount() < value.length()) {
            TextView slot = Ui.text(getContext(), " ", textSp, color);
            slot.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            slot.setGravity(Gravity.CENTER);
            addView(slot, new LayoutParams(Ui.dp(getContext(), textSp * 0.68f), LayoutParams.WRAP_CONTENT));
        }
        for (int i = 0; i < getChildCount(); i++) {
            TextView slot = (TextView) getChildAt(i);
            slot.setText(i < value.length() ? String.valueOf(value.charAt(i)) : " ");
            slot.setVisibility(i < value.length() ? VISIBLE : GONE);
        }
    }
}
