package com.musicart.player.util;

import android.content.Context;
import android.graphics.Color;

/** 全局配色与尺寸工具：柔和圆角渐变风格。 */
public class Theme {
    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    // 主配色：奶油底 + 薰衣草紫 + 蜜桃粉
    public static final int BG_TOP    = Color.parseColor("#F6F3FF");
    public static final int BG_BOTTOM = Color.parseColor("#FFF1F5");
    public static final int CARD      = Color.parseColor("#FFFFFF");
    public static final int PRIMARY   = Color.parseColor("#8E7CFF");
    public static final int PRIMARY2  = Color.parseColor("#FF9AC5");
    public static final int TEXT_DARK = Color.parseColor("#2B2740");
    public static final int TEXT_SUB  = Color.parseColor("#9A94B8");
    public static final int TRACK     = Color.parseColor("#ECE8F7");
    public static final int SHADOW    = Color.parseColor("#2230204A");

    public static int mix(int a, int b, float t) {
        int ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a);
        int br = Color.red(b), bg = Color.green(b), bb = Color.blue(b);
        return Color.rgb((int)(ar+(br-ar)*t), (int)(ag+(bg-ag)*t), (int)(ab+(bb-ab)*t));
    }
}
