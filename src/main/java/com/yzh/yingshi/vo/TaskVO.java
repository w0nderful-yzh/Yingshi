package com.yzh.yingshi.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskVO {
    private Long taskId;
    private String taskName;
    private Long deviceId;
    private String sourceType;
    private String status;
    private Integer progress;
    private Integer frameIntervalSec;
    private Integer eventCount;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
}
