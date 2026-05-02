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

    /** 越界告警冷却时间(秒), 默认300 */
    private Integer cooldownSeconds;

    private String remark;

    // ---- 异常行为检测阈值 ----

    /** 长时间未出现阈值(分钟), 默认60 */
    private Integer petAbsentMinutes;

    /** 异常活跃: 时间窗口(分钟), 默认10 */
    private Integer activityWindowMinutes;

    /** 异常活跃: 窗口内触发次数阈值, 默认5 */
    private Integer activityCountThreshold;

    /** 长时间静止阈值(分钟), 默认30 */
    private Integer stillnessMinutes;
}
