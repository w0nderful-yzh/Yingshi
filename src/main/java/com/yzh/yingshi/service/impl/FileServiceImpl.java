package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.service.FileService;
import com.yzh.yingshi.vo.FileUploadVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl implements FileService {
    @Override
    public FileUploadVO uploadVideo(MultipartFile file, Long deviceId) {
        return new FileUploadVO();
    }
}

