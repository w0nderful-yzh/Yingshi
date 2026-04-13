package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.service.SummaryService;
import com.yzh.yingshi.vo.TaskSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tasks")
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping("/{taskId}/summary")
    public ApiResponse<TaskSummaryVO> taskSummary(@PathVariable("taskId") Long taskId) {
        return ApiResponse.success(summaryService.taskSummary(taskId));
    }
}

