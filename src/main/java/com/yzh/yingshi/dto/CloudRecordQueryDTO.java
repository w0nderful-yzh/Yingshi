package com.yzh.yingshi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CloudRecordQueryDTO {

    @NotNull(message = "deviceId 不能为空")
    private Long deviceId;

    @NotBlank(message = "startTime 不能为空")
    private String startTime;

    @NotBlank(message = "endTime 不能为空")
    private String endTime;
}
