package com.yzh.yingshi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.dto.DeviceSyncResultDTO;
import com.yzh.yingshi.dto.DeviceUpdateDTO;
import com.yzh.yingshi.entity.Device;
import com.yzh.yingshi.mapper.DeviceMapper;
import com.yzh.yingshi.service.DeviceService;
import com.yzh.yingshi.service.EzvizDeviceService;
import com.yzh.yingshi.vo.DeviceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final EzvizDeviceService ezvizDeviceService;

    private static final Set<String> VALID_STATUSES = Set.of("ONLINE", "OFFLINE", "DISABLED");

    @Override
    public DeviceSyncResultDTO syncFromEzviz() {
        List<JsonNode> ezvizDevices = ezvizDeviceService.listEzvizDevices();

        int total = ezvizDevices.size();
        int inserted = 0;
        int updated = 0;

        for (JsonNode ezvizDevice : ezvizDevices) {
            String deviceSerial = ezvizDevice.get("deviceSerial").asText();
            String deviceName = ezvizDevice.get("deviceName").asText();
            String deviceType = ezvizDevice.has("deviceType") ? ezvizDevice.get("deviceType").asText() : null;
            int ezvizStatus = ezvizDevice.get("status").asInt();
            String mappedStatus = ezvizStatus == 1 ? "ONLINE" : "OFFLINE";

            QueryWrapper<Device> query = new QueryWrapper<>();
            query.eq("device_serial", deviceSerial);
            Device localDevice = deviceMapper.selectOne(query);

            if (localDevice == null) {
                Device newDevice = new Device();
                newDevice.setDeviceSerial(deviceSerial);
                newDevice.setChannelNo(1);
                newDevice.setDeviceName(deviceName);
                newDevice.setDeviceType(deviceType);
                newDevice.setSourceType("EZVIZ");
                newDevice.setStatus(mappedStatus);
                newDevice.setStreamUrl(null);
                newDevice.setRemark(null);
                newDevice.setDeleted(0);
                deviceMapper.insert(newDevice);
                inserted++;
            } else {
                localDevice.setDeviceName(deviceName);
                localDevice.setDeviceType(deviceType);
                localDevice.setChannelNo(1);
                localDevice.setSourceType("EZVIZ");

                if (!"DISABLED".equals(localDevice.getStatus())) {
                    localDevice.setStatus(mappedStatus);
                }

                deviceMapper.updateById(localDevice);
                updated++;
            }
        }

        log.info("萤石设备同步完成, total={}, inserted={}, updated={}", total, inserted, updated);
        return new DeviceSyncResultDTO(total, inserted, updated, "同步成功");
    }

    @Override
    public List<DeviceVO> listDevices(String status, String sourceType, String keyword) {
        QueryWrapper<Device> query = new QueryWrapper<>();

        if (StringUtils.hasText(status)) {
            query.eq("status", status);
        }
        if (StringUtils.hasText(sourceType)) {
            query.eq("source_type", sourceType);
        }
        if (StringUtils.hasText(keyword)) {
            query.and(q -> q.like("device_name", keyword)
                    .or()
                    .like("device_serial", keyword));
        }

        query.orderByDesc("updated_at");

        List<Device> devices = deviceMapper.selectList(query);
        return devices.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public DeviceVO getDeviceById(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "设备不存在");
        }
        return toVO(device);
    }

    @Override
    public DeviceVO updateDevice(Long id, DeviceUpdateDTO dto) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "设备不存在");
        }

        if (StringUtils.hasText(dto.getStatus()) && !VALID_STATUSES.contains(dto.getStatus())) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "status 只允许 ONLINE / OFFLINE / DISABLED");
        }

        device.setDeviceName(dto.getDeviceName());
        if (dto.getRemark() != null) {
            device.setRemark(dto.getRemark());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            device.setStatus(dto.getStatus());
        }

        deviceMapper.updateById(device);
        return toVO(device);
    }

    @Override
    public void disableDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "设备不存在");
        }
        device.setStatus("DISABLED");
        deviceMapper.updateById(device);
    }

    @Override
    public void enableDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "设备不存在");
        }
        device.setStatus("OFFLINE");
        deviceMapper.updateById(device);
    }

    @Override
    public void deleteDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "设备不存在");
        }
        deviceMapper.deleteById(id);
    }

    private DeviceVO toVO(Device device) {
        DeviceVO vo = new DeviceVO();
        vo.setId(device.getId());
        vo.setDeviceSerial(device.getDeviceSerial());
        vo.setChannelNo(device.getChannelNo());
        vo.setDeviceName(device.getDeviceName());
        vo.setDeviceType(device.getDeviceType());
        vo.setSourceType(device.getSourceType());
        vo.setStatus(device.getStatus());
        vo.setRemark(device.getRemark());
        vo.setCreatedAt(device.getCreatedAt());
        vo.setUpdatedAt(device.getUpdatedAt());
        return vo;
    }
}
