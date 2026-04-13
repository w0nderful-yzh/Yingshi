package com.yzh.yingshi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceCreateRequest {
    @NotBlank(message = "deviceName 不能为空")
    private String deviceName;
    @NotBlank(message = "sourceType 不能为空")
    private String sourceType;
    private String streamUrl;
    private String remark;
}
