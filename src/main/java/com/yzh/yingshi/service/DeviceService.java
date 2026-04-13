package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.DeviceCreateRequest;
import com.yzh.yingshi.dto.DeviceUpdateRequest;
import com.yzh.yingshi.vo.ConnectivityTestVO;
import com.yzh.yingshi.vo.DeviceVO;

import java.util.List;

public interface DeviceService {
    DeviceVO create(DeviceCreateRequest request);

    List<DeviceVO> list(String keyword, String status);

    DeviceVO detail(Long id);

    Boolean update(Long id, DeviceUpdateRequest request);

    Boolean delete(Long id);

    ConnectivityTestVO testConnectivity(Long id);
}

