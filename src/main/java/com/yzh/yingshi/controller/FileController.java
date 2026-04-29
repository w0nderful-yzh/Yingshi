package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.FileUploadRequest;
import com.yzh.yingshi.service.FileService;
import com.yzh.yingshi.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/video-source")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ApiResponse<FileUploadVO> uploadVideo(@Valid @RequestBody FileUploadRequest request) {
        return ApiResponse.success(fileService.reportVideo(request));
    }
}


