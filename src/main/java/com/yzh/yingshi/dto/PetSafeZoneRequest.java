package com.yzh.yingshi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PetSafeZoneRequest {

    @NotNull(message = "检测配置ID不能为空")
    private Long detectionConfigId;

    private String zoneName;

    /** 区域类型: RECTANGLE / POLYGON, 默认RECTANGLE */
    @NotBlank(message = "区域类型不能为空")
    private String zoneType;

    /** 矩形: 左上角X(百分比 0-100) */
    private Double rectLeft;

    /** 矩形: 左上角Y(百分比 0-100) */
    private Double rectTop;

    /** 矩形: 右下角X(百分比 0-100) */
    private Double rectRight;

    /** 矩形: 右下角Y(百分比 0-100) */
    private Double rectBottom;

    /** 多边形: 顶点坐标列表(百分比 0-100) */
    private List<PointDTO> polygonPoints;

    @Data
    public static class PointDTO {
        private Double x;
        private Double y;
    }
}
