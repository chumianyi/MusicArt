package com.musicart.player.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.musicart.player.MusicApp;
import com.musicart.player.media.Song;
import com.musicart.player.net.Api;
import com.musicart.player.util.ImageLoader;
import com.musicart.player.util.Theme;

import java.util.ArrayList;
import java.util.List;

/** 主界面根 View：全部自绘。包含 推荐/搜索 内容流、迷你播放条、Toast。 */
public class MainView extends FrameLayout {

    public interface Host {
        void openPlayer();
        void openPlaylist();
        void openSearch();
        void showToast(String msg);
    }

    private final Context ctx;
    private Host host;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // 数据
    private List<Song> results = new ArrayList<>();
    private List<Song> recommend = new ArrayList<>();
    private String keyword = "";
    private boolean searching = false;

    // 滚动
    private float scrollY = 0, touchStartY = 0, touchStartScroll = 0;
    private boolean dragging = false;

    // 命中区
    private final List<RectF> rowHits = new ArrayList<>();
    private final List<RectF> chipHits = new ArrayList<>();
    private RectF miniPlayerHit = new RectF();
    private RectF searchPillHit = new RectF();
    private RectF playlistBtnHit = new RectF();
    private RectF playBtnHit = new RectF();
    private RectF backBtnHit = new RectF();
    private boolean playerOpen = false;

    private static final String[] HOT = {"晴天", "周杰伦", "起风了", "Lemon", "稻香",
            "夜曲", "海阔天空", "好想爱这个世界啊", "孤勇者", "一路向北"};

    public MainView(Context c) {
        super(c);
        this.ctx = c;
        setBackgroundColor(Color.TRANSPARENT);
        setFocusable(true);
        seedFeatured();
        // 在线推荐失败时使用本地精选封面兜底
        Api.fetchRecommend((list, err) -> {
            if (list != null && !list.isEmpty()) recommend = list;
            invalidate();
        });
    }

    /** 本地精选：用打包插画做封面，点按即搜即播。 */
    private void seedFeatured() {
        String[][] data = {
            {"cover1.png", "云端耳机", "晴天 周杰伦"},
            {"cover6.png", "森林音符", "起风了"},
            {"cover7.png", "城市唱片", "海阔天空 Beyond"},
            {"cover8.png", "钢琴河流", "稻香 周杰伦"},
            {"cover9.png", "舞台麦克风", "孤勇者"},
            {"cover10.png", "泡泡音波", "夜曲 周杰伦"},
            {"cover2.png", "声波波浪", "一路向北"},
            {"cover3.png", "键盘星河", "好想爱这个世界啊"},
            {"cover4.png", "聚光舞台", "Lemon 米津玄师"},
            {"cover5.png", "黑胶云端", "夜空中最亮的星"}
        };
        for (String[] d : data) {
            Song s = new Song();
            s.name = d[1]; s.artist = d[2]; s.album = "编辑精选";
            s.coverUrl = "asset://" + d[0];
            recommend.add(s);
        }
    }

    public void setHost(Host h) { this.host = h; }
    public void setPlayerOpen(boolean open) { this.playerOpen = open; }

    private float dp(float v) { return Theme.dp(ctx, v); }

    @Override protected void onDraw(Canvas canvas) {
        float W = getWidth(), H = getHeight();
        // 渐变背景
        paint.setShader(new LinearGradient(0, 0, W, H, Theme.BG_TOP, Theme.BG_BOTTOM, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, W, H, paint);
        paint.setShader(null);

        float pad = dp(20);
        float top = dp(44);

        // 头部
        paint.setColor(Theme.TEXT_DARK);
        paint.setTextSize(dp(26));
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("画音", pad, top, paint);
        paint.setTextSize(dp(13));
        paint.setColor(Theme.TEXT_SUB);
        paint.setFakeBoldText(false);
        canvas.drawText("MusicArt · 手绘音乐", pad, top + dp(22), paint);

        // 搜索胶囊
        searchPillHit.set(pad, top + dp(34), W - pad, top + dp(34) + dp(48));
        UITools.drawCard(canvas, searchPillHit, dp(24), dp(8), Color.WHITE);
        paint.setColor(Theme.TEXT_SUB);
        paint.setTextSize(dp(15));
        paint.setTextAlign(Paint.Align.LEFT);
        String q = keyword.isEmpty() ? "  搜索歌曲、歌手…" : "  " + keyword;
        canvas.drawText(q, searchPillHit.left + dp(16), searchPillHit.centerY() + dp(5), paint);
        // 搜索图标
        paint.setColor(Theme.PRIMARY);
        canvas.drawCircle(searchPillHit.right - dp(28), searchPillHit.centerY(), dp(9), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        canvas.drawLine(searchPillHit.right - dp(21), searchPillHit.centerY() + dp(7),
                searchPillHit.right - dp(15), searchPillHit.centerY() + dp(13), paint);
        paint.setStyle(Paint.Style.FILL);

        // 内容（可滚动）
        canvas.save();
        canvas.clipRect(0, searchPillHit.bottom + dp(16), W, H - dp(140));
        canvas.translate(0, -scrollY);

        float cy = searchPillHit.bottom + dp(28);
        rowHits.clear();
        chipHits.clear();

        if (!searching && results.isEmpty()) {
            // 推荐 / 热门搜索
            paint.setColor(Theme.TEXT_DARK);
            paint.setTextSize(dp(18));
            paint.setFakeBoldText(true);
            canvas.drawText("热门搜索", pad, cy, paint);
            paint.setFakeBoldText(false);
            cy += dp(18);
            float cx = pad;
            float cyy = cy;
            for (int i = 0; i < HOT.length; i++) {
                String s = HOT[i];
                float w = measure(s, dp(13)) + dp(28);
                if (cx + w > W - pad) { cx = pad; cyy += dp(46); }
                RectF chip = new RectF(cx, cyy, cx + w, cyy + dp(38));
                UITools.drawCard(canvas, chip, dp(19), dp(4), Color.WHITE);
                paint.setColor(Theme.TEXT_SUB);
                paint.setTextSize(dp(13));
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(s, cx + dp(14), cyy + dp(25), paint);
                chipHits.add(chip);
                cx += w + dp(12);
            }
            cy = cyy + dp(60);

            // 推荐标题
            paint.setColor(Theme.TEXT_DARK);
            paint.setTextSize(dp(18));
            paint.setFakeBoldText(true);
            canvas.drawText("为你推荐", pad, cy, paint);
            paint.setFakeBoldText(false);
            cy += dp(24);

            if (recommend.isEmpty()) {
                paint.setColor(Theme.TEXT_SUB);
                paint.setTextSize(dp(13));
                canvas.drawText("正在拉取在线推荐…（也可点击上方热门搜索）", pad, cy, paint);
            } else {
                for (Song s : recommend) {
                    drawSongRow(canvas, s, pad, cy, W - pad, dp(64));
                    cy += dp(78);
                }
            }
        } else {
            // 搜索结果
            paint.setColor(Theme.TEXT_DARK);
            paint.setTextSize(dp(18));
            paint.setFakeBoldText(true);
            canvas.drawText(searching ? "搜索中…" : ("搜索结果 · " + results.size()), pad, cy, paint);
            paint.setFakeBoldText(false);
            cy += dp(20);
            if (results.isEmpty() && !searching) {
                paint.setColor(Theme.TEXT_SUB);
                paint.setTextSize(dp(13));
                canvas.drawText("没有找到相关歌曲，换个词试试", pad, cy, paint);
            }
            for (Song s : results) {
                drawSongRow(canvas, s, pad, cy, W - pad, dp(64));
                cy += dp(78);
            }
        }
        canvas.restore();

        // 迷你播放条
        drawMiniPlayer(canvas, W, H);
        // 底部安全区
    }

    private void drawSongRow(Canvas canvas, Song s, float left, float top, float right, float h) {
        RectF card = new RectF(left, top, right, top + h);
        UITools.drawCard(canvas, card, dp(18), dp(6), Color.WHITE);
        rowHits.add(card);

        // 封面
        RectF cover = new RectF(left + dp(10), top + dp(10), left + dp(10) + h - dp(20), top + h - dp(10));
        drawCoverThumb(canvas, s, cover);

        // 标题 / 歌手
        paint.setColor(Theme.TEXT_DARK);
        paint.setTextSize(dp(15));
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(true);
        canvas.drawText(ellipsize(s.displayTitle(), 18), cover.right + dp(14), top + dp(28), paint);
        paint.setFakeBoldText(false);
        paint.setColor(Theme.TEXT_SUB);
        paint.setTextSize(dp(12));
        canvas.drawText(ellipsize(s.displayArtist() + " · " + s.album, 22), cover.right + dp(14), top + dp(48), paint);

        // 右侧播放三角
        paint.setColor(Theme.PRIMARY);
        RectF cir = new RectF(right - dp(44), top + h/2 - dp(16), right - dp(12), top + h/2 + dp(16));
        canvas.drawRoundRect(cir, dp(16), dp(16), paint);
        paint.setColor(Color.WHITE);
        canvas.drawPath(buildTriangle(cir.centerX(), cir.centerY()), paint);
    }

    private android.graphics.Path buildTriangle(float cx, float cy) {
        android.graphics.Path p = new android.graphics.Path();
        p.moveTo(cx - dp(4), cy - dp(7));
        p.lineTo(cx - dp(4), cy + dp(7));
        p.lineTo(cx + dp(7), cy);
        p.close();
        return p;
    }

    private void drawCoverThumb(Canvas canvas, Song s, RectF r) {
        canvas.save();
        canvas.clipRect(r);
        if (s.coverBitmap != null) {
            canvas.drawBitmap(s.coverBitmap, null, r, paint);
        } else {
            paint.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom,
                    Theme.PRIMARY, Theme.PRIMARY2, Shader.TileMode.CLAMP));
            canvas.drawRect(r, paint);
            paint.setShader(null);
            paint.setColor(Color.WHITE);
            paint.setTextSize(dp(20));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("♪", r.centerX(), r.centerY() + dp(7), paint);
            // 异步加载
            if (s.coverUrl != null && !s.coverUrl.isEmpty()) {
                ImageLoader.get().load(s.coverUrl, 200, b -> { s.coverBitmap = b; invalidate(); });
            }
        }
        canvas.restore();
    }

    private void drawMiniPlayer(Canvas canvas, float W, float H) {
        float mh = dp(64);
        RectF bar = new RectF(dp(14), H - mh - dp(20), W - dp(14), H - dp(20));
        miniPlayerHit = bar;
        Song cur = MusicApp.get().current();
        if (cur == null) return; // 无播放不显示

        UITools.drawCard(canvas, bar, dp(20), dp(10), Color.WHITE);
        RectF cover = new RectF(bar.left + dp(8), bar.top + dp(8), bar.left + dp(8) + mh - dp(16), bar.bottom - dp(8));
        drawCoverThumb(canvas, cur, cover);

        paint.setColor(Theme.TEXT_DARK);
        paint.setTextSize(dp(14));
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(ellipsize(cur.displayTitle(), 16), cover.right + dp(12), bar.centerY() - dp(2), paint);
        paint.setFakeBoldText(false);
        paint.setColor(Theme.TEXT_SUB);
        paint.setTextSize(dp(11));
        canvas.drawText(ellipsize(cur.displayArtist(), 18), cover.right + dp(12), bar.centerY() + dp(16), paint);

        // 播放/暂停按钮
        boolean playing = MusicApp.get().isPlaying();
        playBtnHit = new RectF(bar.right - dp(56), bar.centerY() - dp(18), bar.right - dp(20), bar.centerY() + dp(18));
        paint.setShader(new LinearGradient(playBtnHit.left, playBtnHit.top, playBtnHit.right, playBtnHit.bottom,
                Theme.PRIMARY, Theme.PRIMARY2, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(playBtnHit, dp(18), dp(18), paint);
        paint.setShader(null);
        paint.setColor(Color.WHITE);
        if (playing) {
            canvas.drawRect(playBtnHit.centerX() - dp(6), playBtnHit.centerY() - dp(8),
                    playBtnHit.centerX() - dp(1), playBtnHit.centerY() + dp(8), paint);
            canvas.drawRect(playBtnHit.centerX() + dp(1), playBtnHit.centerY() - dp(8),
                    playBtnHit.centerX() + dp(6), playBtnHit.centerY() + dp(8), paint);
        } else {
            canvas.drawPath(buildTriangle(playBtnHit.centerX() + dp(1), playBtnHit.centerY()), paint);
        }
    }

    private float measure(String s, float sizeDp) {
        Paint p = new Paint(); p.setTextSize(sizeDp); return p.measureText(s);
    }
    private String ellipsize(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ---------------- 触摸 ----------------
    @Override public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartY = y; touchStartScroll = scrollY; dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dy = touchStartY - y;
                if (Math.abs(dy) > dp(10)) dragging = true;
                if (dragging) {
                    scrollY = Math.max(0, touchStartScroll + dy);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragging) handleTap(x, y);
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void handleTap(float x, float y) {
        if (playerOpen) return; // 全屏播放页自己处理
        // 迷你条
        if (miniPlayerHit.contains(x, y)) {
            if (playBtnHit.contains(x, y)) { MusicApp.get().toggle(); invalidate(); return; }
            if (host != null) host.openPlayer();
            return;
        }
        if (searchPillHit.contains(x, y)) { if (host != null) host.openSearch(); return; }
        for (RectF chip : chipHits) {
            if (chip.contains(x, y)) {
                int idx = chipHits.indexOf(chip);
                doSearch(HOT[idx % HOT.length]);
                return;
            }
        }
        for (RectF row : rowHits) {
            if (row.contains(x, y)) {
                int idx = rowHits.indexOf(row);
                if (!results.isEmpty()) {
                    MusicApp.get().playQueue(results, idx);
                    if (host != null) host.openPlayer();
                } else if (idx < recommend.size()) {
                    // 本地精选：用 artist 字段作为搜索词，即搜即播
                    Song f = recommend.get(idx);
                    MusicApp.get().searchAndPlay(f.artist);
                    if (host != null) host.openPlayer();
                }
                return;
            }
        }
    }

    public void doSearch(String kw) {
        keyword = kw;
        searching = true;
        scrollY = 0;
        invalidate();
        Api.search(kw, 0, 30, (list, err) -> {
            searching = false;
            results = list;
            invalidate();
            if (host != null && (list == null || list.isEmpty())) host.showToast("没有搜到「" + kw + "」");
        });
    }
}
