package com.musicart.player.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.musicart.player.MusicApp;
import com.musicart.player.media.Song;
import com.musicart.player.util.Theme;

import java.util.ArrayList;
import java.util.List;

/** 自绘 BottomSheet 播放列表。 */
public class PlaylistSheetView extends FrameLayout {

    public interface Host { void onPick(int idx); void onClose(); }

    private final Context ctx;
    private Host host;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float scrollY=0, downY=0, downScroll=0;
    private boolean dragging=false;
    private final List rowHits = new ArrayList();
    private RectF closeHit = new RectF();

    public PlaylistSheetView(Context c) { super(c); this.ctx=c; setBackgroundColor(Color.argb(160,20,16,40)); }
    public void setHost(Host h){ this.host=h; }
    private float dp(float v){ return Theme.dp(ctx,v); }

    @Override protected void onDraw(Canvas canvas) {
        float W=getWidth(), H=getHeight();
        List q = MusicApp.get().getQueue();
        int cur = MusicApp.get().getCurrentIndex();
        float sheetH = H*0.7f, top = H-sheetH;
        RectF sheet = new RectF(0,top,W,H);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(dp(16),0,-dp(6),Theme.SHADOW);
        canvas.drawRoundRect(sheet, dp(28), dp(28), paint);
        paint.clearShadowLayer();

        paint.setColor(Theme.TEXT_DARK); paint.setTextSize(dp(17)); paint.setFakeBoldText(true); paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("播放列表 · "+q.size()+" 首", dp(24), top+dp(40), paint);
        paint.setFakeBoldText(false);
        closeHit.set(W-dp(56), top+dp(16), W-dp(16), top+dp(46));
        paint.setColor(Theme.TEXT_SUB); paint.setTextSize(dp(18)); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("x", closeHit.centerX(), closeHit.centerY()+dp(6), paint);

        canvas.save();
        canvas.clipRect(0, top+dp(60), W, H);
        canvas.translate(0, top+dp(60)-scrollY);
        rowHits.clear();
        float y = 0;
        for (int i=0;i<q.size();i++) {
            Song s = (Song) q.get(i);
            RectF row = new RectF(dp(16), y, W-dp(16), y+dp(56));
            rowHits.add(row);
            boolean active = (i==cur);
            paint.setColor(active?Theme.BG_TOP:Color.TRANSPARENT);
            canvas.drawRoundRect(row, dp(14), dp(14), paint);
            paint.setColor(active?Theme.PRIMARY:Theme.TEXT_DARK);
            paint.setTextSize(dp(14)); paint.setTextAlign(Paint.Align.LEFT);
            String t = s.displayTitle();
            if (t.length()>18) t=t.substring(0,18)+"…";
            canvas.drawText((active?"♪ ":"")+t, row.left+dp(16), row.centerY()-dp(2), paint);
            paint.setColor(Theme.TEXT_SUB); paint.setTextSize(dp(11));
            String a = s.displayArtist();
            if (a.length()>16) a=a.substring(0,16)+"…";
            canvas.drawText(a, row.left+dp(16), row.centerY()+dp(16), paint);
            y += dp(64);
        }
        canvas.restore();
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=e.getX(), y=e.getY();
        float W=getWidth(), H=getHeight();
        float sheetTop = H - H*0.7f;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: downY=y; downScroll=scrollY; dragging=false;
                if (y < sheetTop) { if(host!=null)host.onClose(); return true; } return true;
            case MotionEvent.ACTION_MOVE:
                float dy = downY - y;
                if (Math.abs(dy)>dp(8)) dragging=true;
                if (dragging) { scrollY=Math.max(0,downScroll+dy); invalidate(); }
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragging) {
                    if (closeHit.contains(x,y)) { if(host!=null)host.onClose(); return true; }
                    for (int i=0;i<rowHits.size();i++) if (((RectF)rowHits.get(i)).contains(x,y)) { if(host!=null)host.onPick(i); return true; }
                }
                return true;
        }
        return true;
    }
}
