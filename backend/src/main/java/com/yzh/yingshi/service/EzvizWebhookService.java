package com.yzh.yingshi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.config.EzvizProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EzvizWebhookService {

    private static final String HMAC_SHA1 = "HmacSHA1";
    private static final String ALARM_MESSAGE_TYPE = "ys.alarm";

    private final EzvizProperties properties;
    private final ObjectMapper objectMapper;
    private final AlarmService alarmService;

    public String handle(String rawBody, String timestamp, String signature, String headerMessageType) {
        verifySignature(rawBody, timestamp, signature);

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "Webhook请求体不是合法JSON");
        }

        JsonNode header = root.path("header");
        String messageId = firstText(List.of(header, root), "messageId", "message_id");
        if (!StringUtils.hasText(messageId)) {
            throw new BusinessException(BusinessCode.PARAM_INVALID, "Webhook缺少messageId");
        }

        String messageType = firstNonBlank(
                firstText(List.of(header, root), "type", "messageType", "message_type"),
                headerMessageType);
        if (ALARM_MESSAGE_TYPE.equalsIgnoreCase(messageType)) {
            Map<String, Object> alarm = normalizeAlarm(root, header, messageId);
            boolean inserted = alarmService.receiveEzvizWebhook(alarm, rawBody);
            log.info("处理萤石告警推送 messageId={}, deviceSerial={}, inserted={}",
                    messageId, alarm.get("deviceSerial"), inserted);
        } else {
            log.info("确认萤石非告警推送 messageId={}, messageType={}", messageId, messageType);
        }
        return messageId;
    }

    private void verifySignature(String rawBody, String timestamp, String signature) {
        String secret = properties.getWebhook().getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new BusinessException(BusinessCode.INTERNAL_ERROR, "萤石Webhook签名密钥未配置");
        }
        if (!StringUtils.hasText(rawBody)
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(signature)) {
            throw new BusinessException(BusinessCode.UNAUTHORIZED, "Webhook签名参数不完整");
        }

        long sentAt;
        try {
            sentAt = Long.parseLong(timestamp);
            if (sentAt < 1_000_000_000_000L) {
                sentAt *= 1000;
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(BusinessCode.UNAUTHORIZED, "Webhook时间戳无效");
        }

        long maxSkewMillis = properties.getWebhook().getMaxClockSkewSeconds() * 1000L;
        if (Math.abs(System.currentTimeMillis() - sentAt) > maxSkewMillis) {
            throw new BusinessException(BusinessCode.UNAUTHORIZED, "Webhook请求已过期");
        }

        String expected = hmacSha1Hex(secret, rawBody + timestamp);
        byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
        byte[] actualBytes = signature.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
            throw new BusinessException(BusinessCode.UNAUTHORIZED, "Webhook签名校验失败");
        }
    }

    private Map<String, Object> normalizeAlarm(JsonNode root, JsonNode header, String messageId) {
        JsonNode body = parseEmbeddedJson(root.path("body"));
        JsonNode data = parseEmbeddedJson(body.path("data"));
        List<JsonNode> sources = List.of(data, body, root);

        Map<String, Object> alarm = new LinkedHashMap<>();
        alarm.put("alarmId", messageId);
        alarm.put("deviceSerial", firstNonBlank(
                firstText(List.of(header, root), "deviceId", "deviceSerial"),
                firstText(sources, "deviceId", "deviceSerial")));
        alarm.put("channelNo", firstNonBlank(
                firstText(List.of(header, root), "channelNo", "channel"),
                firstText(sources, "channelNo", "channel")));
        alarm.put("alarmType", firstText(sources,
                "alarmType", "alarm_type", "eventType", "event_type", "type"));
        alarm.put("alarmName", firstText(sources,
                "alarmName", "alarm_name", "eventName", "event_name", "name"));
        alarm.put("alarmPicUrl", firstText(sources,
                "alarmPicUrl", "alarmPicURI", "picUrl", "pictureUrl", "imageUrl", "pic_url"));
        alarm.put("alarmContent", firstText(sources,
                "alarmContent", "alarm_content", "content", "message", "msg", "description"));
        alarm.put("alarmTime", firstNonBlank(
                firstText(sources, "alarmTime", "alarm_time", "createTime", "startTime", "time"),
                firstText(List.of(header), "messageTime")));
        return alarm;
    }

    private JsonNode parseEmbeddedJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return objectMapper.createObjectNode();
        }
        if (!node.isTextual()) {
            return node;
        }
        String value = node.asText();
        if (!StringUtils.hasText(value)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private String firstText(List<JsonNode> nodes, String... fields) {
        for (JsonNode node : nodes) {
            if (node == null || !node.isObject()) {
                continue;
            }
            for (String field : fields) {
                JsonNode value = node.get(field);
                if (value != null && !value.isNull() && StringUtils.hasText(value.asText())) {
                    return value.asText();
                }
            }
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String hmacSha1Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA1);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA1));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new BusinessException(BusinessCode.INTERNAL_ERROR, "Webhook验签失败");
        }
    }
}
