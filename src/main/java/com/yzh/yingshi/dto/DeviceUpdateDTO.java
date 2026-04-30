package com.yzh.yingshi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeviceUpdateDTO {

    @NotBlank(message = "deviceName 不能为空")
    @Size(max = 100, message = "deviceName 长度不能超过100")
    private String deviceName;

    @Size(max = 255, message = "remark 长度不能超过255")
    private String remark;

    private String status;
}
