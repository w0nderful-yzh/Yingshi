package com.yzh.yingshi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PetDetectionConfigRequest {

    @NotNull(message = "宠物ID不能为空")
    private Long petId;

    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    /** 是否启用检测, 默认true */
    private Boolean enabled;

    /** 告警冷却时间(秒), 默认300 */
    private Integer cooldownSeconds;

    private String remark;
}
