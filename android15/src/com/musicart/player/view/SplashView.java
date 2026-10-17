package com.musicart.player.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.view.View;

import com.musicart.player.util.Theme;

/** 开屏：渐变 + 旋转唱片 + 呼吸光晕。 */
public class SplashView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler h = new Handler();
    private long start = System.currentTimeMillis();
    private static final long DUR = 2200;
    private final Runnable loop = new Runnable() { public void run() { invalidate(); h.postDelayed(this, 30); } };

    public SplashView(Context c) {
        super(c);
        h.post(loop);
    }

    private float dp(float v) { return Theme.dp(getContext(), v); }

    @Override protected void onDraw(Canvas canvas) {
        float W = getWidth(), H = getHeight();
        float t = Math.min(1f, (System.currentTimeMillis() - start) / (float) DUR);

        paint.setShader(new LinearGradient(0, 0, 0, H, 0xF6F3FF, 0xFFE9F2, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, W, H, paint);
        paint.setShader(null);

        float cx = W/2, cy = H/2f - H*0.05f;
        float R = Math.min(W, H) * 0.16f;
        float pulse = (float) Math.sin(t * Math.PI * 2);

        paint.setColor(Color.argb(40 + (int)(30*pulse), 142, 124, 255));
        canvas.drawCircle(cx, cy, R * (1.5f + 0.15f*pulse), paint);

        paint.setColor(Color.WHITE);
        paint.setShadowLayer(20, 0, 10, 0x338E7CFF);
        canvas.drawCircle(cx, cy, R, paint);
        paint.clearShadowLayer();

        paint.setColor(Color.parseColor("#EDE9FF"));
        canvas.drawCircle(cx, cy, R*0.78f, paint);
        paint.setColor(Color.parseColor("#DED6FF"));
        canvas.drawCircle(cx, cy, R*0.6f, paint);

        canvas.save();
        canvas.rotate(t*360, cx, cy);
        paint.setShader(new LinearGradient(cx-R, cy-R, cx+R, cy+R,
                Color.parseColor("#8E7CFF"), Color.parseColor("#FF9AC5"), Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, R*0.45f, paint);
        paint.setShader(null);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, R*0.1f, paint);
        canvas.restore();

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(R*0.5f);
        paint.setFakeBoldText(true);
        canvas.drawText("♪", cx, cy + R*0.18f, paint);

        paint.setColor(0xFF2B2740);
        paint.setTextSize(R*0.42f);
        canvas.drawText("画音 MusicArt", cx, cy + R*1.7f, paint);
        paint.setTextSize(R*0.2f);
        paint.setColor(0xFF9A94B8);
        canvas.drawText("用手画出来的音乐", cx, cy + R*2.1f, paint);

        float bw = W*0.4f;
        RectF track = new RectF(cx-bw/2, H*0.82f, cx+bw/2, H*0.82f+dp(4));
        paint.setColor(0xFFECE8F7);
        canvas.drawRoundRect(track, dp(2), dp(2), paint);
        RectF fill = new RectF(track.left, track.top, track.left + bw*t, track.bottom);
        paint.setShader(new LinearGradient(track.left,0,track.right,0,
                Color.parseColor("#8E7CFF"), Color.parseColor("#FF9AC5"), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(fill, dp(2), dp(2), paint);
        paint.setShader(null);
    }
}
