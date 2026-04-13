package com.yzh.yingshi.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InternalEventReportRequest {
    @NotBlank(message = "eventType 不能为空")
    private String eventType;
    @NotBlank(message = "eventLevel 不能为空")
    private String eventLevel;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @NotBlank(message = "description 不能为空")
    private String description;
    private String snapshotUrl;
    private JsonNode ruleDetail;
}
