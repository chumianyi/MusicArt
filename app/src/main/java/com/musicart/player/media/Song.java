package com.musicart.player.media;

import java.io.Serializable;

/** 一首歌曲的数据模型。 */
public class Song implements Serializable {
    public long id;
    public String name = "";
    public String artist = "";
    public String album = "";
    public String coverUrl = "";
    public long durationMs;
    public String mp3Url = "";
    public String lrc = "";
    public transient android.graphics.Bitmap coverBitmap;

    public String displayTitle() { return name == null ? "" : name; }
    public String displayArtist() { return artist == null ? "" : artist; }
}
