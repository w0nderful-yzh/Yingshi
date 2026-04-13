package com.yzh.yingshi.service;

import com.yzh.yingshi.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileUploadVO uploadVideo(MultipartFile file, Long deviceId);
}

