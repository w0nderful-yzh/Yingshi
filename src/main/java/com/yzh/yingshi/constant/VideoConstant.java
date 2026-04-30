package com.yzh.yingshi.constant;

public class VideoConstant {

    private VideoConstant() {}

    public static final String SOURCE_TYPE_EZVIZ = "EZVIZ";

    public static final String STATUS_ONLINE = "ONLINE";
    public static final String STATUS_OFFLINE = "OFFLINE";
    public static final String STATUS_DISABLED = "DISABLED";

    public static final Integer DEFAULT_CHANNEL_NO = 1;

    public static final Integer PROTOCOL_EZOPEN = 1;
    public static final Integer PROTOCOL_HLS = 2;
    public static final Integer PROTOCOL_RTMP = 3;
    public static final Integer PROTOCOL_FLV = 4;

    public static final Integer ADDRESS_TYPE_LIVE = 1;
    public static final Integer ADDRESS_TYPE_LOCAL_PLAYBACK = 2;
    public static final Integer ADDRESS_TYPE_CLOUD_PLAYBACK = 3;

    public static final Integer QUALITY_HD = 1;
    public static final Integer QUALITY_SMOOTH = 2;

    public static final Integer DEFAULT_LIVE_PROTOCOL = PROTOCOL_HLS;
    public static final Integer DEFAULT_CLOUD_PLAYBACK_PROTOCOL = PROTOCOL_FLV;
    public static final Integer DEFAULT_QUALITY = QUALITY_SMOOTH;
    public static final Integer DEFAULT_EXPIRE_TIME = 86400;

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
}
