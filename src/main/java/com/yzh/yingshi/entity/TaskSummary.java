package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("task_summary")
public class TaskSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer frameCount;
    private Integer eventCount;
    private Integer stillnessCount;
    private Integer pacingCount;
    private Integer dangerZoneCount;
    private BigDecimal movementScore;
    private String zoneStayStats;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
