package com.yzh.yingshi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.dto.PetAiVisionResult;
import com.yzh.yingshi.entity.AlarmMessage;
import com.yzh.yingshi.entity.Pet;
import com.yzh.yingshi.entity.PetAiReport;
import com.yzh.yingshi.entity.PetDetectionConfig;
import com.yzh.yingshi.mapper.AlarmMessageMapper;
import com.yzh.yingshi.mapper.PetAiReportMapper;
import com.yzh.yingshi.mapper.PetDetectionConfigMapper;
import com.yzh.yingshi.mapper.PetMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 告警自动AI分析服务
 * 告警触发后异步生成AI分析报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetAutoAnalysisService {

    private final AlarmMessageMapper alarmMessageMapper;
    private final PetAiReportMapper reportMapper;
    private final PetDetectionConfigMapper configMapper;
    private final PetMapper petMapper;
    private final MimoMultimodalClient multimodalClient;
    private final ObjectMapper objectMapper;

    private static final String PROMPT_VERSION = "auto-alarm-v1";

    private static final String SYSTEM_PROMPT = """
            你是宠物居家监护系统的自动分析员。系统检测到一条告警，请根据告警信息和宠物档案生成简洁的事件分析。
            只返回合法JSON，不要使用Markdown代码块。JSON字段必须为：
            riskLevel: LOW、MEDIUM或HIGH；
            summary: 一句话事件摘要；
            observedBehavior: 根据告警类型推断的宠物行为；
            evidenceBasis: 告警触发原因和系统检测数据；
            recommendations: 字符串数组，给主人的行动建议；
            uncertainties: 字符串数组，列出自动分析的局限性。
            """;

    /**
     * 异步为告警生成AI分析报告
     */
    @Async
    public void analyzeAlarmAsync(Long alarmId) {
        try {
            analyzeAlarm(alarmId);
        } catch (Exception e) {
            log.warn("告警自动AI分析失败 alarmId={}: {}", alarmId, e.getMessage());
        }
    }

    /**
     * 为告警生成AI分析报告（同步）
     */
    public void analyzeAlarm(Long alarmId) {
        AlarmMessage alarm = alarmMessageMapper.selectById(alarmId);
        if (alarm == null) {
            return;
        }

        // 查找关联的检测配置和宠物
        Long configId = extractConfigId(alarm);
        if (configId == null) {
            log.debug("告警无关联检测配置, 跳过自动分析 alarmId={}", alarmId);
            return;
        }

        PetDetectionConfig config = configMapper.selectById(configId);
        if (config == null) {
            return;
        }

        Pet pet = petMapper.selectById(config.getPetId());
        if (pet == null) {
            return;
        }

        // 检查是否已存在该告警的分析报告
        Long existingCount = reportMapper.selectCount(
                new LambdaQueryWrapper<PetAiReport>()
                        .eq(PetAiReport::getSourceType, "ALARM")
                        .eq(PetAiReport::getSourceId, alarmId));
        if (existingCount != null && existingCount > 0) {
            log.debug("告警已有分析报告, 跳过 alarmId={}", alarmId);
            return;
        }

        // 构建提示词
        String prompt = buildPrompt(alarm, pet);
        Map<String, Object> evidence = buildEvidence(alarm, config);

        // 调用 MiMo 分析
        PetAiVisionResult analysis;
        try {
            if (StringUtils.hasText(alarm.getAlarmPicUrl())) {
                analysis = multimodalClient.analyze(
                        List.of(alarm.getAlarmPicUrl()),
                        SYSTEM_PROMPT,
                        prompt
                );
            } else {
                // 无图片时用文本模式（简化处理，直接生成基础分析）
                analysis = buildFallbackAnalysis(alarm);
            }
        } catch (Exception e) {
            log.warn("MiMo分析失败, 使用兜底分析 alarmId={}: {}", alarmId, e.getMessage());
            analysis = buildFallbackAnalysis(alarm);
        }

        normalizeAnalysis(analysis);

        // 保存报告
        PetAiReport report = new PetAiReport();
        report.setUserId(pet.getUserId());
        report.setPetId(pet.getId());
        report.setPetName(pet.getPetName());
        report.setSourceType("ALARM");
        report.setSourceId(alarmId);
        report.setSourceTime(alarm.getAlarmTime());
        report.setImageUrl(alarm.getAlarmPicUrl() != null ? alarm.getAlarmPicUrl() : "");
        report.setReportType("EVENT");
        report.setRiskLevel(analysis.getRiskLevel());
        report.setTitle(pet.getPetName() + " · 告警自动分析");
        report.setSummary(analysis.getSummary());
        report.setObservedBehavior(analysis.getObservedBehavior());
        report.setEvidenceBasis(analysis.getEvidenceBasis());
        report.setRecommendationsJson(writeJson(analysis.getRecommendations()));
        report.setUncertaintiesJson(writeJson(analysis.getUncertainties()));
        report.setEvidenceJson(writeJson(evidence));
        report.setAnalysisJson(writeJson(analysis));
        report.setModelName(multimodalClient.getModel());
        report.setPromptVersion(PROMPT_VERSION);
        reportMapper.insert(report);

        log.info("告警自动AI分析完成 alarmId={}, petName={}, riskLevel={}",
                alarmId, pet.getPetName(), analysis.getRiskLevel());
    }

    private Long extractConfigId(AlarmMessage alarm) {
        // 从 alarmId 格式 "PET_{configId}_{timestamp}" 或 alarmContent 中提取
        if (alarm.getAlarmId() != null && alarm.getAlarmId().startsWith("PET_")) {
            String[] parts = alarm.getAlarmId().split("_");
            if (parts.length >= 2) {
                try {
                    return Long.parseLong(parts[1]);
                } catch (NumberFormatException ignored) {}
            }
        }
        // 从 alarmContent 中提取 [configId:X]
        if (alarm.getAlarmContent() != null) {
            int idx = alarm.getAlarmContent().indexOf("[configId:");
            if (idx >= 0) {
                int end = alarm.getAlarmContent().indexOf("]", idx);
                if (end > idx) {
                    try {
                        return Long.parseLong(alarm.getAlarmContent().substring(idx + 10, end));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }

    private String buildPrompt(AlarmMessage alarm, Pet pet) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("系统检测到一条宠物告警，请分析。\n\n");

        prompt.append("宠物档案：\n");
        prompt.append("- 名称：").append(pet.getPetName()).append('\n');
        prompt.append("- 类型：").append(pet.getPetType()).append('\n');
        if (pet.getAge() != null) {
            prompt.append("- 年龄：").append(pet.getAge()).append("个月\n");
        }

        prompt.append("\n告警信息：\n");
        prompt.append("- 类型：").append(alarm.getAlarmName()).append('\n');
        prompt.append("- 时间：").append(alarm.getAlarmTime()).append('\n');
        prompt.append("- 内容：").append(alarm.getAlarmContent()).append('\n');
        prompt.append("- 来源：").append(alarm.getSource()).append('\n');

        prompt.append("\n请只描述可推断的事实，不要臆测。");
        return prompt.toString();
    }

    private Map<String, Object> buildEvidence(AlarmMessage alarm, PetDetectionConfig config) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("alarmType", alarm.getAlarmType());
        evidence.put("alarmName", alarm.getAlarmName());
        evidence.put("alarmContent", alarm.getAlarmContent());
        evidence.put("alarmTime", alarm.getAlarmTime());
        evidence.put("deviceSerial", alarm.getDeviceSerial());
        evidence.put("source", alarm.getSource());
        evidence.put("configId", config.getId());
        return evidence;
    }

    private PetAiVisionResult buildFallbackAnalysis(AlarmMessage alarm) {
        PetAiVisionResult result = new PetAiVisionResult();
        String alarmType = alarm.getAlarmType();
        if ("PET_OUT_OF_ZONE".equals(alarmType)) {
            result.setRiskLevel("HIGH");
            result.setSummary("宠物离开安全区域，系统已触发告警。");
            result.setObservedBehavior("宠物被检测到离开了预设的安全区域。");
        } else if ("PET_ABSENT".equals(alarmType)) {
            result.setRiskLevel("MEDIUM");
            result.setSummary("宠物长时间未在画面中出现。");
            result.setObservedBehavior("监控画面中未检测到宠物。");
        } else if ("PET_ABNORMAL_ACTIVITY".equals(alarmType)) {
            result.setRiskLevel("MEDIUM");
            result.setSummary("宠物活动频率异常偏高。");
            result.setObservedBehavior("短时间内检测到多次大幅位移。");
        } else if ("PET_LONG_STILLNESS".equals(alarmType)) {
            result.setRiskLevel("MEDIUM");
            result.setSummary("宠物长时间保持静止。");
            result.setObservedBehavior("宠物位置长时间未发生变化。");
        } else {
            result.setRiskLevel("MEDIUM");
            result.setSummary("系统检测到一条告警。");
            result.setObservedBehavior("详见告警内容。");
        }
        result.setEvidenceBasis("基于系统自动检测数据。");
        result.setRecommendations(List.of("建议查看监控画面确认宠物状态"));
        result.setUncertainties(List.of("自动分析未包含图片视觉信息，仅基于检测数据推断"));
        return result;
    }

    private void normalizeAnalysis(PetAiVisionResult analysis) {
        if (analysis == null) {
            return;
        }
        String risk = StringUtils.hasText(analysis.getRiskLevel())
                ? analysis.getRiskLevel().trim().toUpperCase(Locale.ROOT) : "MEDIUM";
        if (!List.of("LOW", "MEDIUM", "HIGH").contains(risk)) {
            risk = "MEDIUM";
        }
        analysis.setRiskLevel(risk);
        analysis.setSummary(defaultText(analysis.getSummary(), "告警事件自动分析"));
        analysis.setObservedBehavior(defaultText(analysis.getObservedBehavior(), "详见告警记录"));
        analysis.setEvidenceBasis(defaultText(analysis.getEvidenceBasis(), "基于系统告警数据"));
        analysis.setRecommendations(defaultList(analysis.getRecommendations(), "建议查看监控画面确认宠物状态"));
        analysis.setUncertainties(defaultList(analysis.getUncertainties(), "自动分析可能存在偏差"));
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private List<String> defaultList(List<String> value, String fallback) {
        return value == null || value.isEmpty() ? List.of(fallback) : value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
