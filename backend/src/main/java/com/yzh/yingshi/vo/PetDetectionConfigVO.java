package com.yzh.yingshi.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PetDetectionConfigVO {

    private Long id;

    private Long userId;

    private Long petId;
    private String petName;

    private Long deviceId;
    private String deviceName;
    private String deviceSerial;

    private Integer enabled;

    private Integer cooldownSeconds;

    private String remark;

    // ---- 异常行为检测阈值 ----

    private Integer petAbsentMinutes;

    private Integer activityWindowMinutes;

    private Integer activityCountThreshold;

    private Integer stillnessMinutes;

    private List<PetSafeZoneVO> safeZones;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
