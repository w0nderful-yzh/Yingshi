package com.yzh.yingshi.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PetDetectionResultVO {

    private Long recordId;

    private Long petId;
    private String petName;

    private Long deviceId;
    private String deviceName;

    private LocalDateTime detectTime;

    private Double petCoordX;
    private Double petCoordY;
    private Double petWidth;
    private Double petHeight;

    private Boolean inSafeZone;

    private Boolean alarmTriggered;

    private String snapshotUrl;

    private List<PetSafeZoneVO> safeZones;

    private String message;
}
