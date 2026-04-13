package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.SaveDeviceZonesRequest;
import com.yzh.yingshi.vo.SavedCountVO;
import com.yzh.yingshi.vo.ZoneVO;

import java.util.List;

public interface ZoneService {
    SavedCountVO saveZones(Long deviceId, SaveDeviceZonesRequest request);

    List<ZoneVO> listZones(Long deviceId);

    Boolean deleteZone(Long zoneId);
}

