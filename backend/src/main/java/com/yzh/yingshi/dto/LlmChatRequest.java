package com.yzh.yingshi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * LLM 聊天请求体 (兼容 OpenAI / DeepSeek API 格式)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatRequest {

    private String model;

    private List<Message> messages;

    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /** 角色: system / user / assistant */
        private String role;
        private String content;
    }
}
