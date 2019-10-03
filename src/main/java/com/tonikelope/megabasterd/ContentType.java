package com.tonikelope.megabasterd;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class ContentType {

    private static final Logger LOG = Logger.getLogger(ContentType.class.getName());

    private final HashMap<String, String> _content_type;

    public ContentType() {

        _content_type = new HashMap();

        _content_type.put("mp2", "audio/x-mpeg");
        _content_type.put("mp3", "audio/x-mpeg");
        _content_type.put("mpga", "audio/x-mpeg");
        _content_type.put("mpega", "audio/x-mpeg");
        _content_type.put("mpg", "video/x-mpeg-system");
        _content_type.put("mpeg", "video/x-mpeg-system");
        _content_type.put("mpe", "video/x-mpeg-system");
        _content_type.put("vob", "video/x-mpeg-system");
        _content_type.put("aac", "audio/mp4");
        _content_type.put("mp4", "video/mp4");
        _content_type.put("mpg4", "video/mp4");
        _content_type.put("m4v", "video/x-m4v");
        _content_type.put("avi", "video/x-msvideo");
        _content_type.put("ogg", "application/ogg");
        _content_type.put("ogv", "video/ogg");
        _content_type.put("asf", "video/x-ms-asf-plugin");
        _content_type.put("asx", "video/x-ms-asf-plugin");
        _content_type.put("ogv", "video/ogg");
        _content_type.put("wmv", "video/x-ms-wmv");
        _content_type.put("wmx", "video/x-ms-wvx");
        _content_type.put("wma", "audio/x-ms-wma");
        _content_type.put("wav", "audio/wav");
        _content_type.put("3gp", "audio/3gpp");
        _content_type.put("3gp2", "audio/3gpp2");
        _content_type.put("divx", "video/divx");
        _content_type.put("flv", "video/flv");
        _content_type.put("mkv", "video/x-matroska");
        _content_type.put("mka", "audio/x-matroska");
        _content_type.put("m3u", "audio/x-mpegurl");
        _content_type.put("webm", "video/webm");
        _content_type.put("rm", "application/vnd.rn-realmedia");
        _content_type.put("ra", "audio/x-realaudio");
        _content_type.put("amr", "audio/amr");
        _content_type.put("flac", "audio/x-flac");
        _content_type.put("mov", "video/quicktime");
        _content_type.put("qt", "video/quicktime");
    }

    public HashMap<String, String> getContent_type() {
        return _content_type;
    }

    public String getMIME(String ext) {
        return _content_type.get(ext);
    }
}
