package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.common.api.PageResult;
import com.yzh.yingshi.service.FrameService;
import com.yzh.yingshi.vo.FrameResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tasks")
public class FrameController {

    private final FrameService frameService;

    @GetMapping("/{taskId}/frames")
    public ApiResponse<PageResult<FrameResultVO>> listTaskFrames(@PathVariable("taskId") Long taskId,
                                                                 @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                                 @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        return ApiResponse.success(frameService.listTaskFrames(taskId, page, size));
    }
}

