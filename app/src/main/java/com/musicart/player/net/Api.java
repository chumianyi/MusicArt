package com.musicart.player.net;

import android.os.Handler;
import android.os.Looper;

import com.musicart.player.media.Song;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/** 所有音乐接口的统一入口。 */
public class Api {

    public interface ListCallback<T> { void onResult(List<T> data, String error); }
    public interface StringCallback { void onResult(String text, String error); }

    private static final String SEARCH = "https://music.163.com/api/search/get";
    private static final String METING = "https://api.injahow.cn/meting/";
    private static final String REC_BASE = "https://api.mnchen.cn";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final java.util.concurrent.ExecutorService POOL =
            Executors.newFixedThreadPool(4);

    private static List<Song> parseRecommend(String body) {
        List<Song> out = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(body);
            JSONArray arr = root.optJSONArray("data");
            if (arr == null) arr = root.optJSONArray("songs");
            if (arr == null) arr = root.optJSONArray("list");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    Song s = new Song();
                    s.id = o.optLong("id");
                    s.name = o.optString("name", o.optString("title"));
                    s.artist = o.optString("artist", o.optString("singer"));
                    s.coverUrl = o.optString("pic", o.optString("cover", o.optString("picUrl")));
                    out.add(s);
                }
            }
        } catch (Exception ignore) {}
        return out;
    }

    // ---------- 搜索 ----------
    public static void search(String keyword, int offset, int limit, final ListCallback<Song> cb) {
        POOL.execute(() -> {
            List<Song> out = new ArrayList<>();
            String err = null;
            try {
                String url = SEARCH + "?s=" + URLEncoder.encode(keyword, "UTF-8")
                        + "&offset=" + offset + "&limit=" + limit + "&type=1";
                String body = Http.getBlocking(url);
                JSONObject root = new JSONObject(body);
                JSONObject result = root.optJSONObject("result");
                if (result != null) {
                    JSONArray songs = result.optJSONArray("songs");
                    if (songs != null) {
                        for (int i = 0; i < songs.length(); i++) {
                            JSONObject o = songs.getJSONObject(i);
                            Song s = new Song();
                            s.id = o.optLong("id");
                            s.name = o.optString("name");
                            s.durationMs = o.optLong("dt", o.optLong("duration"));
                            s.artist = parseArtists(o);
                            JSONObject al = o.optJSONObject("album");
                            if (al == null) al = o.optJSONObject("al");
                            if (al != null) {
                                s.album = al.optString("name");
                                s.coverUrl = al.optString("picUrl");
                                if (s.coverUrl.isEmpty()) s.coverUrl = al.optString("pic");
                            }
                            out.add(s);
                        }
                    }
                }
            } catch (Exception e) {
                err = e.getMessage();
            }
            final List<Song> f = out; final String fe = err;
            MAIN.post(() -> cb.onResult(f, fe));
        });
    }

    private static String parseArtists(JSONObject o) {
        JSONArray arr = o.optJSONArray("artists");
        if (arr == null) arr = o.optJSONArray("ar");
        if (arr == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(arr.optJSONObject(i).optString("name"));
        }
        return sb.toString();
    }

    // ---------- 播放直链 ----------
    public static void fetchPlayUrl(long songId, final StringCallback cb) {
        POOL.execute(() -> {
            String url = METING + "?server=netease&type=url&id=" + songId;
            try {
                String body = Http.getBlocking(url).trim();
                // Meting 直接返回音频地址文本
                final String play = cleanUrl(body);
                MAIN.post(() -> cb.onResult(play, play == null ? "empty url" : null));
            } catch (Exception e) {
                MAIN.post(() -> cb.onResult(null, e.getMessage()));
            }
        });
    }

    private static String cleanUrl(String body) {
        if (body == null) return null;
        String t = body.trim();
        // 去掉首尾引号
        while ((t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2)) {
            t = t.substring(1, t.length() - 1).trim();
        }
        if (t.startsWith("http")) return t;
        return null;
    }

    // ---------- 歌词 ----------
    public static void fetchLrc(long songId, final StringCallback cb) {
        POOL.execute(() -> {
            String url = METING + "?server=netease&type=lrc&id=" + songId;
            try {
                String body = Http.getBlocking(url);
                MAIN.post(() -> cb.onResult(body, null));
            } catch (Exception e) {
                MAIN.post(() -> cb.onResult(null, e.getMessage()));
            }
        });
    }

    // ---------- 在线推荐 ----------
    public static void fetchRecommend(final ListCallback<Song> cb) {
        POOL.execute(() -> {
            List<Song> out = new ArrayList<>();
            String err = null;
            String[] candidates = new String[]{
                    REC_BASE + "/api/playlist",
                    REC_BASE + "/playlist",
                    REC_BASE + "/api/recommend",
                    REC_BASE + "/music/home"
            };
            for (String u : candidates) {
                try {
                    String body = Http.getBlocking(u);
                    out = parseRecommend(body);
                    if (!out.isEmpty()) break;
                } catch (Exception e) {
                    err = e.getMessage();
                }
            }
            final List<Song> f = out; final String fe = err;
            MAIN.post(() -> cb.onResult(f, fe));
        });
    }
}
