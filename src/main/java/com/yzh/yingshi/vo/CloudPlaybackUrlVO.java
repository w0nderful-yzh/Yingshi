package com.yzh.yingshi.vo;

import lombok.Data;

@Data
public class CloudPlaybackUrlVO {

    private Long deviceId;

    private String deviceSerial;

    private Integer channelNo;

    private Integer protocol;

    private Integer quality;

    private String startTime;

    private String endTime;

    private String url;

    private String expireTime;
}
