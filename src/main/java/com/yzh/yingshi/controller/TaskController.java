package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.common.api.PageResult;
import com.yzh.yingshi.dto.TaskCreateRequest;
import com.yzh.yingshi.service.TaskService;
import com.yzh.yingshi.vo.TaskStatusVO;
import com.yzh.yingshi.vo.TaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ApiResponse<TaskStatusVO> create(@Valid @RequestBody TaskCreateRequest request) {
        return ApiResponse.success(taskService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResult<TaskVO>> list(@RequestParam(value = "deviceId", required = false) Long deviceId,
                                                @RequestParam(value = "status", required = false) String status,
                                                @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        return ApiResponse.success(taskService.list(deviceId, status, page, size));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskVO> detail(@PathVariable("taskId") Long taskId) {
        return ApiResponse.success(taskService.detail(taskId));
    }

    @PostMapping("/{taskId}/start")
    public ApiResponse<TaskStatusVO> start(@PathVariable("taskId") Long taskId) {
        return ApiResponse.success(taskService.start(taskId));
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<TaskStatusVO> cancel(@PathVariable("taskId") Long taskId) {
        return ApiResponse.success(taskService.cancel(taskId));
    }
}

