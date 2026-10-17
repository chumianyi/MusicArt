package com.musicart.player.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.musicart.player.MusicApp;
import com.musicart.player.media.Song;
import com.musicart.player.util.LrcParser;
import com.musicart.player.util.Theme;

import java.util.List;

/** 全屏播放页：自绘旋转封面、可拖进度条、逐行滚动歌词。 */
public class PlayerView extends FrameLayout implements MusicApp.PlayerListener {

    public interface Host {
        void onClosePlayer();
        void onOpenPlaylist();
    }

    private final Context ctx;
    private Host host;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float coverAngle = 0;
    private ValueAnimator spin;
    private long progressMs = 0, durationMs = 1;
    private boolean dragging = false;
    private boolean open = false;

    public void setPlayerOpen(boolean o){ this.open = o; }

    private List<LrcParser.Line> lrc;
    private float lrcScroll = 0;

    // hit rects
    private RectF backHit = new RectF(), listHit = new RectF();
    private RectF prevHit = new RectF(), playHit = new RectF(), nextHit = new RectF();
    private RectF progressHit = new RectF();

    public PlayerView(Context c) {
        super(c);
        this.ctx = c;
        setBackgroundColor(Color.TRANSPARENT);
        MusicApp.get().addListener(this);
        lrc = LrcParser.parse(null);
        startSpin();
    }

    public void setHost(Host h) { this.host = h; }

    private float dp(float v) { return Theme.dp(ctx, v); }

    private void startSpin() {
        spin = ValueAnimator.ofFloat(0, 360);
        spin.setDuration(24000);
        spin.setRepeatCount(ValueAnimator.INFINITE);
        spin.addUpdateListener(a -> {
            coverAngle = (float) a.getAnimatedValue();
            if (MusicApp.get().isPlaying()) invalidate();
        });
        spin.start();
    }

    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); MusicApp.get().addListener(this); }
    @Override protected void onDetachedFromWindow() { super.onDetachedFromWindow(); MusicApp.get().removeListener(this); }

    @Override public void onCurrentChanged(Song s) {
        lrc = LrcParser.parse(s == null ? null : s.lrc);
        invalidate();
    }
    @Override public void onProgress(long pos, long dur) {
        if (!dragging) { progressMs = pos; durationMs = Math.max(dur, 1); }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        float W = getWidth(), H = getHeight();
        // 渐变背景
        paint.setShader(new LinearGradient(0, 0, 0, H,
                Theme.BG_TOP, Theme.BG_BOTTOM, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, W, H, paint);
        paint.setShader(null);

        float pad = dp(22);
        float top = dp(46);

        // 顶栏
        backHit.set(pad, top, pad + dp(44), top + dp(44));
        drawCircleBtn(canvas, backHit, "‹");
        listHit.set(W - pad - dp(44), top, W - pad, top + dp(44));
        drawCircleBtn(canvas, listHit, "≡");

        Song s = MusicApp.get().current();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Theme.TEXT_DARK);
        paint.setTextSize(dp(16));
        paint.setFakeBoldText(true);
        canvas.drawText(s != null ? ell(s.displayTitle(), 14) : "未在播放", W/2, top + dp(29), paint);
        paint.setFakeBoldText(false);
        paint.setColor(Theme.TEXT_SUB);
        paint.setTextSize(dp(12));
        canvas.drawText(s != null ? s.displayArtist() : "", W/2, top + dp(48), paint);

        // 大唱片封面
        float disc = Math.min(W, H) * 0.52f;
        float cx = W/2, cy = top + dp(120) + disc/2;
        RectF discRect = new RectF(cx - disc/2, cy - disc/2, cx + disc/2, cy + disc/2);
        // 外圈阴影
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(dp(20), 0, dp(10), Theme.SHADOW);
        canvas.drawCircle(cx, cy, disc/2, paint);
        paint.clearShadowLayer();

        canvas.save();
        canvas.clipRect(discRect);
        canvas.translate(cx, cy);
        canvas.rotate(coverAngle);
        if (s != null && s.coverBitmap != null) {
            canvas.drawBitmap(s.coverBitmap, -disc/2, -disc/2, paint);
        } else {
            paint.setShader(new LinearGradient(-disc/2, -disc/2, disc/2, disc/2,
                    Theme.PRIMARY, Theme.PRIMARY2, Shader.TileMode.CLAMP));
            canvas.drawRect(-disc/2, -disc/2, disc/2, disc/2, paint);
            paint.setShader(null);
        }
        canvas.restore();
        // 中心孔
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, disc*0.09f, paint);
        paint.setColor(Theme.PRIMARY);
        canvas.drawCircle(cx, cy, disc*0.05f, paint);

        // 歌词区
        float lrcTop = discRect.bottom + dp(24);
        float lrcBottom = H - dp(150);
        drawLyrics(canvas, s, pad, lrcTop, W - pad, lrcBottom);

        // 进度条
        float py = H - dp(110);
        progressHit.set(pad, py - dp(16), W - pad, py + dp(16));
        float ratio = durationMs <= 0 ? 0 : Math.min(1f, (float) progressMs / durationMs);
        RectF track = new RectF(pad, py, W - pad, py + dp(4));
        paint.setColor(Theme.TRACK);
        canvas.drawRoundRect(track, dp(2), dp(2), paint);
        RectF fill = new RectF(track.left, track.top, track.left + (track.right-track.left)*ratio, track.bottom);
        paint.setShader(new LinearGradient(track.left,0,track.right,0,Theme.PRIMARY,Theme.PRIMARY2,Shader.TileMode.CLAMP));
        canvas.drawRoundRect(fill, dp(2), dp(2), paint);
        paint.setShader(null);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(fill.right, py + dp(2), dp(7), paint);
        paint.setColor(Theme.TEXT_SUB);
        paint.setTextSize(dp(11));
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(fmt(progressMs), pad, py + dp(24), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(fmt(durationMs), W - pad, py + dp(24), paint);

        // 控制按钮
        float cyy = H - dp(56);
        prevHit.set(W/2 - dp(90), cyy - dp(28), W/2 - dp(34), cyy + dp(28));
        nextHit.set(W/2 + dp(34), cyy - dp(28), W/2 + dp(90), cyy + dp(28));
        playHit.set(W/2 - dp(32), cyy - dp(32), W/2 + dp(32), cyy + dp(32));

        paint.setColor(Theme.TEXT_DARK);
        paint.setTextSize(dp(26));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("⏮", prevHit.centerX(), cyy + dp(9), paint);
        canvas.drawText("⏭", nextHit.centerX(), cyy + dp(9), paint);

        UITools.drawGradientPill(canvas, playHit, dp(32));
        paint.setColor(Color.WHITE);
        paint.setTextSize(dp(24));
        boolean playing = MusicApp.get().isPlaying();
        canvas.drawText(playing ? "⏸" : "▶", playHit.centerX(), cyy + dp(9), paint);
    }

    private void drawLyrics(Canvas c, Song s, float left, float top, float right, float bottom) {
        if (lrc == null) lrc = LrcParser.parse(null);
        // 找到当前行
        int active = 0;
        for (int i = 0; i < lrc.size(); i++) if (lrc.get(i).timeMs <= progressMs) active = i;
        float lineH = dp(34);
        float centerY = (top + bottom) / 2f;
        c.save();
        c.clipRect(left, top, right, bottom);
        for (int i = 0; i < lrc.size(); i++) {
            float y = centerY + (i - active) * lineH;
            if (y < top - lineH || y > bottom + lineH) continue;
            boolean isCur = (i == active);
            paint.setColor(isCur ? Theme.PRIMARY : Theme.TEXT_SUB);
            paint.setTextSize(isCur ? dp(17) : dp(14));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(isCur);
            String t = lrc.get(i).text;
            if (t != null && t.length() > 18) t = t.substring(0,18) + "…";
            c.drawText(t == null ? "" : t, (left+right)/2, y, paint);
            paint.setFakeBoldText(false);
        }
        c.restore();
    }

    private void drawCircleBtn(Canvas c, RectF r, String glyph) {
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(dp(6), 0, dp(3), Theme.SHADOW);
        c.drawRoundRect(r, dp(22), dp(22), paint);
        paint.clearShadowLayer();
        paint.setColor(Theme.TEXT_DARK);
        paint.setTextSize(dp(22));
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(glyph, r.centerX(), r.centerY() + dp(8), paint);
    }

    private String fmt(long ms) {
        long s = ms/1000;
        return String.format("%02d:%02d", s/60, s%60);
    }
    private String ell(String t, int n){ return t==null?"":(t.length()>n?t.substring(0,n)+"…":t); }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (progressHit.contains(x, y)) { dragging = true; seekByX(x); return true; }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) { seekByX(x); return true; }
                return true;
            case MotionEvent.ACTION_UP:
                dragging = false;
                if (backHit.contains(x, y)) { if (host!=null) host.onClosePlayer(); return true; }
                if (listHit.contains(x, y)) { if (host!=null) host.onOpenPlaylist(); return true; }
                if (prevHit.contains(x, y)) { MusicApp.get().prev(); return true; }
                if (nextHit.contains(x, y)) { MusicApp.get().next(); return true; }
                if (playHit.contains(x, y)) { MusicApp.get().toggle(); invalidate(); return true; }
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void seekByX(float x) {
        float pad = dp(22);
        float r = (x - pad) / (getWidth() - pad*2);
        r = Math.max(0, Math.min(1, r));
        progressMs = (long)(r * durationMs);
        MusicApp.get().seekTo(progressMs);
        invalidate();
    }
}
