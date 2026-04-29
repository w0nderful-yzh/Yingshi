package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.dto.FileUploadRequest;
import com.yzh.yingshi.entity.VideoSource;
import com.yzh.yingshi.mapper.VideoSourceMapper;
import com.yzh.yingshi.service.FileService;
import com.yzh.yingshi.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final VideoSourceMapper videoSourceMapper;

    @Override
    public FileUploadVO reportVideo(FileUploadRequest request) {
        VideoSource source = new VideoSource();
        source.setSourceType("UPLOAD");
        source.setSourceUrl(request.getFileUrl());
        
        // 可选字段
        source.setFileName(request.getFileName());
        source.setDurationSeconds(request.getDurationSeconds());
        source.setRemark(request.getRemark());
        
        videoSourceMapper.insert(source);

        FileUploadVO vo = new FileUploadVO();
        vo.setSourceId(source.getId());
        vo.setFileName(request.getFileName());
        vo.setFileUrl(request.getFileUrl());
        
        return vo;
    }
}

