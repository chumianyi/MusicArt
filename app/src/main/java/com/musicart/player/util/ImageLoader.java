package com.musicart.player.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import com.musicart.player.net.Http;

import java.util.concurrent.Executors;

/** 自写极简图片加载：内存 LruCache + 后台线程网络下载。 */
public class ImageLoader {

    private static ImageLoader INSTANCE;
    private static android.content.Context appCtx;
    private final LruCache<String, Bitmap> cache;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService pool = Executors.newFixedThreadPool(3);

    public static void init(android.content.Context c) { appCtx = c.getApplicationContext(); }
    android.content.Context ctx() { return appCtx; }

    public interface Callback { void onBitmap(Bitmap b); }

    private ImageLoader() {
        int max = (int) (Runtime.getRuntime().maxMemory() / 8);
        cache = new LruCache<String, Bitmap>(max) {
            @Override protected int sizeOf(String k, Bitmap v) {
                return v.getByteCount();
            }
        };
    }

    public static synchronized ImageLoader get() {
        if (INSTANCE == null) INSTANCE = new ImageLoader();
        return INSTANCE;
    }

    public void load(String url, int reqSizePx, Callback cb) {
        if (url == null || url.isEmpty()) { cb.onBitmap(null); return; }
        String key = url + "@" + reqSizePx;
        Bitmap hit = cache.get(key);
        if (hit != null) { cb.onBitmap(hit); return; }
        pool.execute(() -> {
            Bitmap bmp = null;
            try {
                byte[] data = downloadBytes(url);
                if (data != null) {
                    BitmapFactory.Options op = new BitmapFactory.Options();
                    op.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(data, 0, data.length, op);
                    op.inSampleSize = calcSample(op.outWidth, op.outHeight, reqSizePx);
                    op.inJustDecodeBounds = false;
                    bmp = BitmapFactory.decodeByteArray(data, 0, data.length, op);
                }
            } catch (Exception ignore) {}
            final Bitmap f = bmp;
            if (f != null) cache.put(key, f);
            main.post(() -> cb.onBitmap(f));
        });
    }

    private int calcSample(int w, int h, int req) {
        int s = 1;
        while (w / s > req || h / s > req) s *= 2;
        return s;
    }

    private byte[] downloadBytes(String url) throws Exception {
        java.io.InputStream is = null;
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try {
            if (url.startsWith("asset://")) {
                is = appCtx.getAssets().open(url.substring("asset://".length()));
            } else {
                is = new java.net.URL(url).openStream();
            }
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toByteArray();
        } finally { if (is != null) try { is.close(); } catch (Exception ignore) {} }
    }
}
