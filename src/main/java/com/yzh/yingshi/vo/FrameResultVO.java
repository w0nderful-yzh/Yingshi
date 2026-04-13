package com.yzh.yingshi.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FrameResultVO {
    private Long frameId;
    private LocalDateTime frameTime;
    private String imageUrl;
    private String petType;
    private List<Integer> bbox;
    private Integer centerX;
    private Integer centerY;
    private BigDecimal confidence;
    private String zoneType;
}
