package com.yzh.yingshi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.auth.CurrentUserService;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.entity.*;
import com.yzh.yingshi.mapper.*;
import com.yzh.yingshi.vo.PetAiReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 宠物活动日结/周结服务
 * 汇总检测数据和告警，生成周期性分析报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetSummaryService {

    private final PetMapper petMapper;
    private final PetDetectionConfigMapper configMapper;
    private final PetDetectionRecordMapper recordMapper;
    private final AlarmMessageMapper alarmMapper;
    private final PetAiReportMapper reportMapper;
    private final CurrentUserService currentUserService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    private static final String SUMMARY_SYSTEM_PROMPT = """
            你是宠物居家监护系统的数据分析师。请根据以下检测统计数据，生成一份简洁的宠物活动总结报告。
            只返回合法JSON，不要使用Markdown代码块。JSON字段必须为：
            riskLevel: LOW、MEDIUM或HIGH（根据异常情况严重程度判断）；
            summary: 一段话总结宠物在这段时间的整体状态；
            observedBehavior: 描述宠物的活动模式（活跃/安静/正常等）；
            evidenceBasis: 列出关键数据指标（检测次数、告警次数、越界次数等）；
            recommendations: 字符串数组，基于数据给出的建议；
            uncertainties: 字符串数组，数据分析的局限性。
            """;

    /**
     * 生成日报
     */
    public PetAiReportVO generateDailySummary(Long petId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        return generateSummary(petId, start, end, "DAILY_SUMMARY",
                today + " 活动日报");
    }

    /**
     * 生成周报
     */
    public PetAiReportVO generateWeeklySummary(Long petId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        return generateSummary(petId, start, end, "WEEKLY_SUMMARY",
                weekStart + " ~ " + today + " 活动周报");
    }

    private PetAiReportVO generateSummary(Long petId, LocalDateTime start, LocalDateTime end,
                                          String sourceType, String periodLabel) {
        Long userId = currentUserService.requireCurrentUserId();
        Pet pet = requireOwnedPet(petId, userId);

        // 检查是否已生成过（同一周期同一宠物）
        PetAiReport existing = reportMapper.selectOne(
                new LambdaQueryWrapper<PetAiReport>()
                        .eq(PetAiReport::getUserId, userId)
                        .eq(PetAiReport::getPetId, petId)
                        .eq(PetAiReport::getSourceType, sourceType)
                        .ge(PetAiReport::getCreatedAt, start)
                        .le(PetAiReport::getCreatedAt, end)
                        .last("LIMIT 1"));
        if (existing != null) {
            return toVO(existing);
        }

        // 汇总数据
        SummaryData data = aggregateData(petId, start, end);

        // 调用 LLM 生成总结
        String prompt = buildSummaryPrompt(pet, data, periodLabel);
        String llmResponse;
        try {
            llmResponse = llmClient.chat(SUMMARY_SYSTEM_PROMPT, prompt);
        } catch (Exception e) {
            log.warn("LLM生成总结失败, 使用兜底: {}", e.getMessage());
            llmResponse = buildFallbackJson(data);
        }

        // 解析结果
        PetAiReportVO vo = parseAndSave(userId, pet, data, llmResponse, sourceType, periodLabel, start);
        log.info("活动总结生成完成 petName={}, type={}, riskLevel={}", pet.getPetName(), sourceType, vo.getRiskLevel());
        return vo;
    }

    /**
     * 定时任务：自动生成所有宠物的日报
     */
    public void generateDailySummariesForAll() {
        List<Pet> pets = petMapper.selectList(
                new LambdaQueryWrapper<Pet>().eq(Pet::getDeleted, 0));
        for (Pet pet : pets) {
            try {
                generateSummary(pet.getId(), LocalDate.now().atStartOfDay(),
                        LocalDate.now().atTime(LocalTime.MAX),
                        "DAILY_SUMMARY", LocalDate.now() + " 活动日报");
            } catch (Exception e) {
                log.warn("自动生成日报失败 petId={}: {}", pet.getId(), e.getMessage());
            }
        }
    }

    /**
     * 定时任务：自动生成所有宠物的周报
     */
    public void generateWeeklySummariesForAll() {
        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek().getValue() != 1) {
            return; // 仅周一生成周报
        }
        LocalDate weekStart = today.minusDays(6);
        List<Pet> pets = petMapper.selectList(
                new LambdaQueryWrapper<Pet>().eq(Pet::getDeleted, 0));
        for (Pet pet : pets) {
            try {
                generateSummary(pet.getId(), weekStart.atStartOfDay(),
                        today.atTime(LocalTime.MAX),
                        "WEEKLY_SUMMARY", weekStart + " ~ " + today + " 活动周报");
            } catch (Exception e) {
                log.warn("自动生成周报失败 petId={}: {}", pet.getId(), e.getMessage());
            }
        }
    }

    // ==================== 数据汇总 ====================

    private record SummaryData(
            int totalDetections,
            int petDetectedCount,
            int outOfZoneCount,
            int alarmTriggeredCount,
            int totalAlarms,
            Map<String, Integer> alarmsByType,
            Double avgCoordX,
            Double avgCoordY
    ) {}

    private SummaryData aggregateData(Long petId, LocalDateTime start, LocalDateTime end) {
        // 检测记录统计
        List<PetDetectionRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<PetDetectionRecord>()
                        .eq(PetDetectionRecord::getPetId, petId)
                        .ge(PetDetectionRecord::getDetectTime, start)
                        .le(PetDetectionRecord::getDetectTime, end));

        int totalDetections = records.size();
        int petDetectedCount = (int) records.stream()
                .filter(r -> r.getPetCoordX() != null).count();
        int outOfZoneCount = (int) records.stream()
                .filter(r -> Integer.valueOf(0).equals(r.getInSafeZone())).count();
        int alarmTriggeredCount = (int) records.stream()
                .filter(r -> Integer.valueOf(1).equals(r.getAlarmTriggered())).count();

        // 平均坐标
        List<PetDetectionRecord> detected = records.stream()
                .filter(r -> r.getPetCoordX() != null).toList();
        Double avgX = detected.isEmpty() ? null :
                detected.stream().mapToDouble(PetDetectionRecord::getPetCoordX).average().orElse(0);
        Double avgY = detected.isEmpty() ? null :
                detected.stream().mapToDouble(PetDetectionRecord::getPetCoordY).average().orElse(0);

        // 告警统计
        LambdaQueryWrapper<AlarmMessage> alarmQuery = new LambdaQueryWrapper<AlarmMessage>()
                .eq(AlarmMessage::getSource, "PET_DETECT")
                .eq(AlarmMessage::getDeleted, 0)
                .ge(AlarmMessage::getAlarmTime, start)
                .le(AlarmMessage::getAlarmTime, end);
        List<AlarmMessage> alarms = alarmMapper.selectList(alarmQuery);

        Map<String, Integer> alarmsByType = alarms.stream()
                .collect(Collectors.groupingBy(AlarmMessage::getAlarmType, Collectors.summingInt(a -> 1)));

        return new SummaryData(totalDetections, petDetectedCount, outOfZoneCount,
                alarmTriggeredCount, alarms.size(), alarmsByType, avgX, avgY);
    }

    private String buildSummaryPrompt(Pet pet, SummaryData data, String periodLabel) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下宠物生成").append(periodLabel).append("。\n\n");

        prompt.append("宠物档案：\n");
        prompt.append("- 名称：").append(pet.getPetName()).append('\n');
        prompt.append("- 类型：").append(pet.getPetType()).append('\n');
        if (pet.getAge() != null) {
            prompt.append("- 年龄：").append(pet.getAge()).append("个月\n");
        }

        prompt.append("\n检测统计数据：\n");
        prompt.append("- 总检测次数：").append(data.totalDetections()).append('\n');
        prompt.append("- 检测到宠物次数：").append(data.petDetectedCount()).append('\n');
        prompt.append("- 离开安全区域次数：").append(data.outOfZoneCount()).append('\n');
        prompt.append("- 触发告警次数：").append(data.alarmTriggeredCount()).append('\n');
        prompt.append("- 系统告警总数：").append(data.totalAlarms()).append('\n');

        if (!data.alarmsByType().isEmpty()) {
            prompt.append("- 告警分类：\n");
            data.alarmsByType().forEach((type, count) ->
                    prompt.append("  - ").append(type).append(": ").append(count).append("次\n"));
        }

        if (data.avgCoordX() != null) {
            prompt.append("- 宠物平均位置：(")
                    .append(String.format("%.1f", data.avgCoordX())).append("%, ")
                    .append(String.format("%.1f", data.avgCoordY())).append("%)\n");
        }

        prompt.append("\n请根据数据给出客观分析和建议。");
        return prompt.toString();
    }

    private String buildFallbackJson(SummaryData data) {
        String riskLevel = data.alarmTriggeredCount() > 0 ? "MEDIUM" : "LOW";
        String summary = String.format("本周期共检测%d次，检测到宠物%d次，触发%d次告警。",
                data.totalDetections(), data.petDetectedCount(), data.alarmTriggeredCount());
        return String.format("""
                {"riskLevel":"%s","summary":"%s","observedBehavior":"详见统计数据","evidenceBasis":"基于系统检测记录","recommendations":["建议定期查看监控画面"],"uncertainties":["自动统计未包含视觉分析"]}
                """, riskLevel, summary);
    }

    private PetAiReportVO parseAndSave(Long userId, Pet pet, SummaryData data,
                                       String llmResponse, String sourceType,
                                       String periodLabel, LocalDateTime sourceTime) {
        PetAiReportVO vo;
        try {
            String json = extractJson(llmResponse);
            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<>() {});

            PetAiReport report = new PetAiReport();
            report.setUserId(userId);
            report.setPetId(pet.getId());
            report.setPetName(pet.getPetName());
            report.setSourceType(sourceType);
            report.setSourceId(null);
            report.setSourceTime(sourceTime);
            report.setImageUrl("");
            report.setReportType("SUMMARY");
            report.setRiskLevel(getStr(result, "riskLevel", "LOW"));
            report.setTitle(pet.getPetName() + " · " + periodLabel);
            report.setSummary(getStr(result, "summary", "活动总结已生成"));
            report.setObservedBehavior(getStr(result, "observedBehavior", ""));
            report.setEvidenceBasis(getStr(result, "evidenceBasis", ""));
            report.setRecommendationsJson(writeJson(result.getOrDefault("recommendations", List.of())));
            report.setUncertaintiesJson(writeJson(result.getOrDefault("uncertainties", List.of())));

            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("period", periodLabel);
            evidence.put("totalDetections", data.totalDetections());
            evidence.put("petDetectedCount", data.petDetectedCount());
            evidence.put("outOfZoneCount", data.outOfZoneCount());
            evidence.put("alarmTriggeredCount", data.alarmTriggeredCount());
            evidence.put("totalAlarms", data.totalAlarms());
            evidence.put("alarmsByType", data.alarmsByType());
            report.setEvidenceJson(writeJson(evidence));
            report.setAnalysisJson(llmResponse);
            report.setModelName("deepseek-chat");
            report.setPromptVersion("summary-v1");
            reportMapper.insert(report);

            vo = toVO(report);
        } catch (Exception e) {
            log.error("解析LLM总结结果失败", e);
            throw new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "总结报告生成失败");
        }
        return vo;
    }

    // ==================== 工具方法 ====================

    private Pet requireOwnedPet(Long petId, Long userId) {
        if (petId == null) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "请选择宠物");
        }
        Pet pet = petMapper.selectById(petId);
        if (pet == null || !userId.equals(pet.getUserId())) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "宠物不存在");
        }
        return pet;
    }

    private String extractJson(String content) {
        if (!StringUtils.hasText(content)) return "{}";
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private String getStr(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return val instanceof String s && StringUtils.hasText(s) ? s : fallback;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private PetAiReportVO toVO(PetAiReport report) {
        PetAiReportVO vo = new PetAiReportVO();
        vo.setId(report.getId());
        vo.setPetId(report.getPetId());
        vo.setPetName(report.getPetName());
        vo.setSourceType(report.getSourceType());
        vo.setSourceId(report.getSourceId());
        vo.setSourceTime(report.getSourceTime());
        vo.setImageUrl(report.getImageUrl());
        vo.setReportType(report.getReportType());
        vo.setRiskLevel(report.getRiskLevel());
        vo.setTitle(report.getTitle());
        vo.setSummary(report.getSummary());
        vo.setObservedBehavior(report.getObservedBehavior());
        vo.setEvidenceBasis(report.getEvidenceBasis());
        vo.setRecommendations(readList(report.getRecommendationsJson()));
        vo.setUncertainties(readList(report.getUncertaintiesJson()));
        vo.setEvidence(readMap(report.getEvidenceJson()));
        vo.setModelName(report.getModelName());
        vo.setPromptVersion(report.getPromptVersion());
        vo.setCreatedAt(report.getCreatedAt());
        vo.setUpdatedAt(report.getUpdatedAt());
        return vo;
    }

    private List<String> readList(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> readMap(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
