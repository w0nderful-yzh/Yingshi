package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.InternalEventReportRequest;
import com.yzh.yingshi.dto.InternalFrameBatchReportRequest;
import com.yzh.yingshi.service.EventService;
import com.yzh.yingshi.service.FrameService;
import com.yzh.yingshi.vo.EventIdVO;
import com.yzh.yingshi.vo.SavedCountVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/tasks")
public class InternalTaskReportController {

    private final FrameService frameService;
    private final EventService eventService;

    @PostMapping("/{taskId}/frames:batch-report")
    public ApiResponse<SavedCountVO> batchReportFrames(@PathVariable("taskId") Long taskId,
                                                       @Valid @RequestBody InternalFrameBatchReportRequest request) {
        return ApiResponse.success(frameService.batchReport(taskId, request));
    }

    @PostMapping("/{taskId}/events:report")
    public ApiResponse<EventIdVO> reportEvent(@PathVariable("taskId") Long taskId,
                                              @Valid @RequestBody InternalEventReportRequest request) {
        return ApiResponse.success(eventService.internalReport(taskId, request));
    }
}


