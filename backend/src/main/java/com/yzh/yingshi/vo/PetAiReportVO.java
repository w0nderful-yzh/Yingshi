package com.yzh.yingshi.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PetAiReportVO {

    private Long id;

    private Long petId;

    private String petName;

    private String sourceType;

    private Long sourceId;

    private LocalDateTime sourceTime;

    private String imageUrl;

    private String reportType;

    private String riskLevel;

    private String title;

    private String summary;

    private String observedBehavior;

    private String evidenceBasis;

    private List<String> recommendations;

    private List<String> uncertainties;

    private Map<String, Object> evidence;

    private String modelName;

    private String promptVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
