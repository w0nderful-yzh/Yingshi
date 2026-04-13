package com.yzh.yingshi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceUpdateRequest {
    @NotBlank(message = "deviceName 不能为空")
    private String deviceName;
    private String streamUrl;
    private String remark;
}
