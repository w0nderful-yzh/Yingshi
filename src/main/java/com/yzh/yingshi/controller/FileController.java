package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.service.FileService;
import com.yzh.yingshi.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload/video")
    public ApiResponse<FileUploadVO> uploadVideo(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "deviceId", required = false) Long deviceId) {
        return ApiResponse.success(fileService.uploadVideo(file, deviceId));
    }
}


