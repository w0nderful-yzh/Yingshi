package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("zone_config")
public class ZoneConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private String zoneName;
    private String zoneType;
    private String coordinates;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
