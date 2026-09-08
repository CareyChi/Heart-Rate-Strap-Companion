package com.careychi.hrstrap.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    private static final int DEFAULT_BG = Color.rgb(18, 18, 18);
    private static final int DEFAULT_SURFACE = Color.rgb(30, 30, 30);
    private static final int DEFAULT_SURFACE_2 = Color.rgb(40, 40, 40);
    private static final int DEFAULT_BUTTON_BG = Color.rgb(19, 38, 33);
    private static final int DEFAULT_ICON_BUTTON_BG = Color.rgb(13, 27, 24);

    /** Runtime-selected background palette. PureBlackBackground configures these at startup. */
    public static int BG = DEFAULT_BG;
    public static int SURFACE = DEFAULT_SURFACE;
    public static int SURFACE_2 = DEFAULT_SURFACE_2;
    public static int BUTTON_BG = DEFAULT_BUTTON_BG;
    public static int ICON_BUTTON_BG = DEFAULT_ICON_BUTTON_BG;

    public static final int ACCENT = Color.rgb(72, 240, 164);
    public static final int TEXT = Color.rgb(241, 250, 246);
    public static final int MUTED = Color.rgb(157, 178, 170);

    private Ui() {}

    /** Switches every UI background role between pure black and the app's existing defaults. */
    public static synchronized void applyPureBlackBackground(boolean enabled) {
        if (enabled) {
            BG = Color.BLACK;
            SURFACE = Color.BLACK;
            SURFACE_2 = Color.BLACK;
            BUTTON_BG = Color.BLACK;
            ICON_BUTTON_BG = Color.BLACK;
        } else {
            BG = DEFAULT_BG;
            SURFACE = DEFAULT_SURFACE;
            SURFACE_2 = DEFAULT_SURFACE_2;
            BUTTON_BG = DEFAULT_BUTTON_BG;
            ICON_BUTTON_BG = DEFAULT_ICON_BUTTON_BG;
        }
    }

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static TextView text(Context c, String value, float sp, int color) {
        TextView v = new TextView(c);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setFontFeatureSettings("tnum");
        return v;
    }

    public static TextView title(Context c, String value) {
        TextView v = text(c, value, 26, TEXT);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    public static GradientDrawable rounded(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static void pad(View v, int allDp) {
        int p = dp(v.getContext(), allDp);
        v.setPadding(p, p, p, p);
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams weight(float value) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, value);
    }
}
