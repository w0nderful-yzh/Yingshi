package com.yzh.yingshi.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlarmMessageVO {

    private Long id;

    private Long deviceId;

    private String deviceSerial;

    private String deviceName;

    private Integer channelNo;

    private String alarmType;

    private String alarmName;

    private LocalDateTime alarmTime;

    private String alarmPicUrl;

    private String alarmContent;

    private Integer readStatus;

    private String source;

    private LocalDateTime createdAt;
}
