package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.FileUploadRequest;
import com.yzh.yingshi.vo.FileUploadVO;

public interface FileService {
    FileUploadVO reportVideo(FileUploadRequest request);
}

