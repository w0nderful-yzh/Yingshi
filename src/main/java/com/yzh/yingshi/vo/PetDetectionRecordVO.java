package com.yzh.yingshi.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PetDetectionRecordVO {

    private Long id;

    private Long detectionConfigId;

    private Long petId;
    private String petName;

    private Long deviceId;
    private String deviceName;
    private String deviceSerial;

    private LocalDateTime detectTime;

    private Double petCoordX;
    private Double petCoordY;
    private Double petWidth;
    private Double petHeight;

    private Integer inSafeZone;

    private Integer alarmTriggered;

    private String snapshotUrl;

    private LocalDateTime createdAt;
}
