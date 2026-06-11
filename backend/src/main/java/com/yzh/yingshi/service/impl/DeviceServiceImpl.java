package com.yzh.yingshi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.auth.CurrentUserService;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.dto.DeviceSyncResultDTO;
import com.yzh.yingshi.dto.DeviceUpdateDTO;
import com.yzh.yingshi.entity.Device;
import com.yzh.yingshi.entity.UserDevice;
import com.yzh.yingshi.mapper.DeviceMapper;
import com.yzh.yingshi.mapper.UserDeviceMapper;
import com.yzh.yingshi.service.DeviceService;
import com.yzh.yingshi.service.EzvizDeviceService;
import com.yzh.yingshi.service.EzvizOAuthService;
import com.yzh.yingshi.vo.DeviceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final UserDeviceMapper userDeviceMapper;
    private final EzvizDeviceService ezvizDeviceService;
    private final EzvizOAuthService ezvizOAuthService;
    private final CurrentUserService currentUserService;

    private static final Set<String> VALID_STATUSES = Set.of("ONLINE", "OFFLINE", "DISABLED");

    @Override
    public DeviceSyncResultDTO syncFromEzviz() {
        Long userId = currentUserService.requireCurrentUserId();
        List<JsonNode> ezvizDevices;

        if (ezvizOAuthService.hasOAuthAccount(userId)) {
            String userToken = ezvizOAuthService.getUserAccessToken(userId);
            if (!StringUtils.hasText(userToken)) {
                throw new BusinessException(BusinessCode.STATUS_CONFLICT, "萤石授权已失效，请重新绑定");
            }
            ezvizDevices = ezvizDeviceService.listEzvizDevicesByToken(userToken);
            if (ezvizDevices == null) {
                userToken = ezvizOAuthService.refreshToken(userId);
                ezvizDevices = StringUtils.hasText(userToken)
                        ? ezvizDeviceService.listEzvizDevicesByToken(userToken)
                        : null;
            }
        } else if ("ADMIN".equals(currentUserService.requireCurrentRole())) {
            // 管理员首次部署时可直接导入当前开放平台应用下的设备。
            ezvizDevices = ezvizDeviceService.listEzvizDevicesAppLevel();
        } else {
            throw new BusinessException(BusinessCode.STATUS_CONFLICT, "请先绑定萤石账号后再同步");
        }

        if (ezvizDevices == null) {
            throw new BusinessException(BusinessCode.INTERNAL_ERROR, "获取萤石设备列表失败");
        }

        int total = 0;
        int inserted = 0;
        int updated = 0;

        for (JsonNode ezvizDevice : ezvizDevices) {
            String deviceSerial = textValue(ezvizDevice, "deviceSerial");
            if (!StringUtils.hasText(deviceSerial)) {
                log.warn("忽略缺少 deviceSerial 的萤石设备数据: {}", ezvizDevice);
                continue;
            }

            total++;
            String deviceName = textValue(ezvizDevice, "deviceName");
            if (!StringUtils.hasText(deviceName)) {
                deviceName = deviceSerial;
            }
            String deviceType = textValue(ezvizDevice, "deviceType");
            int channelNo = positiveIntValue(ezvizDevice, "channelNum", 1);
            int ezvizStatus = ezvizDevice.path("status").asInt(0);
            String mappedStatus = ezvizStatus == 1 ? "ONLINE" : "OFFLINE";

            upsertUserBinding(userId, deviceSerial, deviceName, deviceType, channelNo);

            QueryWrapper<Device> query = new QueryWrapper<>();
            query.eq("device_serial", deviceSerial);
            Device localDevice = deviceMapper.selectOne(query);

            if (localDevice == null) {
                Device newDevice = new Device();
                newDevice.setDeviceSerial(deviceSerial);
                newDevice.setChannelNo(channelNo);
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
                localDevice.setChannelNo(channelNo);
                localDevice.setSourceType("EZVIZ");

                if (!"DISABLED".equals(localDevice.getStatus())) {
                    localDevice.setStatus(mappedStatus);
                }

                deviceMapper.updateById(localDevice);
                updated++;
            }
        }

        if (total == 0) {
            throw new BusinessException(
                    BusinessCode.RESOURCE_NOT_FOUND,
                    "萤石账号下未找到可用设备，请确认摄像头已添加到当前萤石账号或开放平台应用");
        }

        log.info("萤石设备同步完成, total={}, inserted={}, updated={}", total, inserted, updated);
        return new DeviceSyncResultDTO(total, inserted, updated,
                String.format("同步成功，共发现 %d 台设备", total));
    }

    @Override
    public List<DeviceVO> listDevices(String status, String sourceType, String keyword) {
        Set<Long> authorizedDeviceIds = currentUserService.getAuthorizedDeviceIds();
        if (authorizedDeviceIds.isEmpty()) {
            return List.of();
        }

        QueryWrapper<Device> query = new QueryWrapper<>();
        query.in("id", authorizedDeviceIds);

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
        currentUserService.assertDeviceAccessible(device);
        return toVO(device);
    }

    @Override
    public DeviceVO updateDevice(Long id, DeviceUpdateDTO dto) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "设备不存在");
        }
        currentUserService.assertDeviceAccessible(device);

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
        currentUserService.assertDeviceAccessible(device);
        device.setStatus("DISABLED");
        deviceMapper.updateById(device);
    }

    @Override
    public void enableDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "设备不存在");
        }
        currentUserService.assertDeviceAccessible(device);
        device.setStatus("OFFLINE");
        deviceMapper.updateById(device);
    }

    @Override
    public void deleteDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "设备不存在");
        }
        currentUserService.assertDeviceAccessible(device);
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

    private void upsertUserBinding(Long userId,
                                   String deviceSerial,
                                   String deviceName,
                                   String deviceType,
                                   int channelNo) {
        UserDevice binding = userDeviceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserDevice>()
                        .eq(UserDevice::getUserId, userId)
                        .eq(UserDevice::getDeviceSerial, deviceSerial));
        if (binding == null) {
            binding = new UserDevice();
            binding.setUserId(userId);
            binding.setDeviceSerial(deviceSerial);
        }
        binding.setDeviceName(deviceName);
        binding.setDeviceType(deviceType);
        binding.setChannelNo(channelNo);
        binding.setBoundAt(LocalDateTime.now());
        binding.setStatus(1);

        if (binding.getId() == null) {
            userDeviceMapper.insert(binding);
        } else {
            userDeviceMapper.updateById(binding);
        }
    }

    private String textValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private int positiveIntValue(JsonNode node, String field, int defaultValue) {
        int value = node.path(field).asInt(defaultValue);
        return value > 0 ? value : defaultValue;
    }
}
