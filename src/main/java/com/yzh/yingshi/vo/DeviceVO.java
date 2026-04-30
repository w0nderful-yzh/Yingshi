package com.yzh.yingshi.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceVO {

    private Long id;

    private String deviceSerial;

    private Integer channelNo;

    private String deviceName;

    private String deviceType;

    private String sourceType;

    private String status;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
