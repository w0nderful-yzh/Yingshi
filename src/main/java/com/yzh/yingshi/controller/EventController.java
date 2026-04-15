package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.service.EventService;
import com.yzh.yingshi.vo.BehaviorEventDetailVO;
import com.yzh.yingshi.vo.BehaviorEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EventController {

    private final EventService eventService;

    @GetMapping("/tasks/{taskId}/events")
    public ApiResponse<List<BehaviorEventVO>> listTaskEvents(@PathVariable("taskId") Long taskId) {
        return ApiResponse.success(eventService.listTaskEvents(taskId));
    }

    @GetMapping("/events/{eventId}")
    public ApiResponse<BehaviorEventDetailVO> detail(@PathVariable("eventId") Long eventId) {
        return ApiResponse.success(eventService.detail(eventId));
    }

    @PostMapping("/events/{eventId}/ack")
    public ApiResponse<Boolean> ack(@PathVariable("eventId") Long eventId) {
        return ApiResponse.success(eventService.ack(eventId));
    }
}


