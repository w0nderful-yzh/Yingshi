package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.dto.DeviceCreateRequest;
import com.yzh.yingshi.dto.DeviceUpdateRequest;
import com.yzh.yingshi.service.DeviceService;
import com.yzh.yingshi.vo.ConnectivityTestVO;
import com.yzh.yingshi.vo.DeviceVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceServiceImpl implements DeviceService {
    @Override
    public DeviceVO create(DeviceCreateRequest request) {
        return new DeviceVO();
    }

    @Override
    public List<DeviceVO> list(String keyword, String status) {
        return List.of();
    }

    @Override
    public DeviceVO detail(Long id) {
        return new DeviceVO();
    }

    @Override
    public Boolean update(Long id, DeviceUpdateRequest request) {
        return Boolean.TRUE;
    }

    @Override
    public Boolean delete(Long id) {
        return Boolean.TRUE;
    }

    @Override
    public ConnectivityTestVO testConnectivity(Long id) {
        ConnectivityTestVO vo = new ConnectivityTestVO();
        vo.setReachable(Boolean.FALSE);
        vo.setLatencyMs(0);
        return vo;
    }
}

