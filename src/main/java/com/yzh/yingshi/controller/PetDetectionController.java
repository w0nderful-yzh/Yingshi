package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.PetDetectionConfigRequest;
import com.yzh.yingshi.dto.PetDetectionRecordQueryDTO;
import com.yzh.yingshi.dto.PetSafeZoneRequest;
import com.yzh.yingshi.service.PetDetectionService;
import com.yzh.yingshi.vo.PetDetectionConfigVO;
import com.yzh.yingshi.vo.PetDetectionRecordVO;
import com.yzh.yingshi.vo.PetDetectionResultVO;
import com.yzh.yingshi.vo.PetSafeZoneVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pet-detection")
public class PetDetectionController {

    private final PetDetectionService petDetectionService;

    // ==================== 检测配置 ====================

    @PostMapping("/configs")
    public ApiResponse<PetDetectionConfigVO> createConfig(@Valid @RequestBody PetDetectionConfigRequest request) {
        return ApiResponse.success(petDetectionService.createConfig(request));
    }

    @PutMapping("/configs/{id}")
    public ApiResponse<PetDetectionConfigVO> updateConfig(@PathVariable Long id,
                                                           @Valid @RequestBody PetDetectionConfigRequest request) {
        return ApiResponse.success(petDetectionService.updateConfig(id, request));
    }

    @DeleteMapping("/configs/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        petDetectionService.deleteConfig(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/configs/{id}")
    public ApiResponse<PetDetectionConfigVO> getConfig(@PathVariable Long id) {
        return ApiResponse.success(petDetectionService.getConfigById(id));
    }

    @GetMapping("/configs")
    public ApiResponse<List<PetDetectionConfigVO>> listConfigs() {
        return ApiResponse.success(petDetectionService.listConfigs());
    }

    // ==================== 安全区域 ====================

    @PostMapping("/zones")
    public ApiResponse<PetSafeZoneVO> createSafeZone(@Valid @RequestBody PetSafeZoneRequest request) {
        return ApiResponse.success(petDetectionService.createSafeZone(request));
    }

    @PutMapping("/zones/{id}")
    public ApiResponse<PetSafeZoneVO> updateSafeZone(@PathVariable Long id,
                                                      @Valid @RequestBody PetSafeZoneRequest request) {
        return ApiResponse.success(petDetectionService.updateSafeZone(id, request));
    }

    @DeleteMapping("/zones/{id}")
    public ApiResponse<Void> deleteSafeZone(@PathVariable Long id) {
        petDetectionService.deleteSafeZone(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/zones")
    public ApiResponse<List<PetSafeZoneVO>> listSafeZones(@RequestParam Long detectionConfigId) {
        return ApiResponse.success(petDetectionService.listSafeZones(detectionConfigId));
    }

    // ==================== 检测记录 ====================

    @GetMapping("/records")
    public ApiResponse<List<PetDetectionRecordVO>> listRecords(
            @RequestParam(required = false) Long detectionConfigId,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Integer alarmTriggered,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        PetDetectionRecordQueryDTO dto = new PetDetectionRecordQueryDTO();
        dto.setDetectionConfigId(detectionConfigId);
        dto.setPetId(petId);
        dto.setDeviceId(deviceId);
        dto.setAlarmTriggered(alarmTriggered);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        return ApiResponse.success(petDetectionService.listRecords(dto));
    }

    // ==================== 手动触发检测 ====================

    @PostMapping("/configs/{id}/detect")
    public ApiResponse<PetDetectionResultVO> triggerDetection(@PathVariable Long id) {
        return ApiResponse.success(petDetectionService.triggerDetection(id));
    }
}
