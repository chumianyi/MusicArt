package com.musicart.player.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** LRC 解析（Java 6）。 */
public class LrcParser {

    public static class Line {
        public long timeMs;
        public String text;
        public Line(long t, String s){ timeMs = t; text = s; }
    }

    public static List parse(String lrc) {
        List out = new ArrayList();
        if (lrc == null || lrc.length() == 0) { out.add(new Line(0, "暂无歌词")); return out; }
        String[] lines = lrc.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            while (line.startsWith("[")) {
                int close = line.indexOf(']');
                if (close < 0) break;
                String tag = line.substring(1, close);
                long t = toMs(tag);
                String text = line.substring(close + 1).trim();
                if (t >= 0 && text.length() > 0) out.add(new Line(t, text));
                line = line.substring(close + 1);
            }
        }
        Collections.sort(out, new Comparator() {
            public int compare(Object a, Object b) {
                long d = ((Line)a).timeMs - ((Line)b).timeMs;
                return d < 0 ? -1 : (d > 0 ? 1 : 0);
            }
        });
        if (out.isEmpty()) out.add(new Line(0, "暂无歌词"));
        return out;
    }

    private static long toMs(String tag) {
        try {
            String[] mmss = tag.split(":");
            if (mmss.length < 2) return -1;
            long min = Long.parseLong(mmss[0].trim());
            float sec = Float.parseFloat(mmss[1].trim());
            return (long)(min * 60000 + sec * 1000);
        } catch (Exception e) { return -1; }
    }
}
