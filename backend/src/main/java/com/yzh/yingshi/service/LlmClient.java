package com.yzh.yingshi.service;

import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * LLM API 客户端，基于 Spring AI ChatClient
 */
@Slf4j
@Service
public class LlmClient {

    private final ChatClient chatClient;

    private static final Pattern CODE_BLOCK_PATTERN =
            Pattern.compile("^```(?:json)?\\s*\\n?(.*?)\\n?```\\s*$", Pattern.DOTALL);

    public LlmClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // ==================== 同步调用 ====================

    /**
     * 同步调用 LLM，返回完整回复内容
     */
    public String chat(String systemPrompt, String userMessage) {
        log.debug("LLM 请求: systemPrompt={}, userMessage={}", truncate(systemPrompt), truncate(userMessage));

        try {
            ChatResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .chatResponse();

            String content = extractContent(response);
            log.debug("LLM 响应: {}", truncate(content));
            return content;

        } catch (Exception e) {
            throw mapError(e);
        }
    }

    // ==================== 流式调用 ====================

    /**
     * 流式调用 LLM，通过 SSE 逐 token 输出，用于前端打字机效果
     */
    public Flux<String> chatStream(String systemPrompt, String userMessage) {
        log.debug("LLM 流式请求: userMessage={}", truncate(userMessage));

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content()
                .doOnError(e -> log.error("LLM 流式调用异常: {}", e.getMessage()))
                .onErrorResume(e -> {
                    Throwable mapped = mapError(e);
                    return Flux.just("[错误] " + mapped.getMessage());
                });
    }

    // ==================== 响应内容提取 ====================

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

    private String stripCodeBlock(String text) {
        var matcher = CODE_BLOCK_PATTERN.matcher(text);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return text;
    }

    // ==================== 错误映射（不泄露原始异常信息） ====================

    private BusinessException mapError(Throwable e) {
        if (e instanceof BusinessException bex) {
            return bex;
        }

        if (e instanceof TimeoutException || hasCause(e, TimeoutException.class)) {
            log.error("LLM 请求超时", e);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型响应超时，请稍后重试");
        }

        if (e instanceof ConnectException || hasCause(e, ConnectException.class)) {
            log.error("LLM 网络连接失败", e);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "无法连接模型服务，请检查网络配置");
        }

        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("401") || msg.contains("Unauthorized")) {
                log.error("LLM API Key 无效: {}", msg);
                return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "API Key 无效，请检查配置");
            }
            if (msg.contains("429") || msg.contains("Too Many Requests")) {
                log.warn("LLM 服务限流: {}", msg);
                return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型服务繁忙，请稍后重试");
            }
            if (msg.contains("403") || msg.contains("402") || msg.contains("Forbidden")) {
                log.error("LLM 账户余额不足或无权限: {}", msg);
                return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "账户余额不足或无访问权限");
            }
            if (msg.contains("500") || msg.contains("502") || msg.contains("503")) {
                log.error("LLM 服务端错误: {}", msg);
                return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型服务暂时不可用，请稍后重试");
            }
        }

        // 兜底：不泄露原始异常 message
        log.error("LLM 调用异常: {}", e.getMessage(), e);
        return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型服务异常，请稍后重试");
    }

    private boolean hasCause(Throwable e, Class<? extends Throwable> causeType) {
        Throwable current = e;
        while (current != null) {
            if (causeType.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }
}
