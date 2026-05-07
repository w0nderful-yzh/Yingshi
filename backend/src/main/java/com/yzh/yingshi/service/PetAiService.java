package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.LlmChatRequest;
import com.yzh.yingshi.dto.LlmChatResponse;
import com.yzh.yingshi.dto.PetAnalyzeRequest;
import com.yzh.yingshi.entity.Pet;
import com.yzh.yingshi.mapper.PetDetectionRecordMapper;
import com.yzh.yingshi.mapper.PetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 宠物AI分析服务 —— 负责业务逻辑和 Prompt 组装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetAiService {

    private final LlmClient llmClient;
    private final PetMapper petMapper;
    private final PetDetectionRecordMapper petDetectionRecordMapper;

    /** 调用失败时返回给前端的兜底文案 */
    private static final String FALLBACK_MESSAGE = "抱歉，AI 分析服务暂时不可用，请稍后再试。";

    /**
     * 分析宠物行为：根据截图和检测结果，让 LLM 判断是否存在异常
     */
    public String analyzePetBehavior(PetAnalyzeRequest request) {
        Pet pet = resolvePet(request);

        List<LlmChatRequest.Message> messages = new ArrayList<>();
        messages.add(buildSystemPrompt());

        StringBuilder userContent = new StringBuilder();
        userContent.append("请分析以下宠物监控截图，判断宠物行为是否正常。\n\n");
        userContent.append("截图地址：").append(request.getImageUrl()).append("\n");

        if (pet != null) {
            userContent.append("宠物名称：").append(pet.getPetName()).append("\n");
            userContent.append("宠物类型：").append(pet.getPetType()).append("\n");
            if (pet.getAge() != null) {
                userContent.append("年龄：").append(pet.getAge()).append("岁\n");
            }
        } else if (request.getPetName() != null) {
            userContent.append("宠物名称：").append(request.getPetName()).append("\n");
            if (request.getPetType() != null) {
                userContent.append("宠物类型：").append(request.getPetType()).append("\n");
            }
        }

        if (request.getDetectionJson() != null && !request.getDetectionJson().isBlank()) {
            userContent.append("\nAI视觉检测原始数据：\n").append(request.getDetectionJson()).append("\n");
        }

        if (request.getUserQuestion() != null && !request.getUserQuestion().isBlank()) {
            userContent.append("\n用户额外问题：").append(request.getUserQuestion()).append("\n");
        }

        userContent.append("\n请从以下角度分析：\n");
        userContent.append("1. 宠物当前可能在做什么\n");
        userContent.append("2. 行为是否正常\n");
        userContent.append("3. 如果有异常，给出可能的原因和建议\n");

        messages.add(LlmChatRequest.Message.builder()
                .role("user")
                .content(userContent.toString())
                .build());

        return callLlm(messages, "宠物行为分析");
    }

    /**
     * 根据宠物历史记录给出健康建议
     */
    public String getHealthAdvice(String petName, String recentRecords) {
        List<LlmChatRequest.Message> messages = new ArrayList<>();
        messages.add(buildSystemPrompt());

        StringBuilder userContent = new StringBuilder();
        userContent.append("请根据以下宠物近期活动记录，给出健康和护理建议。\n\n");
        userContent.append("宠物名称：").append(petName).append("\n\n");

        if (recentRecords != null && !recentRecords.isBlank()) {
            userContent.append("近期活动记录：\n").append(recentRecords);
        } else {
            userContent.append("暂无近期活动记录。");
        }

        messages.add(LlmChatRequest.Message.builder()
                .role("user")
                .content(userContent.toString())
                .build());

        return callLlm(messages, "健康建议");
    }

    /**
     * 通用聊天：用户自由提问关于宠物的问题
     */
    public String chat(String userMessage) {
        List<LlmChatRequest.Message> messages = new ArrayList<>();
        messages.add(buildSystemPrompt());
        messages.add(LlmChatRequest.Message.builder()
                .role("user")
                .content(userMessage)
                .build());

        return callLlm(messages, "宠物问答");
    }

    // ==================== 内部方法 ====================

    /**
     * 统一调用 LLM 并兜底
     */
    private String callLlm(List<LlmChatRequest.Message> messages, String scene) {
        try {
            LlmChatResponse response = llmClient.chat(buildRequest(messages));
            return extractContent(response);
        } catch (Exception e) {
            log.error("[{}] LLM 调用失败，返回兜底文案: {}", scene, e.getMessage());
            return FALLBACK_MESSAGE;
        }
    }

    /**
     * 构建系统提示词
     */
    private LlmChatRequest.Message buildSystemPrompt() {
        return LlmChatRequest.Message.builder()
                .role("system")
                .content("你是一位专业的宠物行为分析助手，擅长通过监控画面判断宠物的行为状态。"
                        + "你的职责包括：分析宠物行为是否正常、识别潜在的异常行为（如焦虑、生病迹象、破坏行为等）、"
                        + "提供宠物健康护理建议。请用友好、专业的方式回答，必要时建议主人及时就医或采取行动。"
                        + "回答请使用中文，简洁明了。")
                .build();
    }

    /**
     * 构建 LLM 请求（默认参数）
     */
    private LlmChatRequest buildRequest(List<LlmChatRequest.Message> messages) {
        return LlmChatRequest.builder()
                .messages(messages)
                .temperature(0.7)
                .maxTokens(2048)
                .build();
    }

    /**
     * 从响应中提取并清洗文本内容
     */
    private String extractContent(LlmChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return FALLBACK_MESSAGE;
        }
        String content = response.getFirstContent();
        if (content == null || content.isBlank()) {
            return FALLBACK_MESSAGE;
        }
        return content;
    }

    /**
     * 解析宠物信息：优先用 petId 查库
     */
    private Pet resolvePet(PetAnalyzeRequest request) {
        if (request.getPetId() != null) {
            return petMapper.selectById(request.getPetId());
        }
        return null;
    }
}
