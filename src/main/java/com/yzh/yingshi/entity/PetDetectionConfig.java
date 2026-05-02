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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
