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

/** 全局播放器。 */
public class MusicApp extends Application {

    public interface PlayerListener {
        void onQueueChanged();
        void onCurrentChanged(Song s);
        void onPlayStateChanged(boolean playing);
        void onProgress(long pos, long dur);
    }

    private static MusicApp INSTANCE;
    public static MusicApp get() { return INSTANCE; }

    private final List listeners = new ArrayList();
    private final List queue = new ArrayList();
    private int current = -1;

    private MediaPlayer player;
    private boolean prepared = false;
    private final Handler ticker = new Handler(Looper.getMainLooper());

    public void onCreate() { super.onCreate(); INSTANCE = this; }

    public void addListener(PlayerListener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(PlayerListener l) { listeners.remove(l); }

    public List getQueue() { return queue; }
    public int getCurrentIndex() { return current; }
    public Song current() { return (current >= 0 && current < queue.size()) ? (Song) queue.get(current) : null; }
    public boolean isPlaying() { return player != null && player.isPlaying(); }

    public void playQueue(List songs, int startIndex) {
        queue.clear();
        queue.addAll(songs);
        emitQueue();
        playIndex(startIndex);
    }

    public void playIndex(int idx) {
        if (idx < 0 || idx >= queue.size()) return;
        current = idx;
        final Song s = (Song) queue.get(idx);
        emitCurrent(s);
        Api.fetchPlayUrl(s.id, new Api.StringCallback() {
            public void onResult(String url, String err) {
                if (url == null) return;
                s.mp3Url = url;
                startPlay(s);
            }
        });
        Api.fetchLrc(s.id, new Api.StringCallback() {
            public void onResult(String lrc, String err) {
                s.lrc = lrc == null ? "" : lrc;
                emitCurrent(s);
            }
        });
    }

    private void startPlay(Song s) {
        try {
            if (player == null) player = new MediaPlayer();
            else player.reset();
            player.setDataSource(s.mp3Url);
            player.setLooping(false);
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    prepared = true;
                    mp.start();
                    emitPlay(true);
                    startTicker();
                }
            });
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) { next(); }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) { next(); return true; }
            });
            player.prepareAsync();
        } catch (Exception e) { Log.e("MusicApp", "play", e); }
    }

    public void toggle() {
        if (player == null || !prepared) return;
        if (player.isPlaying()) { player.pause(); emitPlay(false); }
        else { player.start(); emitPlay(true); startTicker(); }
    }

    public void next() { if (!queue.isEmpty()) playIndex((current + 1) % queue.size()); }
    public void prev() { if (!queue.isEmpty()) playIndex((current - 1 + queue.size()) % queue.size()); }

    public void seekTo(long ms) { if (player != null && prepared) player.seekTo((int) ms); }
    public long position() { return (player != null && prepared) ? player.getCurrentPosition() : 0; }
    public long duration() { return (player != null && prepared) ? player.getDuration() : 0; }

    public void searchAndPlay(String kw) {
        Api.search(kw, 0, 5, new Api.ListCallback() {
            public void onResult(List list, String err) {
                if (list != null && !list.isEmpty()) playQueue(list, 0);
            }
        });
    }

    private void startTicker() {
        ticker.removeCallbacks(tick);
        ticker.postDelayed(tick, 300);
    }
    private final Runnable tick = new Runnable() { public void run() {
        if (player != null && player.isPlaying()) {
            long p = player.getCurrentPosition(); long d = player.getDuration();
            for (int i = 0; i < listeners.size(); i++) ((PlayerListener) listeners.get(i)).onProgress(p, d);
            ticker.postDelayed(this, 300);
        }
    }};

    private void emitQueue() { for (int i=0;i<listeners.size();i++) ((PlayerListener)listeners.get(i)).onQueueChanged(); }
    private void emitCurrent(Song s) { for (int i=0;i<listeners.size();i++) ((PlayerListener)listeners.get(i)).onCurrentChanged(s); }
    private void emitPlay(boolean p) { for (int i=0;i<listeners.size();i++) ((PlayerListener)listeners.get(i)).onPlayStateChanged(p); }
}
