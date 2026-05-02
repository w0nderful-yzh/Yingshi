package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.DeviceSyncResultDTO;
import com.yzh.yingshi.dto.DeviceUpdateDTO;
import com.yzh.yingshi.service.DeviceService;
import com.yzh.yingshi.vo.DeviceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/sync")
    public ApiResponse<DeviceSyncResultDTO> sync() {
        return ApiResponse.success(deviceService.syncFromEzviz());
    }

    @GetMapping
    public ApiResponse<List<DeviceVO>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(deviceService.listDevices(status, sourceType, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceVO> detail(@PathVariable("id") Long id) {
        return ApiResponse.success(deviceService.getDeviceById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<DeviceVO> update(@PathVariable("id") Long id,
                                        @Valid @RequestBody DeviceUpdateDTO dto) {
        return ApiResponse.success(deviceService.updateDevice(id, dto));
    }

    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable("id") Long id) {
        deviceService.disableDevice(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable("id") Long id) {
        deviceService.enableDevice(id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        deviceService.deleteDevice(id);
        return ApiResponse.success(null);
    }
}
