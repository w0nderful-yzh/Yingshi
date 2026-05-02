package com.yzh.yingshi.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PetSafeZoneVO {

    private Long id;

    private Long detectionConfigId;

    private String zoneName;

    private String zoneType;

    /** 矩形坐标 (百分比) */
    private Double rectLeft;
    private Double rectTop;
    private Double rectRight;
    private Double rectBottom;

    /** 多边形顶点 (百分比) */
    private List<PointVO> polygonPoints;

    private LocalDateTime createdAt;

    @Data
    public static class PointVO {
        private Double x;
        private Double y;
    }
}
