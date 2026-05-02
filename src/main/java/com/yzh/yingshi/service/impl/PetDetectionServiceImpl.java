package com.yzh.yingshi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.constant.AlarmConstant;
import com.yzh.yingshi.constant.PetDetectionConstant;
import com.yzh.yingshi.dto.PetDetectionConfigRequest;
import com.yzh.yingshi.dto.PetDetectionRecordQueryDTO;
import com.yzh.yingshi.dto.PetSafeZoneRequest;
import com.yzh.yingshi.entity.*;
import com.yzh.yingshi.mapper.*;
import com.yzh.yingshi.config.PetDetectionProperties;
import com.yzh.yingshi.service.EzvizSnapshotService;
import com.yzh.yingshi.service.PetAiDetector;
import com.yzh.yingshi.service.PetDetectionService;
import com.yzh.yingshi.vo.PetDetectionConfigVO;
import com.yzh.yingshi.vo.PetDetectionRecordVO;
import com.yzh.yingshi.vo.PetDetectionResultVO;
import com.yzh.yingshi.vo.PetSafeZoneVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetDetectionServiceImpl implements PetDetectionService {

    private final PetDetectionConfigMapper configMapper;
    private final PetSafeZoneMapper safeZoneMapper;
    private final PetDetectionRecordMapper recordMapper;
    private final DeviceMapper deviceMapper;
    private final PetMapper petMapper;
    private final AlarmMessageMapper alarmMessageMapper;
    private final HttpServletRequest request;
    private final EzvizSnapshotService ezvizSnapshotService;
    private final EzvizPetAiDetector ezvizPetAiDetector;
    private final PetDetectionProperties detectionProperties;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 检测配置 CRUD ====================

    @Override
    @Transactional
    public PetDetectionConfigVO createConfig(PetDetectionConfigRequest req) {
        Long userId = getCurrentUserId();

        Pet pet = petMapper.selectById(req.getPetId());
        if (pet == null || !pet.getUserId().equals(userId)) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "宠物不存在");
        }

        Device device = deviceMapper.selectById(req.getDeviceId());
        if (device == null || device.getDeleted() != null && device.getDeleted() == 1) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "设备不存在");
        }

        // 检查是否已存在相同宠物+设备的配置
        LambdaQueryWrapper<PetDetectionConfig> dupQuery = new LambdaQueryWrapper<PetDetectionConfig>()
                .eq(PetDetectionConfig::getPetId, req.getPetId())
                .eq(PetDetectionConfig::getDeviceId, req.getDeviceId());
        Long exists = configMapper.selectCount(dupQuery);
        if (exists != null && exists > 0) {
            throw new BusinessException(BusinessCode.STATUS_CONFLICT, "该宠物在此设备上已有检测配置");
        }

        PetDetectionConfig config = new PetDetectionConfig();
        config.setUserId(userId);
        config.setPetId(req.getPetId());
        config.setDeviceId(req.getDeviceId());
        config.setEnabled(req.getEnabled() != null && req.getEnabled() ? 1 : 0);
        config.setCooldownSeconds(req.getCooldownSeconds() != null ?
                req.getCooldownSeconds() : detectionProperties.getDefaultCooldownSeconds());
        config.setRemark(req.getRemark());
        config.setCreatedAt(LocalDateTime.now());
        configMapper.insert(config);

        return buildConfigVO(config, pet, device, Collections.emptyList());
    }

    @Override
    @Transactional
    public PetDetectionConfigVO updateConfig(Long id, PetDetectionConfigRequest req) {
        PetDetectionConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "检测配置不存在");
        }
        checkOwnership(config.getUserId());

        if (req.getEnabled() != null) {
            config.setEnabled(req.getEnabled() ? 1 : 0);
        }
        if (req.getCooldownSeconds() != null) {
            config.setCooldownSeconds(req.getCooldownSeconds());
        }
        if (req.getRemark() != null) {
            config.setRemark(req.getRemark());
        }
        configMapper.updateById(config);

        Pet pet = petMapper.selectById(config.getPetId());
        Device device = deviceMapper.selectById(config.getDeviceId());
        List<PetSafeZone> zones = listZonesByConfigId(id);
        return buildConfigVO(config, pet, device, zones);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        PetDetectionConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "检测配置不存在");
        }
        checkOwnership(config.getUserId());

        // 删除关联的安全区域
        LambdaQueryWrapper<PetSafeZone> zoneQuery = new LambdaQueryWrapper<PetSafeZone>()
                .eq(PetSafeZone::getDetectionConfigId, id);
        safeZoneMapper.delete(zoneQuery);

        configMapper.deleteById(id);
    }

    @Override
    public PetDetectionConfigVO getConfigById(Long id) {
        PetDetectionConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "检测配置不存在");
        }
        checkOwnership(config.getUserId());

        Pet pet = petMapper.selectById(config.getPetId());
        Device device = deviceMapper.selectById(config.getDeviceId());
        List<PetSafeZone> zones = listZonesByConfigId(id);
        return buildConfigVO(config, pet, device, zones);
    }

    @Override
    public List<PetDetectionConfigVO> listConfigs() {
        Long userId = getCurrentUserId();
        LambdaQueryWrapper<PetDetectionConfig> query = new LambdaQueryWrapper<PetDetectionConfig>()
                .eq(PetDetectionConfig::getUserId, userId)
                .orderByDesc(PetDetectionConfig::getUpdatedAt);
        List<PetDetectionConfig> configs = configMapper.selectList(query);

        if (configs.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查宠物和设备
        Set<Long> petIds = configs.stream().map(PetDetectionConfig::getPetId).collect(Collectors.toSet());
        Set<Long> deviceIds = configs.stream().map(PetDetectionConfig::getDeviceId).collect(Collectors.toSet());

        Map<Long, Pet> petMap = new HashMap<>();
        if (!petIds.isEmpty()) {
            petMapper.selectBatchIds(petIds).forEach(p -> petMap.put(p.getId(), p));
        }
        Map<Long, Device> deviceMap = new HashMap<>();
        if (!deviceIds.isEmpty()) {
            deviceMapper.selectBatchIds(deviceIds).forEach(d -> deviceMap.put(d.getId(), d));
        }

        // 批量查安全区域
        Set<Long> configIds = configs.stream().map(PetDetectionConfig::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<PetSafeZone> zoneQuery = new LambdaQueryWrapper<PetSafeZone>()
                .in(PetSafeZone::getDetectionConfigId, configIds);
        List<PetSafeZone> allZones = safeZoneMapper.selectList(zoneQuery);
        Map<Long, List<PetSafeZone>> zoneMap = allZones.stream()
                .collect(Collectors.groupingBy(PetSafeZone::getDetectionConfigId));

        return configs.stream()
                .map(c -> buildConfigVO(c, petMap.get(c.getPetId()), deviceMap.get(c.getDeviceId()),
                        zoneMap.getOrDefault(c.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    // ==================== 安全区域 CRUD ====================

    @Override
    @Transactional
    public PetSafeZoneVO createSafeZone(PetSafeZoneRequest req) {
        PetDetectionConfig config = configMapper.selectById(req.getDetectionConfigId());
        if (config == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "检测配置不存在");
        }
        checkOwnership(config.getUserId());

        validateZoneRequest(req);

        PetSafeZone zone = new PetSafeZone();
        zone.setDetectionConfigId(req.getDetectionConfigId());
        zone.setZoneName(req.getZoneName());
        zone.setZoneType(req.getZoneType());
        fillZoneCoordinates(zone, req);
        zone.setCreatedAt(LocalDateTime.now());
        safeZoneMapper.insert(zone);

        return toZoneVO(zone);
    }

    @Override
    @Transactional
    public PetSafeZoneVO updateSafeZone(Long id, PetSafeZoneRequest req) {
        PetSafeZone zone = safeZoneMapper.selectById(id);
        if (zone == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "安全区域不存在");
        }

        PetDetectionConfig config = configMapper.selectById(zone.getDetectionConfigId());
        if (config != null) {
            checkOwnership(config.getUserId());
        }

        validateZoneRequest(req);

        zone.setZoneName(req.getZoneName());
        zone.setZoneType(req.getZoneType());
        fillZoneCoordinates(zone, req);
        safeZoneMapper.updateById(zone);

        return toZoneVO(zone);
    }

    @Override
    @Transactional
    public void deleteSafeZone(Long id) {
        PetSafeZone zone = safeZoneMapper.selectById(id);
        if (zone == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "安全区域不存在");
        }

        PetDetectionConfig config = configMapper.selectById(zone.getDetectionConfigId());
        if (config != null) {
            checkOwnership(config.getUserId());
        }

        safeZoneMapper.deleteById(id);
    }

    @Override
    public List<PetSafeZoneVO> listSafeZones(Long detectionConfigId) {
        PetDetectionConfig config = configMapper.selectById(detectionConfigId);
        if (config == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "检测配置不存在");
        }
        checkOwnership(config.getUserId());

        List<PetSafeZone> zones = listZonesByConfigId(detectionConfigId);
        return zones.stream().map(this::toZoneVO).collect(Collectors.toList());
    }

    // ==================== 检测记录查询 ====================

    @Override
    public List<PetDetectionRecordVO> listRecords(PetDetectionRecordQueryDTO dto) {
        Long userId = getCurrentUserId();

        LambdaQueryWrapper<PetDetectionRecord> query = new LambdaQueryWrapper<>();
        if (dto.getDetectionConfigId() != null) {
            query.eq(PetDetectionRecord::getDetectionConfigId, dto.getDetectionConfigId());
        }
        if (dto.getPetId() != null) {
            query.eq(PetDetectionRecord::getPetId, dto.getPetId());
        }
        if (dto.getDeviceId() != null) {
            query.eq(PetDetectionRecord::getDeviceId, dto.getDeviceId());
        }
        if (dto.getAlarmTriggered() != null) {
            query.eq(PetDetectionRecord::getAlarmTriggered, dto.getAlarmTriggered());
        }
        if (dto.getStartTime() != null && !dto.getStartTime().isBlank()) {
            query.ge(PetDetectionRecord::getDetectTime, LocalDateTime.parse(dto.getStartTime(), DT_FMT));
        }
        if (dto.getEndTime() != null && !dto.getEndTime().isBlank()) {
            query.le(PetDetectionRecord::getDetectTime, LocalDateTime.parse(dto.getEndTime(), DT_FMT));
        }

        query.orderByDesc(PetDetectionRecord::getDetectTime);
        List<PetDetectionRecord> records = recordMapper.selectList(query);

        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查宠物名和设备名
        Set<Long> petIds = records.stream().map(PetDetectionRecord::getPetId).collect(Collectors.toSet());
        Set<Long> deviceIds = records.stream().map(PetDetectionRecord::getDeviceId).collect(Collectors.toSet());

        Map<Long, String> petNameMap = new HashMap<>();
        petMapper.selectBatchIds(petIds).forEach(p -> petNameMap.put(p.getId(), p.getPetName()));

        Map<Long, String> deviceNameMap = new HashMap<>();
        deviceMapper.selectBatchIds(deviceIds).forEach(d -> deviceNameMap.put(d.getId(), d.getDeviceName()));

        return records.stream().map(r -> {
            PetDetectionRecordVO vo = new PetDetectionRecordVO();
            vo.setId(r.getId());
            vo.setDetectionConfigId(r.getDetectionConfigId());
            vo.setPetId(r.getPetId());
            vo.setPetName(petNameMap.getOrDefault(r.getPetId(), null));
            vo.setDeviceId(r.getDeviceId());
            vo.setDeviceName(deviceNameMap.getOrDefault(r.getDeviceId(), null));
            vo.setDeviceSerial(r.getDeviceSerial());
            vo.setDetectTime(r.getDetectTime());
            vo.setPetCoordX(r.getPetCoordX());
            vo.setPetCoordY(r.getPetCoordY());
            vo.setPetWidth(r.getPetWidth());
            vo.setPetHeight(r.getPetHeight());
            vo.setInSafeZone(r.getInSafeZone());
            vo.setAlarmTriggered(r.getAlarmTriggered());
            vo.setSnapshotUrl(r.getSnapshotUrl());
            vo.setCreatedAt(r.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 手动触发检测 ====================

    @Override
    public PetDetectionResultVO triggerDetection(Long configId) {
        PetDetectionConfig config = configMapper.selectById(configId);
        if (config == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "检测配置不存在");
        }
        checkOwnership(config.getUserId());

        Pet pet = petMapper.selectById(config.getPetId());
        Device device = deviceMapper.selectById(config.getDeviceId());
        if (pet == null || device == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "关联的宠物或设备不存在");
        }

        List<PetSafeZone> zones = listZonesByConfigId(configId);

        return executeDetection(config, pet, device, zones);
    }

    // ==================== 定时任务调用的检测方法 ====================

    /**
     * 执行所有已启用配置的检测 (由定时任务调用)
     */
    public void runAllDetections() {
        LambdaQueryWrapper<PetDetectionConfig> query = new LambdaQueryWrapper<PetDetectionConfig>()
                .eq(PetDetectionConfig::getEnabled, 1);
        List<PetDetectionConfig> configs = configMapper.selectList(query);

        if (configs.isEmpty()) {
            return;
        }

        log.info("开始宠物检测, 共{}个启用的配置", configs.size());

        for (PetDetectionConfig config : configs) {
            try {
                Pet pet = petMapper.selectById(config.getPetId());
                Device device = deviceMapper.selectById(config.getDeviceId());
                if (pet == null || device == null) {
                    log.warn("检测配置关联的宠物或设备不存在, configId={}", config.getId());
                    continue;
                }
                if ("DISABLED".equals(device.getStatus())) {
                    log.debug("设备已禁用, 跳过检测, configId={}, deviceId={}", config.getId(), device.getId());
                    continue;
                }

                List<PetSafeZone> zones = listZonesByConfigId(config.getId());
                executeDetection(config, pet, device, zones);
            } catch (Exception e) {
                log.error("宠物检测异常, configId={}", config.getId(), e);
            }
        }
    }

    // ==================== 核心检测逻辑 ====================

    private PetDetectionResultVO executeDetection(PetDetectionConfig config, Pet pet, Device device,
                                                   List<PetSafeZone> zones) {
        LocalDateTime now = LocalDateTime.now();
        PetDetectionResultVO result = new PetDetectionResultVO();
        result.setPetId(pet.getId());
        result.setPetName(pet.getPetName());
        result.setDeviceId(device.getId());
        result.setDeviceName(device.getDeviceName());
        result.setDetectTime(now);

        // 1. 获取截图
        String snapshotUrl = null;
        try {
            snapshotUrl = ezvizSnapshotService.captureSnapshot(device.getDeviceSerial(), device.getChannelNo());
        } catch (Exception e) {
            log.warn("获取截图失败 deviceSerial={}: {}", device.getDeviceSerial(), e.getMessage());
        }
        result.setSnapshotUrl(snapshotUrl);

        // 2. 萤石AI宠物检测
        List<PetAiDetector.PetDetection> detections;
        try {
            detections = ezvizPetAiDetector.detect(snapshotUrl);
        } catch (Exception e) {
            log.error("萤石AI检测失败 configId={}: {}", config.getId(), e.getMessage());
            result.setMessage("AI检测失败: " + e.getMessage());
            saveRecord(config, device, now, null, true, false, snapshotUrl, null);
            return result;
        }

        if (detections == null || detections.isEmpty()) {
            log.debug("未检测到宠物 configId={}", config.getId());
            result.setInSafeZone(true);
            result.setAlarmTriggered(false);
            result.setMessage("未检测到宠物");
            saveRecord(config, device, now, null, true, false, snapshotUrl, null);
            return result;
        }

        // 3. 取第一个检测结果 (MVP阶段处理单只宠物)
        PetAiDetector.PetDetection detection = detections.get(0);

        // 4. 判断是否在安全区域内
        boolean inSafeZone = true;
        if (!zones.isEmpty()) {
            inSafeZone = isInsideAnyZone(detection, zones);
        }

        boolean alarmTriggered = false;

        // 5. 如果在区域外, 检查冷却时间后触发告警
        if (!inSafeZone) {
            if (!isInCooldown(config.getId(), now, config.getCooldownSeconds())) {
                alarmTriggered = true;
                createAlarm(config, device, detection, snapshotUrl, now);
                log.info("宠物越界告警已触发! configId={}, petName={}, deviceSerial={}",
                        config.getId(), pet.getPetName(), device.getDeviceSerial());
            } else {
                log.debug("告警冷却中, 跳过告警 configId={}", config.getId());
            }
        }

        // 6. 保存检测记录
        saveRecord(config, device, now, detection, inSafeZone, alarmTriggered, snapshotUrl, detection);

        result.setInSafeZone(inSafeZone);
        result.setAlarmTriggered(alarmTriggered);
        result.setMessage(inSafeZone ? "宠物在安全区域内" : (alarmTriggered ? "宠物越界, 已触发告警" : "宠物越界, 告警冷却中"));

        return result;
    }

    /**
     * 判断宠物是否在任意一个安全区域内
     */
    private boolean isInsideAnyZone(PetAiDetector.PetDetection detection, List<PetSafeZone> zones) {
        for (PetSafeZone zone : zones) {
            if (isInsideZone(detection, zone)) {
                return true;
            }
        }
        // 如果有安全区域但都不在里面, 返回false
        return false;
    }

    /**
     * 判断宠物是否在指定安全区域内
     * 使用边界框中心点判断
     */
    private boolean isInsideZone(PetAiDetector.PetDetection detection, PetSafeZone zone) {
        // 宠物边界框中心点
        double centerX = detection.getX() + detection.getWidth() / 2.0;
        double centerY = detection.getY() + detection.getHeight() / 2.0;

        if (PetDetectionConstant.ZONE_TYPE_RECTANGLE.equals(zone.getZoneType())) {
            return isPointInRectangle(centerX, centerY, zone);
        } else if (PetDetectionConstant.ZONE_TYPE_POLYGON.equals(zone.getZoneType())) {
            return isPointInPolygon(centerX, centerY, zone);
        }
        return false;
    }

    private boolean isPointInRectangle(double x, double y, PetSafeZone zone) {
        if (zone.getRectLeft() == null || zone.getRectTop() == null ||
                zone.getRectRight() == null || zone.getRectBottom() == null) {
            return true; // 区域配置不完整, 默认安全
        }
        return x >= zone.getRectLeft() && x <= zone.getRectRight()
                && y >= zone.getRectTop() && y <= zone.getRectBottom();
    }

    private boolean isPointInPolygon(double x, double y, PetSafeZone zone) {
        if (zone.getPolygonPoints() == null || zone.getPolygonPoints().isBlank()) {
            return true;
        }

        try {
            List<Map<String, Double>> points = objectMapper.readValue(
                    zone.getPolygonPoints(), new TypeReference<>() {});
            return isPointInPolygonList(x, y, points);
        } catch (Exception e) {
            log.warn("解析多边形坐标失败: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 射线法判断点是否在多边形内
     */
    private boolean isPointInPolygonList(double x, double y, List<Map<String, Double>> points) {
        int n = points.size();
        if (n < 3) return true;

        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = points.get(i).get("x");
            double yi = points.get(i).get("y");
            double xj = points.get(j).get("x");
            double yj = points.get(j).get("y");

            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * 检查是否在冷却时间内
     */
    private boolean isInCooldown(Long configId, LocalDateTime now, int cooldownSeconds) {
        LocalDateTime cooldownStart = now.minusSeconds(cooldownSeconds);
        LambdaQueryWrapper<PetDetectionRecord> query = new LambdaQueryWrapper<PetDetectionRecord>()
                .eq(PetDetectionRecord::getDetectionConfigId, configId)
                .eq(PetDetectionRecord::getAlarmTriggered, PetDetectionConstant.ALARM_TRIGGERED)
                .ge(PetDetectionRecord::getDetectTime, cooldownStart);
        Long count = recordMapper.selectCount(query);
        return count != null && count > 0;
    }

    /**
     * 创建告警消息 (复用alarm_message表)
     */
    private void createAlarm(PetDetectionConfig config, Device device,
                             PetAiDetector.PetDetection detection, String snapshotUrl, LocalDateTime now) {
        AlarmMessage alarm = new AlarmMessage();
        alarm.setDeviceId(device.getId());
        alarm.setDeviceSerial(device.getDeviceSerial());
        alarm.setChannelNo(device.getChannelNo() != null ? device.getChannelNo() : AlarmConstant.DEFAULT_CHANNEL_NO);
        alarm.setAlarmId("PET_" + config.getId() + "_" + System.currentTimeMillis());
        alarm.setAlarmType(PetDetectionConstant.ALARM_TYPE_PET_OUT_OF_ZONE);
        alarm.setAlarmName("宠物越界告警");
        alarm.setAlarmTime(now);
        alarm.setAlarmPicUrl(snapshotUrl);
        alarm.setAlarmContent(String.format("宠物离开安全区域! 检测位置: (%.1f%%, %.1f%%)",
                detection.getX() + detection.getWidth() / 2,
                detection.getY() + detection.getHeight() / 2));
        alarm.setReadStatus(AlarmConstant.READ_STATUS_UNREAD);
        alarm.setSource(PetDetectionConstant.SOURCE_PET_DETECT);
        alarm.setDeleted(AlarmConstant.DELETED_NO);

        try {
            alarmMessageMapper.insert(alarm);
        } catch (Exception e) {
            log.warn("插入宠物越界告警失败(可能重复): {}", e.getMessage());
        }
    }

    /**
     * 保存检测记录
     */
    private void saveRecord(PetDetectionConfig config, Device device, LocalDateTime detectTime,
                            PetAiDetector.PetDetection detection, boolean inSafeZone,
                            boolean alarmTriggered, String snapshotUrl, PetAiDetector.PetDetection det) {
        PetDetectionRecord record = new PetDetectionRecord();
        record.setDetectionConfigId(config.getId());
        record.setPetId(config.getPetId());
        record.setDeviceId(device.getId());
        record.setDeviceSerial(device.getDeviceSerial());
        record.setDetectTime(detectTime);
        if (det != null) {
            record.setPetCoordX(det.getX());
            record.setPetCoordY(det.getY());
            record.setPetWidth(det.getWidth());
            record.setPetHeight(det.getHeight());
        }
        record.setInSafeZone(inSafeZone ? PetDetectionConstant.IN_ZONE_YES : PetDetectionConstant.IN_ZONE_NO);
        record.setAlarmTriggered(alarmTriggered ? PetDetectionConstant.ALARM_TRIGGERED : PetDetectionConstant.ALARM_NOT_TRIGGERED);
        record.setSnapshotUrl(snapshotUrl);

        if (det != null) {
            try {
                record.setAiResultJson(objectMapper.writeValueAsString(det));
            } catch (Exception ignored) {}
        }

        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
    }

    // ==================== 辅助方法 ====================

    private List<PetSafeZone> listZonesByConfigId(Long configId) {
        LambdaQueryWrapper<PetSafeZone> query = new LambdaQueryWrapper<PetSafeZone>()
                .eq(PetSafeZone::getDetectionConfigId, configId);
        return safeZoneMapper.selectList(query);
    }

    private void validateZoneRequest(PetSafeZoneRequest req) {
        if (PetDetectionConstant.ZONE_TYPE_RECTANGLE.equals(req.getZoneType())) {
            if (req.getRectLeft() == null || req.getRectTop() == null ||
                    req.getRectRight() == null || req.getRectBottom() == null) {
                throw new BusinessException(BusinessCode.PARAM_INVALID, "矩形区域必须提供四个坐标值");
            }
            if (req.getRectLeft() >= req.getRectRight() || req.getRectTop() >= req.getRectBottom()) {
                throw new BusinessException(BusinessCode.PARAM_INVALID, "矩形区域坐标不合法");
            }
        } else if (PetDetectionConstant.ZONE_TYPE_POLYGON.equals(req.getZoneType())) {
            if (req.getPolygonPoints() == null || req.getPolygonPoints().size() < 3) {
                throw new BusinessException(BusinessCode.PARAM_INVALID, "多边形区域至少需要3个顶点");
            }
        } else {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "不支持的区域类型: " + req.getZoneType());
        }
    }

    private void fillZoneCoordinates(PetSafeZone zone, PetSafeZoneRequest req) {
        if (PetDetectionConstant.ZONE_TYPE_RECTANGLE.equals(req.getZoneType())) {
            zone.setRectLeft(req.getRectLeft());
            zone.setRectTop(req.getRectTop());
            zone.setRectRight(req.getRectRight());
            zone.setRectBottom(req.getRectBottom());
        } else if (PetDetectionConstant.ZONE_TYPE_POLYGON.equals(req.getZoneType())) {
            try {
                List<Map<String, Double>> points = req.getPolygonPoints().stream()
                        .map(p -> {
                            Map<String, Double> map = new HashMap<>();
                            map.put("x", p.getX());
                            map.put("y", p.getY());
                            return map;
                        }).collect(Collectors.toList());
                zone.setPolygonPoints(objectMapper.writeValueAsString(points));
            } catch (Exception e) {
                throw new BusinessException(BusinessCode.PARAM_INVALID, "多边形坐标格式错误");
            }
        }
    }

    private PetDetectionConfigVO buildConfigVO(PetDetectionConfig config, Pet pet, Device device,
                                                List<PetSafeZone> zones) {
        PetDetectionConfigVO vo = new PetDetectionConfigVO();
        vo.setId(config.getId());
        vo.setUserId(config.getUserId());
        vo.setPetId(config.getPetId());
        vo.setPetName(pet != null ? pet.getPetName() : null);
        vo.setDeviceId(config.getDeviceId());
        vo.setDeviceName(device != null ? device.getDeviceName() : null);
        vo.setDeviceSerial(device != null ? device.getDeviceSerial() : null);
        vo.setEnabled(config.getEnabled());
        vo.setCooldownSeconds(config.getCooldownSeconds());
        vo.setRemark(config.getRemark());
        vo.setCreatedAt(config.getCreatedAt());
        vo.setUpdatedAt(config.getUpdatedAt());
        vo.setSafeZones(zones.stream().map(this::toZoneVO).collect(Collectors.toList()));
        return vo;
    }

    private PetSafeZoneVO toZoneVO(PetSafeZone zone) {
        PetSafeZoneVO vo = new PetSafeZoneVO();
        vo.setId(zone.getId());
        vo.setDetectionConfigId(zone.getDetectionConfigId());
        vo.setZoneName(zone.getZoneName());
        vo.setZoneType(zone.getZoneType());
        vo.setRectLeft(zone.getRectLeft());
        vo.setRectTop(zone.getRectTop());
        vo.setRectRight(zone.getRectRight());
        vo.setRectBottom(zone.getRectBottom());

        if (zone.getPolygonPoints() != null && !zone.getPolygonPoints().isBlank()) {
            try {
                List<Map<String, Double>> points = objectMapper.readValue(
                        zone.getPolygonPoints(), new TypeReference<>() {});
                List<PetSafeZoneVO.PointVO> pointVOs = points.stream().map(p -> {
                    PetSafeZoneVO.PointVO pv = new PetSafeZoneVO.PointVO();
                    pv.setX(p.get("x"));
                    pv.setY(p.get("y"));
                    return pv;
                }).collect(Collectors.toList());
                vo.setPolygonPoints(pointVOs);
            } catch (Exception ignored) {}
        }

        vo.setCreatedAt(zone.getCreatedAt());
        return vo;
    }

    private Long getCurrentUserId() {
        return (Long) request.getAttribute("userId");
    }

    private void checkOwnership(Long ownerUserId) {
        Long userId = getCurrentUserId();
        if (!ownerUserId.equals(userId)) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "无权操作");
        }
    }
}
