package com.yzh.yingshi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.constant.AlarmConstant;
import com.yzh.yingshi.dto.AlarmQueryDTO;
import com.yzh.yingshi.dto.AlarmSyncResultDTO;
import com.yzh.yingshi.entity.AlarmMessage;
import com.yzh.yingshi.entity.Device;
import com.yzh.yingshi.mapper.AlarmMessageMapper;
import com.yzh.yingshi.mapper.DeviceMapper;
import com.yzh.yingshi.service.AlarmService;
import com.yzh.yingshi.service.EzvizAlarmService;
import com.yzh.yingshi.vo.AlarmMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    private final DeviceMapper deviceMapper;
    private final AlarmMessageMapper alarmMessageMapper;
    private final EzvizAlarmService ezvizAlarmService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public AlarmSyncResultDTO syncFromEzviz() {
        log.info("开始同步萤石告警");

        LambdaQueryWrapper<Device> query = new LambdaQueryWrapper<Device>()
                .eq(Device::getSourceType, "EZVIZ")
                .eq(Device::getDeleted, 0);
        List<Device> devices = deviceMapper.selectList(query);

        long end = System.currentTimeMillis();
        long start = end - AlarmConstant.SYNC_LOOKBACK_MINUTES * 60 * 1000;

        int fetched = 0;
        int inserted = 0;

        for (Device device : devices) {
            if ("DISABLED".equals(device.getStatus())) {
                continue;
            }

            try {
                List<Map<String, Object>> alarms = ezvizAlarmService.listDeviceAlarms(
                        device.getDeviceSerial(), start, end);

                fetched += alarms.size();

                for (Map<String, Object> raw : alarms) {
                    if (saveIfAbsent(device, raw)) {
                        inserted++;
                    }
                }
            } catch (Exception e) {
                log.warn("同步设备告警失败 deviceId={}, deviceSerial={}, error={}",
                        device.getId(), device.getDeviceSerial(), e.getMessage());
            }
        }

        log.info("萤石告警同步完成 deviceCount={}, fetched={}, inserted={}", devices.size(), fetched, inserted);
        return new AlarmSyncResultDTO(devices.size(), fetched, inserted, "同步完成");
    }

    private boolean saveIfAbsent(Device device, Map<String, Object> raw) {
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
        entity.setChannelNo(device.getChannelNo() != null ? device.getChannelNo() : AlarmConstant.DEFAULT_CHANNEL_NO);
        entity.setAlarmId(alarmId);
        entity.setAlarmType(alarmType);
        entity.setAlarmName(alarmName);
        entity.setAlarmTime(alarmTime);
        entity.setAlarmPicUrl(alarmPicUrl);
        entity.setAlarmContent(alarmContent);
        entity.setReadStatus(AlarmConstant.READ_STATUS_UNREAD);
        entity.setSource(AlarmConstant.SOURCE_EZVIZ);
        entity.setDeleted(AlarmConstant.DELETED_NO);

        try {
            entity.setRawJson(objectMapper.writeValueAsString(raw));
        } catch (Exception e) {
            entity.setRawJson(null);
        }

        try {
            alarmMessageMapper.insert(entity);
            return true;
        } catch (Exception e) {
            // 唯一索引冲突视为重复
            log.debug("告警插入跳过(可能重复) deviceSerial={}, alarmType={}, alarmTime={}", deviceSerial, alarmType, alarmTime);
            return false;
        }
    }

    @Override
    public List<AlarmMessageVO> listAlarms(AlarmQueryDTO dto) {
        LambdaQueryWrapper<AlarmMessage> query = new LambdaQueryWrapper<>();

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

        return alarms.stream().map(a -> toVO(a, deviceNameMap)).collect(Collectors.toList());
    }

    @Override
    public long countUnread() {
        LambdaQueryWrapper<AlarmMessage> query = new LambdaQueryWrapper<AlarmMessage>()
                .eq(AlarmMessage::getReadStatus, AlarmConstant.READ_STATUS_UNREAD);
        Long count = alarmMessageMapper.selectCount(query);
        return count != null ? count : 0L;
    }

    @Override
    public void markRead(Long id) {
        AlarmMessage alarm = alarmMessageMapper.selectById(id);
        if (alarm == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "告警不存在");
        }
        alarm.setReadStatus(AlarmConstant.READ_STATUS_READ);
        alarmMessageMapper.updateById(alarm);
    }

    @Override
    public void markAllRead(Long deviceId) {
        LambdaUpdateWrapper<AlarmMessage> update = new LambdaUpdateWrapper<AlarmMessage>()
                .eq(AlarmMessage::getReadStatus, AlarmConstant.READ_STATUS_UNREAD)
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

    private AlarmMessageVO toVO(AlarmMessage a, Map<Long, String> deviceNameMap) {
        AlarmMessageVO vo = new AlarmMessageVO();
        vo.setId(a.getId());
        vo.setDeviceId(a.getDeviceId());
        vo.setDeviceSerial(a.getDeviceSerial());
        vo.setDeviceName(deviceNameMap.getOrDefault(a.getDeviceId(), null));
        vo.setChannelNo(a.getChannelNo());
        vo.setAlarmType(a.getAlarmType());
        vo.setAlarmName(a.getAlarmName());
        vo.setAlarmTime(a.getAlarmTime());
        vo.setAlarmPicUrl(a.getAlarmPicUrl());
        vo.setAlarmContent(a.getAlarmContent());
        vo.setReadStatus(a.getReadStatus());
        vo.setSource(a.getSource());
        vo.setCreatedAt(a.getCreatedAt());
        return vo;
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
}
