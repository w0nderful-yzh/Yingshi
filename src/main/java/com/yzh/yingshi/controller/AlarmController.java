package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.AlarmQueryDTO;
import com.yzh.yingshi.dto.AlarmSyncResultDTO;
import com.yzh.yingshi.service.AlarmService;
import com.yzh.yingshi.vo.AlarmMessageVO;
import com.yzh.yingshi.vo.AlarmUnreadCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    @PostMapping("/sync")
    public ApiResponse<AlarmSyncResultDTO> sync() {
        return ApiResponse.success(alarmService.syncFromEzviz());
    }

    @GetMapping
    public ApiResponse<List<AlarmMessageVO>> list(
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Integer readStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String keyword) {
        AlarmQueryDTO dto = new AlarmQueryDTO();
        dto.setDeviceId(deviceId);
        dto.setReadStatus(readStatus);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        dto.setKeyword(keyword);
        return ApiResponse.success(alarmService.listAlarms(dto));
    }

    @GetMapping("/unread-count")
    public ApiResponse<AlarmUnreadCountVO> unreadCount() {
        return ApiResponse.success(new AlarmUnreadCountVO(alarmService.countUnread()));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable("id") Long id) {
        alarmService.markRead(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead(@RequestParam(required = false) Long deviceId) {
        alarmService.markAllRead(deviceId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        alarmService.deleteAlarm(id);
        return ApiResponse.success(null);
    }
}
