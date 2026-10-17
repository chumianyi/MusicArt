package com.musicart.player.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/** 开屏：渐变底 + 自绘旋转唱片 logo + 呼吸光晕 + 应用名。 */
public class SplashView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress = 0f;      // 0..1 动画进度
    private float pulse = 0f;
    private ValueAnimator anim;

    public SplashView(Context c) {
        super(c);
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(2200);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> { progress = (float) a.getAnimatedValue(); invalidate(); });
        anim.start();
    }

    public void stop() { if (anim != null) anim.cancel(); }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        // 渐变背景
        LinearGradient bg = new LinearGradient(0, 0, 0, h,
                Theme_BG(), Theme_END(), Shader.TileMode.CLAMP);
        paint.setShader(bg);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        float cx = w / 2, cy = h / 2f - h * 0.05f;
        float R = Math.min(w, h) * 0.16f;

        // 呼吸光晕
        pulse = (float) Math.sin(progress * Math.PI * 2);
        paint.setColor(Color.argb((int)(40 + 30 * pulse), 142, 124, 255));
        canvas.drawCircle(cx, cy, R * (1.5f + 0.15f * pulse), paint);

        // 唱片外圈
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(20, 0, 10, 0x338E7CFF);
        canvas.drawCircle(cx, cy, R, paint);
        paint.clearShadowLayer();

        // 唱片纹路
        paint.setColor(Color.parseColor("#EDE9FF"));
        canvas.drawCircle(cx, cy, R * 0.78f, paint);
        paint.setColor(Color.parseColor("#DED6FF"));
        canvas.drawCircle(cx, cy, R * 0.6f, paint);

        // 旋转内圈
        canvas.save();
        canvas.rotate(progress * 360, cx, cy);
        LinearGradient disc = new LinearGradient(cx-R, cy-R, cx+R, cy+R,
                Color.parseColor("#8E7CFF"), Color.parseColor("#FF9AC5"), Shader.TileMode.CLAMP);
        paint.setShader(disc);
        canvas.drawCircle(cx, cy, R * 0.45f, paint);
        paint.setShader(null);
        // 中心孔
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, R * 0.1f, paint);
        canvas.restore();

        // 音符符号
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(R * 0.5f);
        paint.setFakeBoldText(true);
        canvas.drawText("♪", cx, cy + R*0.18f, paint);

        // 应用名
        paint.setColor(Color.parseColor("#2B2740"));
        paint.setTextSize(R * 0.42f);
        paint.setFakeBoldText(true);
        paint.setShadowLayer(0,0,0,0);
        canvas.drawText("画音 MusicArt", cx, cy + R * 1.7f, paint);
        paint.setTextSize(R * 0.2f);
        paint.setColor(Color.parseColor("#9A94B8"));
        canvas.drawText("用手画出来的音乐", cx, cy + R * 2.1f, paint);

        // 底部进度条
        float bw = w * 0.4f;
        RectF track = new RectF(cx - bw/2, h * 0.82f, cx + bw/2, h * 0.82f + dp(4));
        paint.setColor(Color.parseColor("#ECE8F7"));
        canvas.drawRoundRect(track, dp(2), dp(2), paint);
        RectF fill = new RectF(track.left, track.top, track.left + bw * progress, track.bottom);
        paint.setShader(new LinearGradient(track.left, 0, track.right, 0,
                Color.parseColor("#8E7CFF"), Color.parseColor("#FF9AC5"), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(fill, dp(2), dp(2), paint);
        paint.setShader(null);
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private int Theme_BG() { return Color.parseColor("#F6F3FF"); }
    private int Theme_END() { return Color.parseColor("#FFE9F2"); }
}
