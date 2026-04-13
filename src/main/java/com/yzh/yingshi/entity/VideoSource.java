package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_source")
public class VideoSource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileId;
    private String sourceType;
    private String fileName;
    private String fileUrl;
    private Long size;
    private String externalUrl;
    private Long deviceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
