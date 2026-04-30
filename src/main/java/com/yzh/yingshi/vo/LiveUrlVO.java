package com.yzh.yingshi.vo;

import lombok.Data;

@Data
public class LiveUrlVO {

    private Long deviceId;

    private String deviceSerial;

    private Integer channelNo;

    private Integer protocol;

    private Integer quality;

    private String url;

    private String expireTime;
}
