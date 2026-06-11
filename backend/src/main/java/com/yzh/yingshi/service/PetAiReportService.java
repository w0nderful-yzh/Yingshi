package com.yzh.yingshi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.auth.CurrentUserService;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.dto.PetAiReportGenerateRequest;
import com.yzh.yingshi.dto.PetAiVisionResult;
import com.yzh.yingshi.entity.AlarmMessage;
import com.yzh.yingshi.entity.Pet;
import com.yzh.yingshi.entity.PetAiReport;
import com.yzh.yingshi.entity.PetDetectionConfig;
import com.yzh.yingshi.entity.PetDetectionRecord;
import com.yzh.yingshi.mapper.AlarmMessageMapper;
import com.yzh.yingshi.mapper.PetAiReportMapper;
import com.yzh.yingshi.mapper.PetDetectionConfigMapper;
import com.yzh.yingshi.mapper.PetDetectionRecordMapper;
import com.yzh.yingshi.mapper.PetMapper;
import com.yzh.yingshi.vo.PetAiReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PetAiReportService {

    private static final String SOURCE_ALARM = "ALARM";
    private static final String SOURCE_DETECTION = "DETECTION";
    private static final String SOURCE_IMAGE = "IMAGE";
    private static final String PROMPT_VERSION = "pet-event-v2";

    private static final String SYSTEM_PROMPT = """
            你是宠物居家监护系统的多模态分析员。请结合图片、宠物档案和系统检测事实生成谨慎、可追溯的事件分析。
            系统给出的告警时间、安全区状态、坐标和告警是否触发属于确定事实，不得被图片推测覆盖。
            不要声称可以确诊疾病；出现可能的健康风险时，只能建议持续观察或咨询专业兽医。
            只返回合法JSON，不要使用Markdown代码块。JSON字段必须为：
            riskLevel: LOW、MEDIUM或HIGH；
            summary: 一句话事件摘要；
            observedBehavior: 图片中可观察到的宠物姿态、行为和环境；
            evidenceBasis: 结论依据，区分图片观察与系统事实；
            recommendations: 字符串数组，给主人的行动建议；
            uncertainties: 字符串数组，列出遮挡、画质、单帧局限等不确定性。
            """;

    private final PetAiReportMapper reportMapper;
    private final PetMapper petMapper;
    private final AlarmMessageMapper alarmMapper;
    private final PetDetectionRecordMapper detectionRecordMapper;
    private final PetDetectionConfigMapper detectionConfigMapper;
    private final CurrentUserService currentUserService;
    private final MimoMultimodalClient multimodalClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public PetAiReportVO generate(PetAiReportGenerateRequest request) {
        currentUserService.requireWriteAccess();
        Long userId = currentUserService.requireCurrentUserId();
        EvidenceContext context = resolveEvidence(request, userId);

        if (!Boolean.TRUE.equals(request.getRegenerate()) && context.sourceId() != null) {
            PetAiReport existing = reportMapper.selectOne(
                    new LambdaQueryWrapper<PetAiReport>()
                            .eq(PetAiReport::getUserId, userId)
                            .eq(PetAiReport::getPetId, context.pet().getId())
                            .eq(PetAiReport::getSourceType, context.sourceType())
                            .eq(PetAiReport::getSourceId, context.sourceId())
                            .orderByDesc(PetAiReport::getCreatedAt)
                            .last("LIMIT 1")
            );
            if (existing != null) {
                return toVO(existing);
            }
        }

        String prompt = buildPrompt(context, request.getQuestion());
        PetAiVisionResult analysis = multimodalClient.analyze(
                List.of(context.imageUrl()),
                SYSTEM_PROMPT,
                prompt
        );
        normalizeAnalysis(analysis);

        PetAiReport report = new PetAiReport();
        report.setUserId(userId);
        report.setPetId(context.pet().getId());
        report.setPetName(context.pet().getPetName());
        report.setSourceType(context.sourceType());
        report.setSourceId(context.sourceId());
        report.setSourceTime(context.sourceTime());
        report.setImageUrl(context.imageUrl());
        report.setReportType("EVENT");
        report.setRiskLevel(analysis.getRiskLevel());
        report.setTitle(buildTitle(context));
        report.setSummary(analysis.getSummary());
        report.setObservedBehavior(analysis.getObservedBehavior());
        report.setEvidenceBasis(analysis.getEvidenceBasis());
        report.setRecommendationsJson(writeJson(analysis.getRecommendations()));
        report.setUncertaintiesJson(writeJson(analysis.getUncertainties()));
        report.setEvidenceJson(writeJson(context.evidence()));
        report.setAnalysisJson(writeJson(analysis));
        report.setModelName(multimodalClient.getModel());
        report.setPromptVersion(PROMPT_VERSION);
        reportMapper.insert(report);
        return toVO(report);
    }

    public List<PetAiReportVO> list(Long petId, String sourceType, String riskLevel) {
        Long userId = currentUserService.requireCurrentUserId();
        LambdaQueryWrapper<PetAiReport> query = new LambdaQueryWrapper<PetAiReport>()
                .eq(PetAiReport::getUserId, userId);
        if (petId != null) {
            Pet pet = requireOwnedPet(petId);
            query.eq(PetAiReport::getPetId, pet.getId());
        }
        if (StringUtils.hasText(sourceType)) {
            query.eq(PetAiReport::getSourceType, normalizeSourceType(sourceType));
        }
        if (StringUtils.hasText(riskLevel)) {
            query.eq(PetAiReport::getRiskLevel, riskLevel.trim().toUpperCase(Locale.ROOT));
        }
        query.orderByDesc(PetAiReport::getCreatedAt);
        return reportMapper.selectList(query).stream().map(this::toVO).toList();
    }

    public PetAiReportVO detail(Long id) {
        PetAiReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "AI分析报告不存在");
        }
        if (!currentUserService.requireCurrentUserId().equals(report.getUserId())) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "无权访问该AI分析报告");
        }
        return toVO(report);
    }

    private EvidenceContext resolveEvidence(PetAiReportGenerateRequest request, Long userId) {
        String sourceType = normalizeSourceType(request.getSourceType());
        return switch (sourceType) {
            case SOURCE_ALARM -> resolveAlarm(request);
            case SOURCE_DETECTION -> resolveDetection(request, userId);
            case SOURCE_IMAGE -> resolveManualImage(request);
            default -> throw new BusinessException(BusinessCode.PARAM_INVALID, "不支持的来源类型");
        };
    }

    private EvidenceContext resolveAlarm(PetAiReportGenerateRequest request) {
        if (request.getSourceId() == null) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "告警ID不能为空");
        }
        AlarmMessage alarm = alarmMapper.selectById(request.getSourceId());
        if (alarm == null || Integer.valueOf(1).equals(alarm.getDeleted())) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "告警不存在");
        }
        if (alarm.getDeviceId() != null) {
            currentUserService.assertDeviceAccessible(alarm.getDeviceId());
        } else if (!currentUserService.getAuthorizedDeviceSerials().contains(alarm.getDeviceSerial())) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "无权访问该告警");
        }
        Pet pet = requireOwnedPet(request.getPetId());
        if (!StringUtils.hasText(alarm.getAlarmPicUrl())) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "该告警没有可分析的图片");
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("alarmType", alarm.getAlarmType());
        evidence.put("alarmName", alarm.getAlarmName());
        evidence.put("alarmContent", alarm.getAlarmContent());
        evidence.put("alarmTime", alarm.getAlarmTime());
        evidence.put("deviceSerial", alarm.getDeviceSerial());
        evidence.put("source", alarm.getSource());
        return new EvidenceContext(
                SOURCE_ALARM, alarm.getId(), alarm.getAlarmTime(), alarm.getAlarmPicUrl(), pet, evidence
        );
    }

    private EvidenceContext resolveDetection(PetAiReportGenerateRequest request, Long userId) {
        if (request.getSourceId() == null) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "检测记录ID不能为空");
        }
        PetDetectionRecord record = detectionRecordMapper.selectById(request.getSourceId());
        if (record == null) {
            throw new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "检测记录不存在");
        }
        PetDetectionConfig config = detectionConfigMapper.selectById(record.getDetectionConfigId());
        if (config == null || !userId.equals(config.getUserId())) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "无权访问该检测记录");
        }
        Pet pet = requireOwnedPet(record.getPetId());
        if (request.getPetId() != null && !request.getPetId().equals(pet.getId())) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "检测记录与所选宠物不匹配");
        }
        if (!StringUtils.hasText(record.getSnapshotUrl())) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "该检测记录没有可分析的快照");
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("detectTime", record.getDetectTime());
        evidence.put("inSafeZone", Integer.valueOf(1).equals(record.getInSafeZone()));
        evidence.put("alarmTriggered", Integer.valueOf(1).equals(record.getAlarmTriggered()));
        evidence.put("boundingBox", Map.of(
                "x", valueOrZero(record.getPetCoordX()),
                "y", valueOrZero(record.getPetCoordY()),
                "width", valueOrZero(record.getPetWidth()),
                "height", valueOrZero(record.getPetHeight())
        ));
        evidence.put("ezvizDetection", record.getAiResultJson());
        evidence.put("deviceSerial", record.getDeviceSerial());
        return new EvidenceContext(
                SOURCE_DETECTION, record.getId(), record.getDetectTime(), record.getSnapshotUrl(), pet, evidence
        );
    }

    private EvidenceContext resolveManualImage(PetAiReportGenerateRequest request) {
        Pet pet = requireOwnedPet(request.getPetId());
        if (!StringUtils.hasText(request.getImageUrl())) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "图片地址不能为空");
        }
        String imageUrl = request.getImageUrl().trim();
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "图片地址格式不正确");
        }
        if (imageUrl.length() > 2000) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "图片地址过长");
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("source", "manual_image");
        evidence.put("submittedAt", LocalDateTime.now());
        return new EvidenceContext(SOURCE_IMAGE, null, LocalDateTime.now(), imageUrl, pet, evidence);
    }

    private Pet requireOwnedPet(Long petId) {
        if (petId == null) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "请选择宠物");
        }
        Pet pet = petMapper.selectById(petId);
        currentUserService.assertPetOwned(pet);
        return pet;
    }

    private String buildPrompt(EvidenceContext context, String question) {
        Pet pet = context.pet();
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析这次宠物监护事件。\n\n");
        prompt.append("宠物档案：\n");
        prompt.append("- 名称：").append(pet.getPetName()).append('\n');
        prompt.append("- 类型：").append(pet.getPetType()).append('\n');
        if (pet.getAge() != null) {
            prompt.append("- 年龄：").append(pet.getAge()).append("个月\n");
        }
        if (StringUtils.hasText(pet.getGender())) {
            prompt.append("- 性别：").append(pet.getGender()).append('\n');
        }
        if (StringUtils.hasText(pet.getRemark())) {
            prompt.append("- 主人备注：").append(pet.getRemark()).append('\n');
        }
        prompt.append("\n系统证据（确定事实）：\n").append(writeJson(context.evidence())).append('\n');
        if (StringUtils.hasText(question)) {
            prompt.append("\n用户重点关注：").append(question.trim()).append('\n');
        }
        prompt.append("\n请只描述图片中可见内容，不要把看不到的行为当作事实。");
        return prompt.toString();
    }

    private void normalizeAnalysis(PetAiVisionResult analysis) {
        if (analysis == null) {
            throw new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "MiMo 未返回分析结果");
        }
        String risk = StringUtils.hasText(analysis.getRiskLevel())
                ? analysis.getRiskLevel().trim().toUpperCase(Locale.ROOT)
                : "MEDIUM";
        if (!List.of("LOW", "MEDIUM", "HIGH").contains(risk)) {
            risk = "MEDIUM";
        }
        analysis.setRiskLevel(risk);
        analysis.setSummary(defaultText(analysis.getSummary(), "已完成本次宠物事件分析"));
        analysis.setObservedBehavior(defaultText(analysis.getObservedBehavior(), "画面信息不足，未能确认具体行为"));
        analysis.setEvidenceBasis(defaultText(analysis.getEvidenceBasis(), "结合监控图片与系统事件记录生成"));
        analysis.setRecommendations(defaultList(analysis.getRecommendations(), "建议查看原始画面并持续观察宠物状态"));
        analysis.setUncertainties(defaultList(analysis.getUncertainties(), "单张监控画面无法反映完整行为过程"));
    }

    private String buildTitle(EvidenceContext context) {
        String sourceLabel = switch (context.sourceType()) {
            case SOURCE_ALARM -> "告警";
            case SOURCE_DETECTION -> "检测";
            default -> "图像";
        };
        return context.pet().getPetName() + " · " + sourceLabel + "事件分析";
    }

    private String normalizeSourceType(String sourceType) {
        return sourceType == null ? "" : sourceType.trim().toUpperCase(Locale.ROOT);
    }

    private double valueOrZero(Double value) {
        return value == null ? 0D : value;
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
            throw new BusinessException(BusinessCode.INTERNAL_ERROR, "报告数据序列化失败");
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
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> readMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private record EvidenceContext(
            String sourceType,
            Long sourceId,
            LocalDateTime sourceTime,
            String imageUrl,
            Pet pet,
            Map<String, Object> evidence
    ) {}
}
