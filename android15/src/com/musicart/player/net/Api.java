package com.musicart.player.net;

import android.os.Handler;
import android.os.Looper;

import com.musicart.player.media.Song;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/** 音乐接口。 */
public class Api {

    public interface ListCallback { void onResult(List data, String error); }
    public interface StringCallback { void onResult(String text, String error); }

    private static final String SEARCH = "https://music.163.com/api/search/get";
    private static final String METING = "https://api.injahow.cn/meting/";
    private static final String REC_BASE = "https://api.mnchen.cn";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static void go(Runnable r) { new Thread(r).start(); }

    public static void search(final String keyword, final int offset, final int limit, final ListCallback cb) {
        go(new Runnable() { public void run() {
            final List out = new ArrayList();
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
                                if (s.coverUrl.length() == 0) s.coverUrl = al.optString("pic");
                            }
                            out.add(s);
                        }
                    }
                }
            } catch (Exception e) { err = e.getMessage(); }
            final List f = out; final String fe = err;
            MAIN.post(new Runnable() { public void run() { cb.onResult(f, fe); } });
        }});
    }

    @SuppressWarnings("unchecked")
    private static String parseArtists(JSONObject o) {
        JSONArray arr = o.optJSONArray("artists");
        if (arr == null) arr = o.optJSONArray("ar");
        if (arr == null) return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < arr.length(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(arr.optJSONObject(i).optString("name"));
        }
        return sb.toString();
    }

    public static void fetchPlayUrl(final long songId, final StringCallback cb) {
        go(new Runnable() { public void run() {
            String url = METING + "?server=netease&type=url&id=" + songId;
            try {
                String body = Http.getBlocking(url).trim();
                final String play = cleanUrl(body);
                MAIN.post(new Runnable() { public void run() { cb.onResult(play, play == null ? "empty" : null); } });
            } catch (final Exception e) {
                MAIN.post(new Runnable() { public void run() { cb.onResult(null, e.getMessage()); } });
            }
        }});
    }

    private static String cleanUrl(String body) {
        if (body == null) return null;
        String t = body.trim();
        while (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
            t = t.substring(1, t.length() - 1).trim();
        }
        if (t.startsWith("http")) return t;
        return null;
    }

    public static void fetchLrc(final long songId, final StringCallback cb) {
        go(new Runnable() { public void run() {
            String url = METING + "?server=netease&type=lrc&id=" + songId;
            try {
                final String body = Http.getBlocking(url);
                MAIN.post(new Runnable() { public void run() { cb.onResult(body, null); } });
            } catch (final Exception e) {
                MAIN.post(new Runnable() { public void run() { cb.onResult(null, e.getMessage()); } });
            }
        }});
    }

    public static void fetchRecommend(final ListCallback cb) {
        go(new Runnable() { public void run() {
            final List out = new ArrayList();
            String[] cands = new String[]{
                    REC_BASE + "/api/playlist", REC_BASE + "/playlist",
                    REC_BASE + "/api/recommend", REC_BASE + "/music/home"
            };
            for (int i = 0; i < cands.length; i++) {
                try {
                    String body = Http.getBlocking(cands[i]);
                    List r = parseRecommend(body);
                    if (!r.isEmpty()) { out.addAll(r); break; }
                } catch (Exception ignore) {}
            }
            MAIN.post(new Runnable() { public void run() { cb.onResult(out, null); } });
        }});
    }

    @SuppressWarnings("unchecked")
    private static List parseRecommend(String body) {
        List out = new ArrayList();
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
}
