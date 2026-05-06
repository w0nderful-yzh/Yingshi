package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pet_detection_config")
public class PetDetectionConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long petId;

    private Long deviceId;

    private Integer enabled;

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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
