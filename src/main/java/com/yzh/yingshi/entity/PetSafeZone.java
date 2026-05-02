package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pet_safe_zone")
public class PetSafeZone {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long detectionConfigId;

    private String zoneName;

    private String zoneType;

    private Double rectLeft;

    private Double rectTop;

    private Double rectRight;

    private Double rectBottom;

    private String polygonPoints;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
