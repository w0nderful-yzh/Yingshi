package com.yzh.yingshi.dto;

import lombok.Data;

@Data
public class FileUploadRequest {
    private String fileName;
    private String fileUrl;
    private Integer durationSeconds;
    private String remark;
}