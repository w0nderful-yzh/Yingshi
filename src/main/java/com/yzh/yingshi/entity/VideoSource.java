package com.yzh.yingshi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_source")
public class VideoSource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private String sourceType;
    private String sourceUrl;
    
    // 以下为根据第一阶段接口额外填充的属性，如果不需要持久化可以声明为非数据库字段或随后自行扩展
    @TableField(exist = false)
    private String fileName;
    
    @TableField(exist = false)
    private Integer durationSeconds;
    
    @TableField(exist = false)
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
