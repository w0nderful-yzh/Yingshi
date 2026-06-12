package com.yzh.yingshi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.auth.CurrentUserService;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.constant.AlarmConstant;
import com.yzh.yingshi.dto.AlarmQueryDTO;
import com.yzh.yingshi.dto.AlarmSyncResultDTO;
import com.yzh.yingshi.entity.AlarmMessage;
import com.yzh.yingshi.entity.Device;
import com.yzh.yingshi.entity.UserDevice;
import com.yzh.yingshi.mapper.AlarmMessageMapper;
import com.yzh.yingshi.mapper.DeviceMapper;
import com.yzh.yingshi.mapper.UserDeviceMapper;
import com.yzh.yingshi.service.AlarmService;
import com.yzh.yingshi.service.AlarmSseService;
import com.yzh.yingshi.service.EzvizAlarmService;
import com.yzh.yingshi.vo.AlarmMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    private final DeviceMapper deviceMapper;
    private final AlarmMessageMapper alarmMessageMapper;
    private final UserDeviceMapper userDeviceMapper;
    private final EzvizAlarmService ezvizAlarmService;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final AlarmSseService alarmSseService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public AlarmSyncResultDTO syncFromEzviz() {
        log.info("开始同步萤石告警");

        LambdaQueryWrapper<Device> query = new LambdaQueryWrapper<Device>()
                .eq(Device::getSourceType, "EZVIZ")
                .eq(Device::getDeleted, 0);
        List<Device> devices = deviceMapper.selectList(query);
        List<AlarmSyncTarget> targets;
        if (currentUserService.hasAuthenticatedUser()) {
            Long userId = currentUserService.requireCurrentUserId();
            Set<Long> authorizedDeviceIds = currentUserService.getAuthorizedDeviceIds();
            if (authorizedDeviceIds.isEmpty()) {
                return new AlarmSyncResultDTO(0, 0, 0, "暂无可同步设备");
            }
            targets = devices.stream()
                    .filter(device -> authorizedDeviceIds.contains(device.getId()))
                    .map(device -> new AlarmSyncTarget(userId, device))
                    .collect(Collectors.toList());
        } else {
            Map<String, Device> deviceBySerial = devices.stream()
                    .filter(device -> StringUtils.hasText(device.getDeviceSerial()))
                    .collect(Collectors.toMap(
                            Device::getDeviceSerial,
                            device -> device,
                            (first, ignored) -> first));
            targets = userDeviceMapper.selectList(
                            new LambdaQueryWrapper<UserDevice>().eq(UserDevice::getStatus, 1))
                    .stream()
                    .filter(binding -> deviceBySerial.containsKey(binding.getDeviceSerial()))
                    .map(binding -> new AlarmSyncTarget(
                            binding.getUserId(), deviceBySerial.get(binding.getDeviceSerial())))
                    .distinct()
                    .collect(Collectors.toList());
            if (targets.isEmpty()) {
                targets = devices.stream()
                        .map(device -> new AlarmSyncTarget(null, device))
                        .collect(Collectors.toList());
            }
        }

        long end = System.currentTimeMillis();
        long start = end - AlarmConstant.SYNC_LOOKBACK_MINUTES * 60 * 1000;

        int fetched = 0;
        int inserted = 0;

        for (AlarmSyncTarget target : targets) {
            Device device = target.device();
            if ("DISABLED".equals(device.getStatus())) {
                continue;
            }

            try {
                List<Map<String, Object>> alarms = target.userId() == null
                        ? ezvizAlarmService.listDeviceAlarms(device.getDeviceSerial(), start, end)
                        : ezvizAlarmService.listDeviceAlarmsForUser(
                                target.userId(), device.getDeviceSerial(), start, end);

                fetched += alarms.size();

                for (Map<String, Object> raw : alarms) {
                    if (saveIfAbsent(device, raw, null)) {
                        inserted++;
                    }
                }
            } catch (Exception e) {
                log.warn("同步设备告警失败 deviceId={}, deviceSerial={}, error={}",
                        device.getId(), device.getDeviceSerial(), e.getMessage());
            }
        }

        log.info("萤石告警同步完成 targetCount={}, fetched={}, inserted={}", targets.size(), fetched, inserted);
        return new AlarmSyncResultDTO(targets.size(), fetched, inserted, "同步完成");
    }

    @Override
    @Transactional
    public boolean receiveEzvizWebhook(Map<String, Object> raw, String rawJson) {
        String deviceSerial = extractText(raw, "deviceSerial", "deviceId");
        if (!StringUtils.hasText(deviceSerial)) {
            log.warn("忽略缺少设备序列号的萤石推送");
            return false;
        }

        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceSerial, deviceSerial)
                        .eq(Device::getDeleted, 0)
                        .last("LIMIT 1"));
        if (device == null) {
            log.warn("忽略未同步到本地的萤石推送 deviceSerial={}", deviceSerial);
            return false;
        }

        Long activeBindings = userDeviceMapper.selectCount(
                new LambdaQueryWrapper<UserDevice>()
                        .eq(UserDevice::getDeviceSerial, deviceSerial)
                        .eq(UserDevice::getStatus, 1));
        if (activeBindings == null || activeBindings == 0) {
            log.warn("忽略没有有效用户绑定的萤石推送 deviceSerial={}", deviceSerial);
            return false;
        }

        return saveIfAbsent(device, raw, rawJson);
    }

    private boolean saveIfAbsent(Device device, Map<String, Object> raw, String rawJson) {
        String alarmId = extractText(raw, "alarmId", "id", "uuid", "alarm_id");
        String alarmType = extractText(raw, "alarmType", "type", "alarm_type");
        String alarmName = extractText(raw, "alarmName", "alarm_name", "name");
        String alarmPicUrl = extractText(raw, "alarmPicUrl", "picUrl", "imageUrl", "pictureUrl", "pic_url");
        String alarmContent = extractText(raw, "alarmContent", "content", "message", "msg");
        LocalDateTime alarmTime = parseAlarmTime(raw);

        String deviceSerial = device.getDeviceSerial();

        // 去重
        LambdaQueryWrapper<AlarmMessage> dupQuery = new LambdaQueryWrapper<>();
        dupQuery.eq(AlarmMessage::getDeviceSerial, deviceSerial);
        if (StringUtils.hasText(alarmId)) {
            dupQuery.eq(AlarmMessage::getAlarmId, alarmId);
        } else {
            dupQuery.eq(AlarmMessage::getAlarmType, alarmType)
                    .eq(AlarmMessage::getAlarmTime, alarmTime);
        }
        Long exists = alarmMessageMapper.selectCount(dupQuery);
        if (exists != null && exists > 0) {
            return false;
        }

        AlarmMessage entity = new AlarmMessage();
        entity.setDeviceId(device.getId());
        entity.setDeviceSerial(deviceSerial);
        entity.setChannelNo(extractInteger(raw, "channelNo", "channel")
                .orElse(device.getChannelNo() != null
                        ? device.getChannelNo()
                        : AlarmConstant.DEFAULT_CHANNEL_NO));
        entity.setAlarmId(alarmId);
        entity.setAlarmType(StringUtils.hasText(alarmType) ? alarmType : "unknown");
        entity.setAlarmName(StringUtils.hasText(alarmName) ? alarmName : entity.getAlarmType());
        entity.setAlarmTime(alarmTime);
        entity.setAlarmPicUrl(alarmPicUrl);
        entity.setAlarmContent(alarmContent);
        entity.setReadStatus(AlarmConstant.READ_STATUS_UNREAD);
        entity.setSource(AlarmConstant.SOURCE_EZVIZ);
        entity.setDeleted(AlarmConstant.DELETED_NO);

        if (StringUtils.hasText(rawJson)) {
            entity.setRawJson(rawJson);
        } else {
            try {
                entity.setRawJson(objectMapper.writeValueAsString(raw));
            } catch (Exception e) {
                entity.setRawJson(null);
            }
        }

        try {
            alarmMessageMapper.insert(entity);
            // SSE推送新告警
            alarmSseService.broadcastAlarm(entity);
            return true;
        } catch (DuplicateKeyException e) {
            log.debug("告警插入跳过(可能重复) deviceSerial={}, alarmType={}, alarmTime={}", deviceSerial, alarmType, alarmTime);
            return false;
        }
    }

    @Override
    public List<AlarmMessageVO> listAlarms(AlarmQueryDTO dto) {
        Set<Long> authorizedDeviceIds = currentUserService.getAuthorizedDeviceIds();
        if (authorizedDeviceIds.isEmpty()) {
            return new ArrayList<>();
        }
        if (dto.getDeviceId() != null) {
            currentUserService.assertDeviceAccessible(dto.getDeviceId());
        }

        LambdaQueryWrapper<AlarmMessage> query = new LambdaQueryWrapper<>();
        query.in(AlarmMessage::getDeviceId, authorizedDeviceIds);

        if (dto.getDeviceId() != null) {
            query.eq(AlarmMessage::getDeviceId, dto.getDeviceId());
        }
        if (dto.getReadStatus() != null) {
            query.eq(AlarmMessage::getReadStatus, dto.getReadStatus());
        }
        if (StringUtils.hasText(dto.getStartTime())) {
            query.ge(AlarmMessage::getAlarmTime, LocalDateTime.parse(dto.getStartTime(), DT_FMT));
        }
        if (StringUtils.hasText(dto.getEndTime())) {
            query.le(AlarmMessage::getAlarmTime, LocalDateTime.parse(dto.getEndTime(), DT_FMT));
        }
        if (StringUtils.hasText(dto.getKeyword())) {
            query.and(q -> q.like(AlarmMessage::getAlarmName, dto.getKeyword())
                    .or()
                    .like(AlarmMessage::getAlarmContent, dto.getKeyword()));
        }

        query.orderByDesc(AlarmMessage::getAlarmTime)
                .orderByDesc(AlarmMessage::getCreatedAt);

        List<AlarmMessage> alarms = alarmMessageMapper.selectList(query);
        if (alarms.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查 device name
        Map<Long, String> deviceNameMap = buildDeviceNameMap(alarms);

        return alarms.stream()
                .map(a -> toVO(a, deviceNameMap.get(a.getDeviceId()), false))
                .collect(Collectors.toList());
    }

    @Override
    public AlarmMessageVO getAlarmDetail(Long id) {
        AlarmMessage alarm = alarmMessageMapper.selectById(id);
        if (alarm == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "告警不存在");
        }
        assertAlarmAccessible(alarm);

        String deviceName = null;
        if (alarm.getDeviceId() != null) {
            Device device = deviceMapper.selectById(alarm.getDeviceId());
            if (device != null) {
                deviceName = device.getDeviceName();
            }
        }
        return toVO(alarm, deviceName, true);
    }

    @Override
    public long countUnread() {
        Set<Long> authorizedDeviceIds = currentUserService.getAuthorizedDeviceIds();
        if (authorizedDeviceIds.isEmpty()) {
            return 0L;
        }
        LambdaQueryWrapper<AlarmMessage> query = new LambdaQueryWrapper<AlarmMessage>()
                .eq(AlarmMessage::getReadStatus, AlarmConstant.READ_STATUS_UNREAD)
                .in(AlarmMessage::getDeviceId, authorizedDeviceIds);
        Long count = alarmMessageMapper.selectCount(query);
        return count != null ? count : 0L;
    }

    @Override
    public void markRead(Long id) {
        AlarmMessage alarm = alarmMessageMapper.selectById(id);
        if (alarm == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "告警不存在");
        }
        assertAlarmAccessible(alarm);
        alarm.setReadStatus(AlarmConstant.READ_STATUS_READ);
        alarmMessageMapper.updateById(alarm);
    }

    @Override
    public void markAllRead(Long deviceId) {
        Set<Long> authorizedDeviceIds = currentUserService.getAuthorizedDeviceIds();
        if (authorizedDeviceIds.isEmpty()) {
            return;
        }
        if (deviceId != null) {
            currentUserService.assertDeviceAccessible(deviceId);
        }
        LambdaUpdateWrapper<AlarmMessage> update = new LambdaUpdateWrapper<AlarmMessage>()
                .eq(AlarmMessage::getReadStatus, AlarmConstant.READ_STATUS_UNREAD)
                .in(AlarmMessage::getDeviceId, authorizedDeviceIds)
                .set(AlarmMessage::getReadStatus, AlarmConstant.READ_STATUS_READ);
        if (deviceId != null) {
            update.eq(AlarmMessage::getDeviceId, deviceId);
        }
        alarmMessageMapper.update(null, update);
    }

    @Override
    public void deleteAlarm(Long id) {
        AlarmMessage alarm = alarmMessageMapper.selectById(id);
        if (alarm == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "告警不存在");
        }
        assertAlarmAccessible(alarm);
        alarmMessageMapper.deleteById(id);
    }

    private Map<Long, String> buildDeviceNameMap(List<AlarmMessage> alarms) {
        List<Long> deviceIds = alarms.stream()
                .map(AlarmMessage::getDeviceId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> map = new HashMap<>();
        if (!deviceIds.isEmpty()) {
            List<Device> devices = deviceMapper.selectBatchIds(deviceIds);
            for (Device d : devices) {
                map.put(d.getId(), d.getDeviceName());
            }
        }
        return map;
    }

    private AlarmMessageVO toVO(AlarmMessage a, String deviceName, boolean includeDetailFields) {
        AlarmMessageVO vo = new AlarmMessageVO();
        vo.setId(a.getId());
        vo.setDeviceId(a.getDeviceId());
        vo.setDeviceSerial(a.getDeviceSerial());
        vo.setDeviceName(deviceName);
        vo.setChannelNo(a.getChannelNo());
        vo.setAlarmType(a.getAlarmType());
        vo.setAlarmName(a.getAlarmName());
        vo.setAlarmTime(a.getAlarmTime());
        vo.setAlarmPicUrl(a.getAlarmPicUrl());
        vo.setAlarmContent(a.getAlarmContent());
        vo.setReadStatus(a.getReadStatus());
        vo.setSource(a.getSource());
        vo.setCreatedAt(a.getCreatedAt());
        if (includeDetailFields) {
            vo.setAlarmId(a.getAlarmId());
            vo.setRawJson(a.getRawJson());
            vo.setUpdatedAt(a.getUpdatedAt());
        }
        return vo;
    }

    private void assertAlarmAccessible(AlarmMessage alarm) {
        if (alarm.getDeviceId() != null) {
            currentUserService.assertDeviceAccessible(alarm.getDeviceId());
            return;
        }
        Set<String> authorizedSerials = currentUserService.getAuthorizedDeviceSerials();
        if (alarm.getDeviceSerial() == null || !authorizedSerials.contains(alarm.getDeviceSerial())) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "无权访问该告警");
        }
    }

    private LocalDateTime parseAlarmTime(Map<String, Object> raw) {
        Object timeVal = raw.get("alarmTime");
        if (timeVal == null) timeVal = raw.get("time");
        if (timeVal == null) timeVal = raw.get("createTime");
        if (timeVal == null) timeVal = raw.get("alarm_time");
        if (timeVal == null) timeVal = raw.get("create_time");

        if (timeVal == null) {
            return LocalDateTime.now();
        }

        String timeStr = String.valueOf(timeVal);

        try {
            if (timeStr.matches("\\d{13}")) {
                return Instant.ofEpochMilli(Long.parseLong(timeStr))
                        .atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            if (timeStr.matches("\\d{10}")) {
                return Instant.ofEpochSecond(Long.parseLong(timeStr))
                        .atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            return LocalDateTime.parse(timeStr, DT_FMT);
        } catch (Exception e) {
            log.debug("告警时间解析失败, 使用当前时间: {}", timeStr);
            return LocalDateTime.now();
        }
    }

    private String extractText(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null && !val.toString().isBlank()) {
                return val.toString();
            }
        }
        return null;
    }

    private java.util.Optional<Integer> extractInteger(Map<String, Object> map, String... keys) {
        String value = extractText(map, keys);
        if (!StringUtils.hasText(value)) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }

    private record AlarmSyncTarget(Long userId, Device device) {
    }
}
