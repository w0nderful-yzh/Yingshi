package com.yzh.yingshi.dto;

import lombok.Data;

@Data
public class PetDetectionRecordQueryDTO {

    private Long detectionConfigId;

    private Long petId;

    private Long deviceId;

    /** 是否只查越界告警 */
    private Integer alarmTriggered;

    private String startTime;

    private String endTime;
}
