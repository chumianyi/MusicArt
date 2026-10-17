package com.musicart.player.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.musicart.player.MusicApp;
import com.musicart.player.media.Song;
import com.musicart.player.util.LrcParser;
import com.musicart.player.util.Theme;

import java.util.List;

/** 全屏播放页。 */
public class PlayerView extends FrameLayout implements MusicApp.PlayerListener {

    public interface Host { void onClosePlayer(); void onOpenPlaylist(); }

    private final Context ctx;
    private Host host;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler h = new Handler();
    private float coverAngle = 0;
    private final Runnable spin = new Runnable() { public void run() {
        coverAngle = (coverAngle + 1.2f) % 360;
        if (MusicApp.get().isPlaying()) invalidate();
        h.postDelayed(this, 40);
    }};

    private long progressMs = 0, durationMs = 1;
    private boolean dragging = false;
    private List lrc;

    private RectF backHit = new RectF(), listHit = new RectF();
    private RectF prevHit = new RectF(), playHit = new RectF(), nextHit = new RectF();
    private RectF progressHit = new RectF();

    public PlayerView(Context c) {
        super(c); this.ctx = c;
        setBackgroundColor(Color.TRANSPARENT);
        lrc = LrcParser.parse(null);
        h.post(spin);
    }
    public void setHost(Host hh) { this.host = hh; }
    private float dp(float v) { return Theme.dp(ctx, v); }

    public void onCurrentChanged(Song s) { lrc = LrcParser.parse(s==null?null:s.lrc); invalidate(); }
    public void onProgress(long pos, long dur) { if (!dragging) { progressMs=pos; durationMs=Math.max(dur,1); } invalidate(); }
    public void onQueueChanged() {}
    public void onPlayStateChanged(boolean p) {}

    @Override protected void onDraw(Canvas canvas) {
        float W = getWidth(), H = getHeight();
        paint.setShader(new LinearGradient(0,0,0,H, Theme.BG_TOP, Theme.BG_BOTTOM, Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,W,H,paint);
        paint.setShader(null);

        float pad = dp(22), top = dp(46);
        backHit.set(pad, top, pad+dp(44), top+dp(44));
        circleBtn(canvas, backHit, "<");
        listHit.set(W-pad-dp(44), top, W-pad, top+dp(44));
        circleBtn(canvas, listHit, "=");

        Song s = MusicApp.get().current();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Theme.TEXT_DARK); paint.setTextSize(dp(16)); paint.setFakeBoldText(true);
        canvas.drawText(s!=null?ell(s.displayTitle(),14):"未在播放", W/2, top+dp(29), paint);
        paint.setFakeBoldText(false); paint.setColor(Theme.TEXT_SUB); paint.setTextSize(dp(12));
        canvas.drawText(s!=null?s.displayArtist():"", W/2, top+dp(48), paint);

        float disc = Math.min(W,H)*0.52f;
        float cx = W/2, cy = top+dp(120)+disc/2;
        RectF discRect = new RectF(cx-disc/2, cy-disc/2, cx+disc/2, cy+disc/2);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(dp(20), 0, dp(10), Theme.SHADOW);
        canvas.drawCircle(cx, cy, disc/2, paint);
        paint.clearShadowLayer();
        canvas.save();
        canvas.clipRect(discRect);
        canvas.translate(cx, cy); canvas.rotate(coverAngle);
        if (s!=null && s.coverBitmap!=null) {
            canvas.drawBitmap(s.coverBitmap, -disc/2, -disc/2, paint);
        } else {
            paint.setShader(new LinearGradient(-disc/2,-disc/2,disc/2,disc/2,
                    Theme.PRIMARY, Theme.PRIMARY2, Shader.TileMode.CLAMP));
            canvas.drawRect(-disc/2,-disc/2,disc/2,disc/2,paint);
            paint.setShader(null);
        }
        canvas.restore();
        paint.setColor(Color.WHITE); canvas.drawCircle(cx, cy, disc*0.09f, paint);
        paint.setColor(Theme.PRIMARY); canvas.drawCircle(cx, cy, disc*0.05f, paint);

        float lrcTop = discRect.bottom + dp(24);
        float lrcBottom = H - dp(150);
        drawLyrics(canvas, lrcTop, lrcBottom);

        float py = H - dp(110);
        progressHit.set(pad, py-dp(16), W-pad, py+dp(16));
        float ratio = durationMs<=0?0:Math.min(1f,(float)progressMs/durationMs);
        RectF track = new RectF(pad, py, W-pad, py+dp(4));
        paint.setColor(Theme.TRACK); canvas.drawRoundRect(track, dp(2), dp(2), paint);
        RectF fill = new RectF(track.left, track.top, track.left+(track.right-track.left)*ratio, track.bottom);
        paint.setShader(new LinearGradient(track.left,0,track.right,0,Theme.PRIMARY,Theme.PRIMARY2,Shader.TileMode.CLAMP));
        canvas.drawRoundRect(fill, dp(2), dp(2), paint);
        paint.setShader(null);
        paint.setColor(Color.WHITE); canvas.drawCircle(fill.right, py+dp(2), dp(7), paint);
        paint.setColor(Theme.TEXT_SUB); paint.setTextSize(dp(11)); paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(fmt(progressMs), pad, py+dp(24), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(fmt(durationMs), W-pad, py+dp(24), paint);

        float cyy = H - dp(56);
        prevHit.set(W/2-dp(90), cyy-dp(28), W/2-dp(34), cyy+dp(28));
        nextHit.set(W/2+dp(34), cyy-dp(28), W/2+dp(90), cyy+dp(28));
        playHit.set(W/2-dp(32), cyy-dp(32), W/2+dp(32), cyy+dp(32));
        paint.setColor(Theme.TEXT_DARK); paint.setTextSize(dp(22)); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("|◁", prevHit.centerX(), cyy+dp(8), paint);
        canvas.drawText("▷|", nextHit.centerX(), cyy+dp(8), paint);
        UITools.drawGradientPill(canvas, playHit, dp(32));
        paint.setColor(Color.WHITE); paint.setTextSize(dp(18));
        canvas.drawText(MusicApp.get().isPlaying() ? "||" : ">", playHit.centerX(), cyy+dp(7), paint);
    }

    private void drawLyrics(Canvas c, float top, float bottom) {
        if (lrc==null) lrc = LrcParser.parse(null);
        int active = 0;
        for (int i=0;i<lrc.size();i++) if (((LrcParser.Line)lrc.get(i)).timeMs <= progressMs) active = i;
        float lineH = dp(34);
        float centerY = (top+bottom)/2f;
        c.save(); c.clipRect(dp(20), top, getWidth()-dp(20), bottom);
        for (int i=0;i<lrc.size();i++) {
            float y = centerY + (i-active)*lineH;
            if (y < top-lineH || y > bottom+lineH) continue;
            LrcParser.Line ln = (LrcParser.Line) lrc.get(i);
            boolean cur = (i==active);
            paint.setColor(cur?Theme.PRIMARY:Theme.TEXT_SUB);
            paint.setTextSize(cur?dp(17):dp(14));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(cur);
            String t = ln.text;
            if (t!=null && t.length()>18) t = t.substring(0,18)+"…";
            c.drawText(t==null?"":t, getWidth()/2, y, paint);
            paint.setFakeBoldText(false);
        }
        c.restore();
    }

    private void circleBtn(Canvas c, RectF r, String g) {
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(dp(6),0,dp(3),Theme.SHADOW);
        c.drawRoundRect(r, dp(22), dp(22), paint);
        paint.clearShadowLayer();
        paint.setColor(Theme.TEXT_DARK); paint.setTextSize(dp(20)); paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(g, r.centerX(), r.centerY()+dp(7), paint);
    }

    private String fmt(long ms){ long s=ms/1000; return ""+(s/60)+":"+(s%60<10?"0":"")+(s%60); }
    private String ell(String t,int n){ return t==null?"":(t.length()>n?t.substring(0,n)+"…":t); }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=e.getX(), y=e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (progressHit.contains(x,y)) { dragging=true; seek(x); return true; }
                return true;
            case MotionEvent.ACTION_MOVE: if (dragging) { seek(x); return true; } return true;
            case MotionEvent.ACTION_UP:
                dragging = false;
                if (backHit.contains(x,y)) { if(host!=null)host.onClosePlayer(); return true; }
                if (listHit.contains(x,y)) { if(host!=null)host.onOpenPlaylist(); return true; }
                if (prevHit.contains(x,y)) { MusicApp.get().prev(); return true; }
                if (nextHit.contains(x,y)) { MusicApp.get().next(); return true; }
                if (playHit.contains(x,y)) { MusicApp.get().toggle(); invalidate(); return true; }
                return true;
        }
        return super.onTouchEvent(e);
    }
    private void seek(float x) {
        float pad=dp(22);
        float r = (x-pad)/(getWidth()-pad*2);
        r = Math.max(0, Math.min(1, r));
        progressMs = (long)(r*durationMs);
        MusicApp.get().seekTo(progressMs);
        invalidate();
    }
}
