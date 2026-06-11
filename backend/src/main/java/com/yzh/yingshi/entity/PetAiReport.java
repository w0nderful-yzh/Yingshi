package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pet_ai_report")
public class PetAiReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

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

    private String recommendationsJson;

    private String uncertaintiesJson;

    private String evidenceJson;

    private String analysisJson;

    private String modelName;

    private String promptVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
