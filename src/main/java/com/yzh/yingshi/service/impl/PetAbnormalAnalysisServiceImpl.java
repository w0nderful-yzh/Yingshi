package com.yzh.yingshi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yzh.yingshi.constant.AlarmConstant;
import com.yzh.yingshi.constant.PetDetectionConstant;
import com.yzh.yingshi.entity.*;
import com.yzh.yingshi.mapper.*;
import com.yzh.yingshi.service.PetAbnormalAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetAbnormalAnalysisServiceImpl implements PetAbnormalAnalysisService {

    private final PetDetectionConfigMapper configMapper;
    private final PetDetectionRecordMapper recordMapper;
    private final DeviceMapper deviceMapper;
    private final PetMapper petMapper;
    private final AlarmMessageMapper alarmMessageMapper;

    @Override
    public void analyzeAll() {
        LambdaQueryWrapper<PetDetectionConfig> query = new LambdaQueryWrapper<PetDetectionConfig>()
                .eq(PetDetectionConfig::getEnabled, 1);
        List<PetDetectionConfig> configs = configMapper.selectList(query);

        if (configs.isEmpty()) return;

        log.info("开始异常行为分析, 共{}个配置", configs.size());

        for (PetDetectionConfig config : configs) {
            try {
                Device device = deviceMapper.selectById(config.getDeviceId());
                Pet pet = petMapper.selectById(config.getPetId());
                if (device == null || pet == null || "DISABLED".equals(device.getStatus())) {
                    continue;
                }

                checkPetAbsent(config, device, pet);
                checkAbnormalActivity(config, device, pet);
                checkLongStillness(config, device, pet);

            } catch (Exception e) {
                log.error("异常行为分析失败 configId={}", config.getId(), e);
            }
        }
    }

    @Override
    public void analyzeConfig(Long configId) {
        PetDetectionConfig config = configMapper.selectById(configId);
        if (config == null) {
            log.warn("手动异常分析: 配置不存在 configId={}", configId);
            return;
        }
        Device device = deviceMapper.selectById(config.getDeviceId());
        Pet pet = petMapper.selectById(config.getPetId());
        if (device == null || pet == null) {
            log.warn("手动异常分析: 设备或宠物不存在 configId={}", configId);
            return;
        }

        log.info("手动触发异常分析 configId={}, petName={}, deviceSerial={}",
                configId, pet.getPetName(), device.getDeviceSerial());

        checkPetAbsent(config, device, pet);
        checkAbnormalActivity(config, device, pet);
        checkLongStillness(config, device, pet);
    }

    /**
     * 异常类型1: 宠物长时间未出现
     * 判断: 最后一次检测到宠物的时间距今超过阈值
     */
    private void checkPetAbsent(PetDetectionConfig config, Device device, Pet pet) {
        int thresholdMinutes = config.getPetAbsentMinutes() != null ? config.getPetAbsentMinutes() : 60;
        LocalDateTime since = LocalDateTime.now().minusMinutes(thresholdMinutes);

        // 查找最近阈值时间内是否有检测到宠物的记录 (petCoordX不为空表示检测到了)
        LambdaQueryWrapper<PetDetectionRecord> query = new LambdaQueryWrapper<PetDetectionRecord>()
                .eq(PetDetectionRecord::getDetectionConfigId, config.getId())
                .isNotNull(PetDetectionRecord::getPetCoordX)
                .ge(PetDetectionRecord::getDetectTime, since);
        Long count = recordMapper.selectCount(query);

        if (count != null && count > 0) {
            return; // 最近有检测到宠物, 正常
        }

        // 检查是否有足够历史记录(至少要有1条记录才能判断"未出现")
        LambdaQueryWrapper<PetDetectionRecord> historyQuery = new LambdaQueryWrapper<PetDetectionRecord>()
                .eq(PetDetectionRecord::getDetectionConfigId, config.getId());
        Long totalRecords = recordMapper.selectCount(historyQuery);
        if (totalRecords == null || totalRecords == 0) {
            return; // 没有任何记录, 说明还没开始检测
        }

        // 检查冷却: 同类型告警在冷却时间内不重复
        if (isAlarmInCooldown(config.getId(), PetDetectionConstant.ALARM_TYPE_PET_ABSENT,
                config.getCooldownSeconds())) {
            log.debug("宠物未出现告警冷却中 configId={}", config.getId());
            return;
        }

        // 触发告警
        createAlarm(config, device, pet,
                PetDetectionConstant.ALARM_TYPE_PET_ABSENT,
                "宠物长时间未出现",
                String.format("宠物[%s]已超过%d分钟未在画面中出现", pet.getPetName(), thresholdMinutes),
                null);

        log.warn("宠物长时间未出现告警! configId={}, petName={}, 未出现{}分钟",
                config.getId(), pet.getPetName(), thresholdMinutes);
    }

    /**
     * 异常类型2: 宠物异常活跃
     * 判断: 短时间内坐标大幅移动的次数超过阈值
     */
    private void checkAbnormalActivity(PetDetectionConfig config, Device device, Pet pet) {
        int windowMinutes = config.getActivityWindowMinutes() != null ? config.getActivityWindowMinutes() : 10;
        int countThreshold = config.getActivityCountThreshold() != null ? config.getActivityCountThreshold() : 5;
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(windowMinutes);

        // 查询时间窗口内有坐标的检测记录, 按时间正序
        LambdaQueryWrapper<PetDetectionRecord> query = new LambdaQueryWrapper<PetDetectionRecord>()
                .eq(PetDetectionRecord::getDetectionConfigId, config.getId())
                .isNotNull(PetDetectionRecord::getPetCoordX)
                .ge(PetDetectionRecord::getDetectTime, windowStart)
                .orderByAsc(PetDetectionRecord::getDetectTime);
        List<PetDetectionRecord> records = recordMapper.selectList(query);

        if (records.size() < 2) {
            return; // 记录不足, 无法比较
        }

        // 相邻两次检测的坐标变化超过10%算一次"大幅移动"
        double movementThreshold = 10.0;
        int bigMoveCount = 0;
        for (int i = 1; i < records.size(); i++) {
            PetDetectionRecord prev = records.get(i - 1);
            PetDetectionRecord curr = records.get(i);
            double dx = Math.abs(curr.getPetCoordX() - prev.getPetCoordX());
            double dy = Math.abs(curr.getPetCoordY() - prev.getPetCoordY());
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance >= movementThreshold) {
                bigMoveCount++;
            }
        }

        log.info("异常活跃检测: configId={}, 窗口{}分钟, 记录{}条, 大幅移动{}次(阈值{}次)",
                config.getId(), windowMinutes, records.size(), bigMoveCount, countThreshold);

        if (bigMoveCount < countThreshold) {
            return; // 大幅移动次数未超阈值, 正常
        }

        // 检查冷却
        if (isAlarmInCooldown(config.getId(), PetDetectionConstant.ALARM_TYPE_PET_ABNORMAL_ACTIVITY,
                config.getCooldownSeconds())) {
            return;
        }

        createAlarm(config, device, pet,
                PetDetectionConstant.ALARM_TYPE_PET_ABNORMAL_ACTIVITY,
                "宠物异常活跃",
                String.format("宠物[%s]在%d分钟内有%d次大幅移动(>10%%), 可能异常活跃",
                        pet.getPetName(), windowMinutes, bigMoveCount),
                null);

        log.warn("宠物异常活跃告警! configId={}, petName={}, {}分钟内{}次大幅移动",
                config.getId(), pet.getPetName(), windowMinutes, bigMoveCount);
    }

    /**
     * 异常类型3: 宠物长时间静止
     * 判断: 最近一段时间内检测到的宠物位置几乎没有变化
     */
    private void checkLongStillness(PetDetectionConfig config, Device device, Pet pet) {
        int thresholdMinutes = config.getStillnessMinutes() != null ? config.getStillnessMinutes() : 30;
        LocalDateTime since = LocalDateTime.now().minusMinutes(thresholdMinutes);

        log.info("静止检测: configId={}, 阈值={}分钟, 查询since={}", config.getId(), thresholdMinutes, since);

        // 查询最近时间段内有宠物坐标的检测记录
        LambdaQueryWrapper<PetDetectionRecord> query = new LambdaQueryWrapper<PetDetectionRecord>()
                .eq(PetDetectionRecord::getDetectionConfigId, config.getId())
                .isNotNull(PetDetectionRecord::getPetCoordX)
                .ge(PetDetectionRecord::getDetectTime, since)
                .orderByDesc(PetDetectionRecord::getDetectTime);
        List<PetDetectionRecord> records = recordMapper.selectList(query);

        log.info("静止检测: configId={}, 查到{}条有坐标记录(最近{}分钟内)", config.getId(), records.size(), thresholdMinutes);

        // 需要足够多的记录才能判断静止
        if (records.size() < 3) {
            log.info("静止检测: 记录不足3条, 跳过 configId={}", config.getId());
            return;
        }

        // 计算所有记录的坐标最大偏移量
        double avgX = 0, avgY = 0;
        for (PetDetectionRecord r : records) {
            avgX += r.getPetCoordX();
            avgY += r.getPetCoordY();
        }
        avgX /= records.size();
        avgY /= records.size();

        double maxDeviation = 0;
        for (PetDetectionRecord r : records) {
            double devX = Math.abs(r.getPetCoordX() - avgX);
            double devY = Math.abs(r.getPetCoordY() - avgY);
            maxDeviation = Math.max(maxDeviation, devX);
            maxDeviation = Math.max(maxDeviation, devY);
        }

        log.info("静止检测: configId={}, 平均坐标({},{}) 最大偏移{}%",
                config.getId(), String.format("%.1f", avgX), String.format("%.1f", avgY),
                String.format("%.2f", maxDeviation));

        // 如果最大偏移量小于2%, 认为宠物静止
        double stillnessThreshold = 2.0;
        if (maxDeviation >= stillnessThreshold) {
            log.info("静止检测: 偏移量{}% >= {}%, 宠物有移动, 不告警 configId={}",
                    String.format("%.2f", maxDeviation), stillnessThreshold, config.getId());
            return;
        }

        // 检查冷却
        if (isAlarmInCooldown(config.getId(), PetDetectionConstant.ALARM_TYPE_PET_LONG_STILLNESS,
                config.getCooldownSeconds())) {
            return;
        }

        createAlarm(config, device, pet,
                PetDetectionConstant.ALARM_TYPE_PET_LONG_STILLNESS,
                "宠物长时间静止",
                String.format("宠物[%s]已超过%d分钟位置几乎未变化(偏移%.1f%%), 可能异常",
                        pet.getPetName(), thresholdMinutes, maxDeviation),
                null);

        log.warn("宠物长时间静止告警! configId={}, petName={}, 静止{}分钟, 偏移{}%",
                config.getId(), pet.getPetName(), thresholdMinutes,
                String.format("%.1f", maxDeviation));
    }

    /**
     * 检查同类型告警是否在冷却时间内
     */
    private boolean isAlarmInCooldown(Long configId, String alarmType, int cooldownSeconds) {
        LocalDateTime cooldownStart = LocalDateTime.now().minusSeconds(cooldownSeconds);
        LambdaQueryWrapper<AlarmMessage> query = new LambdaQueryWrapper<AlarmMessage>()
                .eq(AlarmMessage::getAlarmType, alarmType)
                .like(AlarmMessage::getAlarmContent, "configId:" + configId)
                .ge(AlarmMessage::getAlarmTime, cooldownStart);
        Long count = alarmMessageMapper.selectCount(query);
        return count != null && count > 0;
    }

    /**
     * 创建异常行为告警
     */
    private void createAlarm(PetDetectionConfig config, Device device, Pet pet,
                             String alarmType, String alarmName, String alarmContent,
                             String snapshotUrl) {
        AlarmMessage alarm = new AlarmMessage();
        alarm.setDeviceId(device.getId());
        alarm.setDeviceSerial(device.getDeviceSerial());
        alarm.setChannelNo(device.getChannelNo() != null ? device.getChannelNo() : AlarmConstant.DEFAULT_CHANNEL_NO);
        alarm.setAlarmId(alarmType + "_" + config.getId() + "_" + System.currentTimeMillis());
        alarm.setAlarmType(alarmType);
        alarm.setAlarmName(alarmName);
        alarm.setAlarmTime(LocalDateTime.now());
        alarm.setAlarmPicUrl(snapshotUrl);
        alarm.setAlarmContent(alarmContent + " [configId:" + config.getId() + "]");
        alarm.setReadStatus(AlarmConstant.READ_STATUS_UNREAD);
        alarm.setSource(PetDetectionConstant.SOURCE_PET_DETECT);
        alarm.setDeleted(AlarmConstant.DELETED_NO);

        try {
            alarmMessageMapper.insert(alarm);
        } catch (Exception e) {
            log.warn("插入异常行为告警失败: {}", e.getMessage());
        }
    }
}
