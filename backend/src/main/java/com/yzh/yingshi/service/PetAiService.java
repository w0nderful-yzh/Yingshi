package com.yzh.yingshi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.dto.PetAnalyzeRequest;
import com.yzh.yingshi.entity.Pet;
import com.yzh.yingshi.mapper.PetMapper;
import com.yzh.yingshi.vo.BehaviorAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 宠物AI分析服务 —— 负责业务逻辑和 Prompt 组装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetAiService {

    private final LlmClient llmClient;
    private final PetMapper petMapper;
    private final ObjectMapper objectMapper;

    private static final String FALLBACK_MESSAGE = "抱歉，AI 分析服务暂时不可用，请稍后再试。";
    private static final Set<String> VALID_STATUSES = Set.of("NORMAL", "ABNORMAL", "UNCERTAIN");
    private static final Set<String> VALID_RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

    // ==================== 三个独立的 System Prompt ====================

    /**
     * 行为分析 Prompt —— 基于结构化检测数据做推断，不声称能"看"图片
     */
    private static final String BEHAVIOR_ANALYSIS_SYSTEM_PROMPT = """
            你是一位专业的宠物行为分析助手，通过分析检测数据来判断宠物行为状态。

            重要规则:
            - 你接收的是 AI 检测算法产生的结构化数据（坐标、时间、运动模式），不是图片。
            - imageUrl 仅作为溯源参考，你不能据此推断画面内容。
            - 基于数据做推断，不确定时如实标注 UNCERTAIN。
            - 必须严格按照 JSON 格式输出，不要包含任何其他文字。

            输出 JSON 格式:
            {
              "status": "NORMAL|ABNORMAL|UNCERTAIN",
              "riskLevel": "LOW|MEDIUM|HIGH",
              "summary": "1-2句话的摘要",
              "evidence": ["数据点1", "数据点2"],
              "possibleCauses": ["可能原因1"],
              "suggestions": ["建议1", "建议2"],
              "needVet": false,
              "confidence": 0.0
            }

            status 判断标准:
            - NORMAL: 宠物在安全区域内，行为模式正常
            - ABNORMAL: 宠物越界、行为模式异常
            - UNCERTAIN: 数据不足以判断

            回答请使用中文。""";

    /**
     * 健康建议 Prompt
     */
    private static final String HEALTH_ADVICE_SYSTEM_PROMPT = """
            你是一位专业的宠物健康护理顾问。
            根据提供的宠物信息和近期活动记录，给出科学、实用的健康与护理建议。

            要求:
            - 回答使用中文，简洁明了
            - 如果主人描述的症状可能提示疾病，建议及时就医
            - 区分"日常护理建议"和"需要就医的警示信号"
            - 不要给出具体药物剂量建议""";

    /**
     * 宠物问答 Prompt
     */
    private static final String PET_CHAT_SYSTEM_PROMPT = """
            你是一位友好的宠物养护助手，擅长回答关于宠物喂养、训练、健康、行为等方面的问题。

            要求:
            - 回答使用中文，语气温暖专业
            - 基于科学养宠知识，不要编造
            - 涉及医疗诊断时，务必建议咨询兽医
            - 回答简洁，通常控制在 300 字以内
            - 使用自然的分段组织内容，每段 2-4 句，段间用空行隔开
            - 涉及列举、步骤或注意事项时，使用编号或 bullet 列表
            - 关键信息（如危险信号、重要提醒）可使用 **加粗** 突出显示""";

    // ==================== 公开方法 ====================

    /**
     * 分析宠物行为 —— 基于检测数据做结构化推断
     */
    public BehaviorAnalysisResult analyzePetBehavior(PetAnalyzeRequest request) {
        Pet pet = resolvePet(request);
        String detectionJson = request.getDetectionJson();

        if (detectionJson == null || detectionJson.isBlank()) {
            log.info("宠物行为分析缺少 detectionJson，直接返回不确定结果");
            return buildMissingDetectionResult(request.getImageUrl());
        }

        StringBuilder userContent = new StringBuilder();
        userContent.append("请根据以下数据判断宠物行为状态。\n\n");

        // 宠物基本信息
        if (pet != null) {
            userContent.append("## 宠物信息\n");
            userContent.append("- 名称：").append(pet.getPetName()).append("\n");
            userContent.append("- 类型：").append(pet.getPetType()).append("\n");
            if (pet.getAge() != null) {
                userContent.append("- 年龄：").append(pet.getAge()).append("岁\n");
            }
            userContent.append("\n");
        } else if (request.getPetName() != null) {
            userContent.append("## 宠物信息\n");
            userContent.append("- 名称：").append(request.getPetName()).append("\n");
            if (request.getPetType() != null) {
                userContent.append("- 类型：").append(request.getPetType()).append("\n");
            }
            userContent.append("\n");
        }

        userContent.append("## AI检测数据\n");
        userContent.append(detectionJson).append("\n\n");

        // 截图URL仅用于溯源
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            userContent.append("## 参考信息\n");
            userContent.append("- 截图URL（仅溯源，不可据此推断画面）：").append(request.getImageUrl()).append("\n\n");
        }

        // 用户额外问题
        if (request.getUserQuestion() != null && !request.getUserQuestion().isBlank()) {
            userContent.append("## 主人提问\n").append(request.getUserQuestion()).append("\n\n");
        }

        userContent.append("请仅基于以上数据输出 JSON 分析结果。");

        String rawJson = callLlm(BEHAVIOR_ANALYSIS_SYSTEM_PROMPT, userContent.toString(), "宠物行为分析");
        return parseBehaviorResult(rawJson, request.getImageUrl());
    }

    /**
     * 根据宠物历史记录给出健康建议
     */
    public String getHealthAdvice(String petName, String recentRecords) {
        StringBuilder userContent = new StringBuilder();
        userContent.append("请根据以下宠物近期活动记录，给出健康和护理建议。\n\n");
        userContent.append("宠物名称：").append(petName).append("\n\n");

        if (recentRecords != null && !recentRecords.isBlank()) {
            userContent.append("近期活动记录：\n").append(recentRecords);
        } else {
            userContent.append("暂无近期活动记录。请基于宠物名称给出一般性护理建议。");
        }

        return callLlm(HEALTH_ADVICE_SYSTEM_PROMPT, userContent.toString(), "健康建议");
    }

    /**
     * 通用聊天（同步）—— 等待完整回复
     */
    public String chat(String userMessage) {
        return callLlm(PET_CHAT_SYSTEM_PROMPT, userMessage, "宠物问答");
    }

    /**
     * 通用聊天（流式）—— 返回 Flux 支持 SSE 流式输出
     */
    public Flux<String> chatStream(String userMessage) {
        log.info("宠物问答(流式): userMessage={}", truncate(userMessage));
        return llmClient.chatStream(PET_CHAT_SYSTEM_PROMPT, userMessage)
                .onErrorResume(e -> {
                    log.error("[宠物问答] LLM 流式调用异常: {}", e.getMessage());
                    return Flux.just(FALLBACK_MESSAGE);
                });
    }

    // ==================== 内部方法 ====================

    private String callLlm(String systemPrompt, String userMessage, String scene) {
        try {
            return llmClient.chat(systemPrompt, userMessage);
        } catch (Exception e) {
            log.error("[{}] LLM 调用失败: {}", scene, e.getMessage());
            return FALLBACK_MESSAGE;
        }
    }

    /**
     * 解析 LLM 返回的 JSON 为 BehaviorAnalysisResult，容错处理
     */
    private BehaviorAnalysisResult parseBehaviorResult(String rawJson, String snapshotUrl) {
        BehaviorAnalysisResult result;
        try {
            result = objectMapper.readValue(rawJson, BehaviorAnalysisResult.class);
        } catch (JsonProcessingException e) {
            log.warn("LLM 返回的 JSON 无法解析，使用降级结果。rawJson={}", truncate(rawJson));
            result = buildFallbackResult(rawJson);
        }
        if (result == null) {
            result = buildFallbackResult(rawJson);
        }
        if (result.getSnapshotUrl() == null) {
            result.setSnapshotUrl(snapshotUrl);
        }
        normalizeBehaviorResult(result);
        return result;
    }

    /**
     * 当 LLM 未返回合法 JSON 时的降级结果
     */
    private BehaviorAnalysisResult buildFallbackResult(String rawText) {
        String safeText = rawText == null ? "" : rawText;
        return BehaviorAnalysisResult.builder()
                .status("UNCERTAIN")
                .riskLevel("LOW")
                .summary("AI 分析结果解析异常，请稍后重试")
                .evidence(List.of("原始响应: " + truncate(safeText)))
                .possibleCauses(List.of())
                .suggestions(List.of("请稍后重新发起分析"))
                .needVet(false)
                .confidence(0.0)
                .build();
    }

    private BehaviorAnalysisResult buildMissingDetectionResult(String snapshotUrl) {
        return BehaviorAnalysisResult.builder()
                .status("UNCERTAIN")
                .riskLevel("LOW")
                .summary("缺少 AI 检测数据，无法可靠判断宠物行为状态。")
                .evidence(List.of("未提供 detectionJson，系统没有坐标、置信度、运动轨迹或区域状态等结构化依据。"))
                .possibleCauses(List.of())
                .suggestions(List.of("请先完成宠物检测，或粘贴检测结果 JSON 后再发起行为分析。"))
                .needVet(false)
                .confidence(0.0)
                .snapshotUrl(snapshotUrl)
                .build();
    }

    private void normalizeBehaviorResult(BehaviorAnalysisResult result) {
        String status = normalizeToken(result.getStatus());
        if (!VALID_STATUSES.contains(status)) {
            result.setStatus("UNCERTAIN");
        } else {
            result.setStatus(status);
        }
        String riskLevel = normalizeToken(result.getRiskLevel());
        if (!VALID_RISK_LEVELS.contains(riskLevel)) {
            result.setRiskLevel("LOW");
        } else {
            result.setRiskLevel(riskLevel);
        }
        if (result.getSummary() == null || result.getSummary().isBlank()) {
            result.setSummary("检测数据不足，无法形成明确结论。");
        }
        if (result.getEvidence() == null) {
            result.setEvidence(List.of());
        }
        if (result.getPossibleCauses() == null) {
            result.setPossibleCauses(List.of());
        }
        if (result.getSuggestions() == null) {
            result.setSuggestions(List.of("请结合实时画面和后续检测结果继续观察。"));
        }
        if (result.getNeedVet() == null) {
            result.setNeedVet(false);
        }
        if (result.getConfidence() == null || result.getConfidence().isNaN()) {
            result.setConfidence(0.0);
        } else if (result.getConfidence() < 0) {
            result.setConfidence(0.0);
        } else if (result.getConfidence() > 1) {
            result.setConfidence(1.0);
        }
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private Pet resolvePet(PetAnalyzeRequest request) {
        if (request.getPetId() != null) {
            return petMapper.selectById(request.getPetId());
        }
        return null;
    }

    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }
}
