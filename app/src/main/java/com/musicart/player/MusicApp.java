package com.musicart.player;

import android.app.Application;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.musicart.player.media.Song;
import com.musicart.player.net.Api;

import java.util.ArrayList;
import java.util.List;

/** 全局播放器：持有 MediaPlayer、播放队列、当前状态。 */
public class MusicApp extends Application {

    private static MusicApp INSTANCE;
    public static MusicApp get() { return INSTANCE; }

    public interface PlayerListener {
        default void onQueueChanged() {}
        default void onCurrentChanged(Song s) {}
        default void onPlayStateChanged(boolean playing) {}
        default void onProgress(long pos, long dur) {}
    }

    private final List<PlayerListener> listeners = new ArrayList<>();
    private final List<Song> queue = new ArrayList<>();
    private int current = -1;

    private MediaPlayer player;
    private boolean prepared = false;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Handler ticker = new Handler(Looper.getMainLooper());

    @Override public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        com.musicart.player.util.ImageLoader.init(this);
    }

    public void addListener(PlayerListener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(PlayerListener l) { listeners.remove(l); }

    public List<Song> getQueue() { return queue; }
    public int getCurrentIndex() { return current; }
    public Song current() { return (current >= 0 && current < queue.size()) ? queue.get(current) : null; }
    public boolean isPlaying() { return player != null && player.isPlaying(); }

    public void playQueue(List<Song> songs, int startIndex) {
        queue.clear();
        queue.addAll(songs);
        emitQueue();
        playIndex(startIndex);
    }

    public void playIndex(int idx) {
        if (idx < 0 || idx >= queue.size()) return;
        current = idx;
        Song s = queue.get(idx);
        emitCurrent(s);
        Api.fetchPlayUrl(s.id, (url, err) -> {
            if (url == null) { Log.w("MusicApp", "no url for " + s.id + " " + err); return; }
            s.mp3Url = url;
            startPlay(s);
        });
        Api.fetchLrc(s.id, (lrc, err) -> {
            s.lrc = lrc == null ? "" : lrc;
            emitCurrent(s);
        });
    }

    /** 搜索关键词并立即播放第一条结果。 */
    public void searchAndPlay(String kw) {
        Api.search(kw, 0, 5, (list, err) -> {
            if (list != null && !list.isEmpty()) playQueue(list, 0);
        });
    }

    private void startPlay(Song s) {
        try {
            if (player == null) player = new MediaPlayer();
            else { player.reset(); }
            player.setDataSource(s.mp3Url);
            player.setLooping(false);
            player.setOnPreparedListener(mp -> {
                prepared = true;
                mp.start();
                emitPlay(true);
                startTicker();
            });
            player.setOnCompletionListener(mp -> next());
            player.setOnErrorListener((mp, what, extra) -> { next(); return true; });
            player.prepareAsync();
        } catch (Exception e) {
            Log.e("MusicApp", "play error", e);
        }
    }

    public void toggle() {
        if (player == null || !prepared) return;
        if (player.isPlaying()) { player.pause(); emitPlay(false); }
        else { player.start(); emitPlay(true); startTicker(); }
    }

    public void next() { if (!queue.isEmpty()) playIndex((current + 1) % queue.size()); }
    public void prev() { if (!queue.isEmpty()) playIndex((current - 1 + queue.size()) % queue.size()); }

    public void seekTo(long ms) {
        if (player != null && prepared) player.seekTo((int) ms);
    }

    public long position() { return (player != null && prepared) ? player.getCurrentPosition() : 0; }
    public long duration() { return (player != null && prepared) ? player.getDuration() : 0; }

    private void startTicker() {
        ticker.removeCallbacks(tick);
        ticker.postDelayed(tick, 300);
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (player != null && player.isPlaying()) {
                long p = player.getCurrentPosition();
                long d = player.getDuration();
                for (PlayerListener l : listeners) l.onProgress(p, d);
                ticker.postDelayed(this, 300);
            }
        }
    };

    private void emitQueue() { for (PlayerListener l : listeners) l.onQueueChanged(); }
    private void emitCurrent(Song s) { for (PlayerListener l : listeners) l.onCurrentChanged(s); }
    private void emitPlay(boolean p) { for (PlayerListener l : listeners) l.onPlayStateChanged(p); }
}
