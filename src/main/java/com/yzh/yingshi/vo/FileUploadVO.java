package com.yzh.yingshi.vo;

import lombok.Data;

@Data
public class FileUploadVO {
    private String fileId;
    private String fileName;
    private String fileUrl;
    private Long size;
}
