package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pet_detection_record")
public class PetDetectionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long detectionConfigId;

    private Long petId;

    private Long deviceId;

    private String deviceSerial;

    private LocalDateTime detectTime;

    private Double petCoordX;

    private Double petCoordY;

    private Double petWidth;

    private Double petHeight;

    private Integer inSafeZone;

    private Integer alarmTriggered;

    private String snapshotUrl;

    private String aiResultJson;

    private LocalDateTime createdAt;
}
