package com.yzh.yingshi.service;

import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * 负责调用 LLM API 的客户端
 * 基于 Spring AI ChatClient，兼容 OpenAI / DeepSeek / 其他 OpenAI 格式 API
 */
@Slf4j
@Service
public class LlmClient {

    private final ChatClient chatClient;

    /** 匹配 markdown 代码块包裹的 JSON */
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("^```(?:json)?\\s*\\n?(.*?)\\n?```\\s*$", Pattern.DOTALL);

    public LlmClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 同步调用 LLM 聊天接口
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return LLM 回复内容
     */
    public String chat(String systemPrompt, String userMessage) {
        log.info("LLM 请求: systemPrompt={}, userMessage={}", truncate(systemPrompt), truncate(userMessage));

        try {
            ChatResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .chatResponse();

            String content = extractContent(response);
            log.info("LLM 响应: {}", truncate(content));
            return content;

        } catch (Exception e) {
            throw mapError(e);
        }
    }

    /**
     * 异步调用 LLM 聊天接口
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return Mono 响应内容
     */
    public Mono<String> chatAsync(String systemPrompt, String userMessage) {
        log.info("LLM 异步请求: systemPrompt={}, userMessage={}", truncate(systemPrompt), truncate(userMessage));

        return Mono.fromCallable(() -> chat(systemPrompt, userMessage))
                .doOnNext(content -> log.info("LLM 异步响应: {}", truncate(content)));
    }

    // ==================== 响应内容提取 ====================

    /**
     * 从 ChatResponse 中提取并清洗文本内容
     */
    private String extractContent(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型返回为空");
        }

        String content = response.getResult().getOutput().getText();
        if (content == null || content.isBlank()) {
            throw new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型返回内容为空");
        }

        return stripCodeBlock(content.trim());
    }

    /**
     * 去掉 LLM 常见的 ```json ... ``` 包裹
     */
    private String stripCodeBlock(String text) {
        var matcher = CODE_BLOCK_PATTERN.matcher(text);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return text;
    }

    // ==================== 错误映射 ====================

    /**
     * 统一异常映射为 BusinessException
     */
    private BusinessException mapError(Throwable e) {
        if (e instanceof BusinessException bex) {
            return bex;
        }

        // 超时
        if (e instanceof TimeoutException || hasCause(e, TimeoutException.class)) {
            log.error("LLM 请求超时", e);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型响应超时，请稍后重试");
        }

        // 网络连接失败
        if (e instanceof ConnectException || hasCause(e, ConnectException.class)) {
            log.error("LLM 网络连接失败", e);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "无法连接模型服务，请检查网络配置");
        }

        // 检查是否是 API Key 相关错误
        String msg = e.getMessage();
        if (msg != null && (msg.contains("401") || msg.contains("Unauthorized"))) {
            log.error("LLM API Key 无效: {}", msg);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "API Key 无效，请检查配置");
        }
        if (msg != null && (msg.contains("429") || msg.contains("Too Many Requests"))) {
            log.warn("LLM 服务限流: {}", msg);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型服务繁忙，请稍后重试");
        }
        if (msg != null && (msg.contains("403") || msg.contains("402") || msg.contains("Forbidden"))) {
            log.error("LLM 账户余额不足或无权限: {}", msg);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "账户余额不足或无访问权限");
        }
        if (msg != null && (msg.contains("500") || msg.contains("502") || msg.contains("503"))) {
            log.error("LLM 服务端错误: {}", msg);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型服务暂时不可用，请稍后重试");
        }

        log.error("LLM 调用异常: {}", e.getMessage(), e);
        return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型调用异常: " + e.getMessage());
    }

    private boolean hasCause(Throwable e, Class<? extends Throwable> causeType) {
        Throwable current = e;
        while (current != null) {
            if (causeType.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    // ==================== 工具方法 ====================

    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
