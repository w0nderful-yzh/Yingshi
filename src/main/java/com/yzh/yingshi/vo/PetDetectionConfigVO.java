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

    private List<PetSafeZoneVO> safeZones;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
