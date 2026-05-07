package com.yzh.yingshi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.config.LlmProperties;
import com.yzh.yingshi.dto.LlmChatRequest;
import com.yzh.yingshi.dto.LlmChatResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * 负责调用 LLM API 的客户端
 * 兼容 OpenAI / DeepSeek / 其他 OpenAI 格式 API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClient {

    private final LlmProperties llmProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private WebClient webClient;

    /** 匹配 markdown 代码块包裹的 JSON */
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("^```(?:json)?\\s*\\n?(.*?)\\n?```\\s*$", Pattern.DOTALL);

    /** 请求超时时间 */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder
                .baseUrl(llmProperties.getBaseUrl())
                .build();
    }

    /**
     * 同步调用 LLM 聊天接口
     *
     * @param request 请求体
     * @return 响应体
     */
    public LlmChatResponse chat(LlmChatRequest request) {
        fillDefaults(request);
        log.info("LLM 请求: model={}, messages={}", request.getModel(), request.getMessages().size());

        try {
            LlmChatResponse response = webClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + llmProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LlmChatResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            // 清洗返回内容
            if (response != null) {
                cleanResponseContent(response);
                if (response.getUsage() != null) {
                    log.info("LLM 响应: tokens={}", response.getUsage().getTotalTokens());
                }
            }
            return response;

        } catch (WebClientResponseException e) {
            throw mapHttpError(e);
        } catch (Exception e) {
            throw mapGeneralError(e);
        }
    }

    /**
     * 异步调用 LLM 聊天接口
     *
     * @param request 请求体
     * @return Mono 响应
     */
    public Mono<LlmChatResponse> chatAsync(LlmChatRequest request) {
        fillDefaults(request);
        log.info("LLM 异步请求: model={}, messages={}", request.getModel(), request.getMessages().size());

        return webClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + llmProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(LlmChatResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .doOnSuccess(response -> {
                    if (response != null) {
                        cleanResponseContent(response);
                        if (response.getUsage() != null) {
                            log.info("LLM 异步响应: tokens={}", response.getUsage().getTotalTokens());
                        }
                    }
                })
                .onErrorMap(WebClientResponseException.class, this::mapHttpError)
                .onErrorMap(e -> !(e instanceof BusinessException), this::mapGeneralError);
    }

    // ==================== 响应内容清洗 ====================

    /**
     * 清洗 LLM 返回内容：去掉 markdown 代码块包裹、首尾空白
     */
    private void cleanResponseContent(LlmChatResponse response) {
        if (response.getChoices() == null) return;

        for (LlmChatResponse.Choice choice : response.getChoices()) {
            if (choice.getMessage() == null || choice.getMessage().getContent() == null) continue;

            String raw = choice.getMessage().getContent();
            String cleaned = stripCodeBlock(raw.trim());
            choice.getMessage().setContent(cleaned);
        }
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
     * HTTP 状态码错误 → BusinessException
     */
    private BusinessException mapHttpError(WebClientResponseException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String responseBody = e.getResponseBodyAsString();
        String apiMessage = extractErrorMessage(responseBody);

        if (status == HttpStatus.UNAUTHORIZED) {
            log.error("LLM API Key 无效或已过期: {}", apiMessage);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "API Key 无效，请检查配置");
        }
        if (status == HttpStatus.FORBIDDEN || status == HttpStatus.PAYMENT_REQUIRED) {
            log.error("LLM 账户余额不足或无权限: {}", apiMessage);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "账户余额不足或无访问权限");
        }
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            log.warn("LLM 服务限流: {}", apiMessage);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型服务繁忙，请稍后重试");
        }
        if (status != null && status.is5xxServerError()) {
            log.error("LLM 服务端错误 [{}]: {}", status.value(), apiMessage);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型服务暂时不可用，请稍后重试");
        }

        log.error("LLM 请求失败 [{}]: {}", status, apiMessage);
        return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型调用失败: " + apiMessage);
    }

    /**
     * 通用异常 → BusinessException
     */
    private BusinessException mapGeneralError(Throwable e) {
        // 超时
        if (e instanceof TimeoutException || isTimeoutCause(e)) {
            log.error("LLM 请求超时", e);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型响应超时，请稍后重试");
        }
        // 网络连接失败
        if (e instanceof ConnectException || hasCause(e, ConnectException.class)) {
            log.error("LLM 网络连接失败", e);
            return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "无法连接模型服务，请检查网络配置");
        }
        // 已经是 BusinessException 直接抛出
        if (e instanceof BusinessException bex) {
            return bex;
        }

        log.error("LLM 调用异常: {}", e.getMessage(), e);
        return new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "模型调用异常: " + e.getMessage());
    }

    /**
     * 尝试从 API 错误响应体中提取 error message
     */
    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // OpenAI / DeepSeek 格式: {"error": {"message": "...", "type": "..."}}
            JsonNode errorNode = root.get("error");
            if (errorNode != null && errorNode.has("message")) {
                return errorNode.get("message").asText();
            }
            // 其他格式: {"message": "..."}
            if (root.has("message")) {
                return root.get("message").asText();
            }
        } catch (Exception ignored) {
        }
        return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
    }

    private boolean isTimeoutCause(Throwable e) {
        return hasCause(e, TimeoutException.class);
    }

    private boolean hasCause(Throwable e, Class<? extends Throwable> causeType) {
        Throwable current = e;
        while (current != null) {
            if (causeType.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    // ==================== 默认值填充 ====================

    private void fillDefaults(LlmChatRequest request) {
        if (request.getModel() == null) {
            request.setModel(llmProperties.getModel());
        }
        if (request.getTemperature() == null) {
            request.setTemperature(0.7);
        }
        if (request.getMaxTokens() == null) {
            request.setMaxTokens(2048);
        }
    }
}
