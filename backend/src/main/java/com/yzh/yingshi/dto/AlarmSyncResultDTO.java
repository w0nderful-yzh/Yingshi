package com.yzh.yingshi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AlarmSyncResultDTO {

    private Integer deviceCount;

    private Integer fetchedCount;

    private Integer insertedCount;

    private String message;
}
