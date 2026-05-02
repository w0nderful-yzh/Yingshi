package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alarm_message")
public class AlarmMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deviceId;

    private String deviceSerial;

    private Integer channelNo;

    private String alarmId;

    private String alarmType;

    private String alarmName;

    private LocalDateTime alarmTime;

    private String alarmPicUrl;

    private String alarmContent;

    private Integer readStatus;

    private String source;

    private String rawJson;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
