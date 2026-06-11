package com.yzh.yingshi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.dto.PetAiVisionResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MimoMultimodalClient {

    private static final Pattern CODE_BLOCK_PATTERN =
            Pattern.compile("^```(?:json)?\\s*\\n?(.*?)\\n?```\\s*$", Pattern.DOTALL);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    @Getter
    private final String model;

    private final int maxCompletionTokens;

    public MimoMultimodalClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.ai.mimo.base-url:https://token-plan-cn.xiaomimimo.com/v1}") String baseUrl,
            @Value("${app.ai.mimo.api-key:}") String apiKey,
            @Value("${app.ai.mimo.model:mimo-v2.5}") String model,
            @Value("${app.ai.mimo.max-completion-tokens:1600}") int maxCompletionTokens) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.maxCompletionTokens = maxCompletionTokens;
    }

    public PetAiVisionResult analyze(List<String> imageUrls, String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "MIMO_API_KEY 未配置");
        }
        List<String> validImages = imageUrls == null ? List.of() : imageUrls.stream()
                .filter(StringUtils::hasText)
                .limit(6)
                .toList();
        if (validImages.isEmpty()) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "没有可供分析的图片");
        }

        List<Map<String, Object>> userContent = new ArrayList<>();
        for (String imageUrl : validImages) {
            userContent.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", imageUrl)
            ));
        }
        userContent.add(Map.of("type", "text", "text", userPrompt));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        ));
        body.put("max_completion_tokens", maxCompletionTokens);
        body.put("temperature", 0.2);
        body.put("stream", false);

        try {
            JsonNode response = restClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String content = extractContent(response);
            return objectMapper.readValue(extractJson(stripCodeBlock(content)), PetAiVisionResult.class);
        } catch (RestClientResponseException e) {
            log.error("MiMo多模态请求失败 status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(BusinessCode.MODEL_SERVICE_ERROR,
                    "MiMo 多模态服务调用失败: HTTP " + e.getStatusCode().value());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("MiMo多模态响应解析失败", e);
            throw new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "MiMo 多模态结果解析失败");
        }
    }

    private String extractContent(JsonNode response) {
        JsonNode content = response == null ? null : response.at("/choices/0/message/content");
        if (content == null || !content.isTextual() || !StringUtils.hasText(content.asText())) {
            throw new BusinessException(BusinessCode.MODEL_SERVICE_ERROR, "MiMo 返回内容为空");
        }
        return content.asText().trim();
    }

    private String stripCodeBlock(String content) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(content);
        return matcher.matches() ? matcher.group(1).trim() : content;
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }
}
