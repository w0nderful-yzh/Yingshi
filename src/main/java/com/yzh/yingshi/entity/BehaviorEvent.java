package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("behavior_event")
public class BehaviorEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String eventType;
    private String eventLevel;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String description;
    private String snapshotUrl;
    private String ruleDetail;
    private Integer ackStatus;
    private LocalDateTime ackAt;
    private LocalDateTime createdAt;
}
