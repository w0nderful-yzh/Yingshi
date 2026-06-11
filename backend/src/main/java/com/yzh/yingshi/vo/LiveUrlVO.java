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

    /**
     * EZUIKit 播放 EZOPEN 地址所需的临时访问令牌。
     * 仅在 protocol=EZOPEN 时返回。
     */
    private String accessToken;

    private String expireTime;
}
