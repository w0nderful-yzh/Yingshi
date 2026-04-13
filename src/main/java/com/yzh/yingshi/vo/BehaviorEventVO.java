package com.yzh.yingshi.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BehaviorEventVO {
    private Long eventId;
    private String eventType;
    private String eventLevel;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String description;
    private String snapshotUrl;
    private Boolean ackStatus;
}
