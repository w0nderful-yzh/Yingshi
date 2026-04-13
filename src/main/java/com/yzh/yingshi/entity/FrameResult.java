package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("frame_result")
public class FrameResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private LocalDateTime frameTime;
    private String imageUrl;
    private String petType;
    private String bbox;
    private Integer centerX;
    private Integer centerY;
    private BigDecimal confidence;
    private String zoneType;
    private LocalDateTime createdAt;
}
