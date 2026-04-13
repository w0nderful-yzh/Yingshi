package com.yzh.yingshi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskCreateRequest {
    @NotBlank(message = "taskName 不能为空")
    private String taskName;
    private Long deviceId;
    @NotBlank(message = "sourceType 不能为空")
    private String sourceType;
    private String fileId;
    private String videoUrl;
    private Integer frameIntervalSec;
    private Boolean autoStart;
}
