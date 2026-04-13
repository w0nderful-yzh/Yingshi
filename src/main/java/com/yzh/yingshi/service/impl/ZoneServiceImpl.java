package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.dto.SaveDeviceZonesRequest;
import com.yzh.yingshi.service.ZoneService;
import com.yzh.yingshi.vo.SavedCountVO;
import com.yzh.yingshi.vo.ZoneVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZoneServiceImpl implements ZoneService {
    @Override
    public SavedCountVO saveZones(Long deviceId, SaveDeviceZonesRequest request) {
        SavedCountVO vo = new SavedCountVO();
        vo.setSavedCount(request.getZones().size());
        return vo;
    }

    @Override
    public List<ZoneVO> listZones(Long deviceId) {
        return List.of();
    }

    @Override
    public Boolean deleteZone(Long zoneId) {
        return Boolean.TRUE;
    }
}

