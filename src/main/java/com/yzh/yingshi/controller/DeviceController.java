package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.DeviceCreateRequest;
import com.yzh.yingshi.dto.DeviceUpdateRequest;
import com.yzh.yingshi.service.DeviceService;
import com.yzh.yingshi.vo.ConnectivityTestVO;
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

    @PostMapping
    public ApiResponse<DeviceVO> create(@Valid @RequestBody DeviceCreateRequest request) {
        return ApiResponse.success(deviceService.create(request));
    }

    @GetMapping
    public ApiResponse<List<DeviceVO>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.success(deviceService.list(keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceVO> detail(@PathVariable("id") Long id) {
        return ApiResponse.success(deviceService.detail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") Long id, @Valid @RequestBody DeviceUpdateRequest request) {
        return ApiResponse.success(deviceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable("id") Long id) {
        return ApiResponse.success(deviceService.delete(id));
    }

    @PostMapping("/{id}/connectivity-test")
    public ApiResponse<ConnectivityTestVO> testConnectivity(@PathVariable("id") Long id) {
        return ApiResponse.success(deviceService.testConnectivity(id));
    }
}


