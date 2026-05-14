package com.yzh.yingshi.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDeviceVO {

    private Long id;

    private String deviceSerial;

    private String deviceName;

    private String deviceType;

    private Integer channelNo;

    private LocalDateTime boundAt;

    private Integer status;
}
