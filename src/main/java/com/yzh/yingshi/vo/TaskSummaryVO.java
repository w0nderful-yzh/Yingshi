package com.yzh.yingshi.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TaskSummaryVO {
    private Long taskId;
    private Integer frameCount;
    private Integer eventCount;
    private Integer stillnessCount;
    private Integer pacingCount;
    private Integer dangerZoneCount;
    private BigDecimal movementScore;
    private List<ZoneStayStatVO> zoneStayStats;
}
