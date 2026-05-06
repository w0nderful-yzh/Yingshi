package com.yzh.yingshi.dto;

import lombok.Data;

@Data
public class AlarmQueryDTO {

    private Long deviceId;

    private Integer readStatus;

    private String startTime;

    private String endTime;

    private String keyword;
}
