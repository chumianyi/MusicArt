package com.musicart.player.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import com.musicart.player.R;
import com.musicart.player.util.ImageLoader;
import com.musicart.player.util.Theme;

/** 圆角封面：自绘阴影 + 圆角裁剪 + 渐变占位。 */
public class RoundedCoverView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float radius;
    private String url;
    private Bitmap bmp;
    private float rotation;

    public RoundedCoverView(Context c) { super(c); radius = Theme.dp(c, 20); }

    public void setRadiusDp(float dp) { radius = Theme.dp(getContext(), dp); invalidate(); }

    public void setUrl(String u) {
        this.url = u;
        this.bmp = null;
        invalidate();
        if (u != null && !u.isEmpty()) {
            ImageLoader.get().load(u, 480, b -> { bmp = b; invalidate(); });
        }
    }

    public void setRotation(float deg) { rotation = deg; invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        rect.set(0, 0, w, h);

        // 柔和阴影
        paint.setShadowLayer(Theme.dp(getContext(), 12), 0, Theme.dp(getContext(), 6), Theme.SHADOW);
        paint.setColor(Theme.CARD);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.clearShadowLayer();

        canvas.save();
        clipRound(canvas);
        if (bmp != null) {
            canvas.save();
            canvas.translate(w/2, h/2);
            canvas.rotate(rotation);
            float side = Math.min(w, h);
            canvas.drawBitmap(bmp, null, new RectF(-side/2, -side/2, side/2, side/2), paint);
            canvas.restore();
        } else {
            // 渐变占位
            LinearGradient g = new LinearGradient(0, 0, w, h, Theme.PRIMARY, Theme.PRIMARY2, Shader.TileMode.CLAMP);
            paint.setShader(g);
            canvas.drawRect(rect, paint);
            paint.setShader(null);
            // 音符符号
            paint.setColor(Color.argb(90,255,255,255));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(Theme.dp(getContext(), 40));
            canvas.drawText("♪", w/2, h/2 + Theme.dp(getContext(),14), paint);
        }
        canvas.restore();
    }

    private void clipRound(Canvas canvas) {
        Path p = new Path();
        p.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.clipPath(p);
    }
}
