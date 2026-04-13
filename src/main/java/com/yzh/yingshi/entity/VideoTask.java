package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_task")
public class VideoTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskName;
    private Long deviceId;
    private String sourceType;
    private String fileId;
    private String videoUrl;
    private Integer frameIntervalSec;
    private String status;
    private Integer progress;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime updatedAt;
}
