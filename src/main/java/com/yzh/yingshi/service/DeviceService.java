package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.DeviceSyncResultDTO;
import com.yzh.yingshi.dto.DeviceUpdateDTO;
import com.yzh.yingshi.vo.DeviceVO;

import java.util.List;

public interface DeviceService {

    DeviceSyncResultDTO syncFromEzviz();

    List<DeviceVO> listDevices(String status, String sourceType, String keyword);

    DeviceVO getDeviceById(Long id);

    DeviceVO updateDevice(Long id, DeviceUpdateDTO dto);

    void disableDevice(Long id);

    void enableDevice(Long id);

    void deleteDevice(Long id);
}
