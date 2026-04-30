package com.yzh.yingshi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LiveUrlRequestDTO {

    @NotNull(message = "deviceId 不能为空")
    private Long deviceId;

    private Integer protocol;

    private Integer quality;

    private Integer expireTime;
}
