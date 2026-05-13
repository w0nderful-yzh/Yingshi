package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_device")
public class UserDevice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String deviceSerial;

    private String deviceName;

    private String deviceType;

    private Integer channelNo;

    private LocalDateTime boundAt;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
