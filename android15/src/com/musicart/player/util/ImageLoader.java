package com.musicart.player.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;

/** 自写图片缓存（LinkedHashMap，API 1 兼容）。 */
public class ImageLoader {

    public interface Callback { void onBitmap(Bitmap b); }

    private static ImageLoader INSTANCE;
    private Context appCtx;
    private final LinkedHashMap<String, Bitmap> cache = new LinkedHashMap<String, Bitmap>(16, 0.75f, true);
    private final int maxCache = 24;
    private final Handler main = new Handler(Looper.getMainLooper());

    public static synchronized ImageLoader get(Context c) {
        if (INSTANCE == null) { INSTANCE = new ImageLoader(); INSTANCE.appCtx = c.getApplicationContext(); }
        return INSTANCE;
    }

    public void load(String url, final int req, final Callback cb) {
        final String furl = url;
        if (url == null || url.length() == 0) { cb.onBitmap(null); return; }
        final String key = url + "@" + req;
        synchronized (cache) {
            Bitmap hit = cache.get(key);
            if (hit != null) { cb.onBitmap(hit); return; }
        }
        new Thread(new Runnable() { public void run() {
            Bitmap bmp = null;
            try {
                byte[] data = downloadBytes(furl);
                if (data != null) {
                    BitmapFactory.Options op = new BitmapFactory.Options();
                    op.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(data, 0, data.length, op);
                    op.inSampleSize = sample(op.outWidth, op.outHeight, req);
                    op.inJustDecodeBounds = false;
                    bmp = BitmapFactory.decodeByteArray(data, 0, data.length, op);
                }
            } catch (Exception ignore) {}
            final Bitmap f = bmp;
            if (f != null) synchronized (cache) {
                cache.put(key, f);
                while (cache.size() > maxCache) {
                    String first = cache.keySet().iterator().next();
                    cache.remove(first);
                }
            }
            main.post(new Runnable() { public void run() { cb.onBitmap(f); } });
        }}).start();
    }

    private int sample(int w, int h, int req) {
        int s = 1;
        while (w / s > req || h / s > req) s *= 2;
        return s;
    }

    private byte[] downloadBytes(String url) throws Exception {
        InputStream is = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            if (url.startsWith("asset://")) {
                is = appCtx.getAssets().open(url.substring("asset://".length()));
            } else {
                is = new URL(url).openStream();
            }
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toByteArray();
        } finally { if (is != null) try { is.close(); } catch (Exception e) {} }
    }
}
