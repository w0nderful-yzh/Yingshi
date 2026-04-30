package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.CloudPlaybackUrlRequestDTO;
import com.yzh.yingshi.dto.CloudRecordQueryDTO;
import com.yzh.yingshi.dto.LiveUrlRequestDTO;
import com.yzh.yingshi.service.VideoService;
import com.yzh.yingshi.vo.CloudPlaybackUrlVO;
import com.yzh.yingshi.vo.CloudRecordFileVO;
import com.yzh.yingshi.vo.LiveUrlVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/video")
public class VideoController {

    private final VideoService videoService;

    @GetMapping("/live-url")
    public ApiResponse<LiveUrlVO> getLiveUrl(
            @RequestParam Long deviceId,
            @RequestParam(required = false) Integer protocol,
            @RequestParam(required = false) Integer quality,
            @RequestParam(required = false) Integer expireTime) {
        LiveUrlRequestDTO dto = new LiveUrlRequestDTO();
        dto.setDeviceId(deviceId);
        dto.setProtocol(protocol);
        dto.setQuality(quality);
        dto.setExpireTime(expireTime);
        return ApiResponse.success(videoService.getLiveUrl(dto));
    }

    @GetMapping("/cloud/records")
    public ApiResponse<List<CloudRecordFileVO>> listCloudRecords(
            @RequestParam Long deviceId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        CloudRecordQueryDTO dto = new CloudRecordQueryDTO();
        dto.setDeviceId(deviceId);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        return ApiResponse.success(videoService.listCloudRecords(dto));
    }

    @GetMapping("/cloud/playback-url")
    public ApiResponse<CloudPlaybackUrlVO> getCloudPlaybackUrl(
            @RequestParam Long deviceId,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) Integer protocol,
            @RequestParam(required = false) Integer quality,
            @RequestParam(required = false) Integer expireTime) {
        CloudPlaybackUrlRequestDTO dto = new CloudPlaybackUrlRequestDTO();
        dto.setDeviceId(deviceId);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        dto.setProtocol(protocol);
        dto.setQuality(quality);
        dto.setExpireTime(expireTime);
        return ApiResponse.success(videoService.getCloudPlaybackUrl(dto));
    }
}
