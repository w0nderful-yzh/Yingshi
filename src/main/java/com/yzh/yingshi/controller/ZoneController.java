package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.SaveDeviceZonesRequest;
import com.yzh.yingshi.service.ZoneService;
import com.yzh.yingshi.vo.SavedCountVO;
import com.yzh.yingshi.vo.ZoneVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ZoneController {

    private final ZoneService zoneService;

    @PostMapping("/devices/{id}/zones")
    public ApiResponse<SavedCountVO> saveZones(@PathVariable("id") Long deviceId,
                                               @Valid @RequestBody SaveDeviceZonesRequest request) {
        return ApiResponse.success(zoneService.saveZones(deviceId, request));
    }

    @GetMapping("/devices/{id}/zones")
    public ApiResponse<List<ZoneVO>> listZones(@PathVariable("id") Long deviceId) {
        return ApiResponse.success(zoneService.listZones(deviceId));
    }

    @DeleteMapping("/zones/{zoneId}")
    public ApiResponse<Boolean> deleteZone(@PathVariable("zoneId") Long zoneId) {
        return ApiResponse.success(zoneService.deleteZone(zoneId));
    }
}


