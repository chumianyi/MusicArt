package com.musicart.player.util;

import android.content.Context;
import android.graphics.Color;

/** 配色。 */
public class Theme {
    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
    public static final int BG_TOP    = Color.parseColor("#F6F3FF");
    public static final int BG_BOTTOM= Color.parseColor("#FFF1F5");
    public static final int CARD      = Color.parseColor("#FFFFFF");
    public static final int PRIMARY   = Color.parseColor("#8E7CFF");
    public static final int PRIMARY2  = Color.parseColor("#FF9AC5");
    public static final int TEXT_DARK = Color.parseColor("#2B2740");
    public static final int TEXT_SUB  = Color.parseColor("#9A94B8");
    public static final int TRACK     = Color.parseColor("#ECE8F7");
    public static final int SHADOW    = Color.parseColor("#2230204A");
}
