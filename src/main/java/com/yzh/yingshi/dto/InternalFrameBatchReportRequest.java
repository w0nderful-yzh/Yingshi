package com.yzh.yingshi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InternalFrameBatchReportRequest {

    @Valid
    @NotEmpty(message = "records 不能为空")
    private List<FrameRecordPayload> records;

    @Data
    public static class FrameRecordPayload {
        private LocalDateTime frameTime;
        private String imageUrl;
        private String petType;
        private List<Integer> bbox;
        private Integer centerX;
        private Integer centerY;
        private BigDecimal confidence;
        private String zoneType;
    }
}
