package com.yzh.yingshi.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PetDetectionResultVO {

    private Long recordId;

    private Long petId;
    private String petName;

    private Long deviceId;
    private String deviceName;

    private LocalDateTime detectTime;

    private Boolean inSafeZone;

    private Boolean alarmTriggered;

    private String snapshotUrl;

    private String message;
}
