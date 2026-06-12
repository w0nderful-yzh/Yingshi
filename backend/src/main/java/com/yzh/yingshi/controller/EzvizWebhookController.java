package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.service.EzvizWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ezviz")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ezviz.webhook.enabled", havingValue = "true")
public class EzvizWebhookController {

    private final EzvizWebhookService webhookService;

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> receive(
            @RequestBody String rawBody,
            @RequestHeader(name = "t", required = false) String timestamp,
            @RequestHeader(name = "signature", required = false) String signature,
            @RequestHeader(name = "message_type", required = false) String messageType) {
        try {
            String messageId = webhookService.handle(rawBody, timestamp, signature, messageType);
            return ResponseEntity.ok(Map.of("messageId", messageId));
        } catch (BusinessException e) {
            HttpStatus status = switch (e.getBusinessCode()) {
                case UNAUTHORIZED, FORBIDDEN -> HttpStatus.UNAUTHORIZED;
                case PARAM_INVALID -> HttpStatus.BAD_REQUEST;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
            log.warn("拒绝萤石Webhook status={}, reason={}", status.value(), e.getMessage());
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("处理萤石Webhook异常", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Webhook处理失败"));
        }
    }
}
