package com.musicart.player.view;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import com.musicart.player.util.Theme;

/** 绘制辅助。 */
public class UITools {

    public static Paint cardPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        return p;
    }

    public static void drawCard(Canvas c, RectF r, float radiusDp, float shadowDp, int color) {
        Paint p = cardPaint();
        p.setColor(color);
        p.setShadowLayer(shadowDp, 0, shadowDp * 0.6f, Theme.SHADOW);
        c.drawRoundRect(r, radiusDp, radiusDp, p);
        p.clearShadowLayer();
    }

    public static void drawGradientPill(Canvas c, RectF r, float radiusDp) {
        Paint p = cardPaint();
        p.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom,
                Theme.PRIMARY, Theme.PRIMARY2, Shader.TileMode.CLAMP));
        p.setShadowLayer(10, 0, 4, Theme.SHADOW);
        c.drawRoundRect(r, radiusDp, radiusDp, p);
        p.clearShadowLayer();
    }

    public static void text(Canvas c, String s, float x, float y, float sizeDp, int color, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(sizeDp);
        p.setTextAlign(align);
        if (bold) p.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText(s, x, y, p);
    }
}
