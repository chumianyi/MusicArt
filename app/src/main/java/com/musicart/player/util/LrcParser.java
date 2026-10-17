package com.musicart.player.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 极简 LRC 解析。 */
public class LrcParser {

    public static class Line {
        public long timeMs;
        public String text;
        public Line(long t, String s){ timeMs = t; text = s; }
    }

    public static List<Line> parse(String lrc) {
        List<Line> out = new ArrayList<>();
        if (lrc == null || lrc.isEmpty()) { out.add(new Line(0, "暂无歌词")); return out; }
        String[] lines = lrc.split("\\n");
        for (String line : lines) {
            line = line.trim();
            int bracket = line.indexOf('[');
            while (bracket == 0) {
                int close = line.indexOf(']');
                if (close < 0) break;
                String tag = line.substring(1, close);
                long t = toMs(tag);
                String text = line.substring(close + 1).trim();
                if (t >= 0 && !text.isEmpty()) out.add(new Line(t, text));
                line = line.substring(close + 1);
                bracket = line.startsWith("[") ? 0 : -1;
            }
        }
        Collections.sort(out, Comparator.comparingLong(a -> a.timeMs));
        if (out.isEmpty()) out.add(new Line(0, "暂无歌词"));
        return out;
    }

    private static long toMs(String tag) {
        try {
            String[] mmss = tag.split(":");
            if (mmss.length < 2) return -1;
            long min = Long.parseLong(mmss[0].trim());
            float sec = Float.parseFloat(mmss[1].trim());
            return (long) (min * 60000 + sec * 1000);
        } catch (Exception e) { return -1; }
    }
}
