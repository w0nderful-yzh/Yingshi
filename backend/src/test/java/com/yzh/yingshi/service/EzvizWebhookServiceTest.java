package com.yzh.yingshi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.config.EzvizProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EzvizWebhookServiceTest {

    private static final String SECRET = "webhook-test-secret";

    private AlarmService alarmService;
    private EzvizWebhookService service;

    @BeforeEach
    void setUp() {
        EzvizProperties properties = new EzvizProperties();
        properties.getWebhook().setEnabled(true);
        properties.getWebhook().setSecret(SECRET);
        properties.getWebhook().setMaxClockSkewSeconds(600);
        alarmService = mock(AlarmService.class);
        service = new EzvizWebhookService(properties, new ObjectMapper(), alarmService);
    }

    @Test
    void validAlarmIsNormalizedPersistedAndAcknowledged() throws Exception {
        String body = """
                {"header":{"messageId":"msg-1","messageTime":1710000000000,"type":"ys.alarm","deviceId":"ABC123","channelNo":1},
                "body":{"data":"{\\"alarmType\\":\\"motiondetect\\",\\"alarmName\\":\\"移动侦测\\",\\"picUrl\\":\\"https://example.com/alarm.jpg\\"}"}}
                """.replace("\n", "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        when(alarmService.receiveEzvizWebhook(any(), eq(body))).thenReturn(true);

        String messageId = service.handle(body, timestamp, sign(body, timestamp), "ys.alarm");

        assertEquals("msg-1", messageId);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(alarmService).receiveEzvizWebhook(captor.capture(), eq(body));
        assertEquals("ABC123", captor.getValue().get("deviceSerial"));
        assertEquals("motiondetect", captor.getValue().get("alarmType"));
        assertEquals("https://example.com/alarm.jpg", captor.getValue().get("alarmPicUrl"));
    }

    @Test
    void invalidSignatureIsRejected() {
        String body = "{\"header\":{\"messageId\":\"msg-2\",\"type\":\"ys.alarm\"}}";
        String timestamp = String.valueOf(System.currentTimeMillis());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.handle(body, timestamp, "bad-signature", "ys.alarm"));

        assertEquals(BusinessCode.UNAUTHORIZED, exception.getBusinessCode());
        verify(alarmService, never()).receiveEzvizWebhook(any(), any());
    }

    @Test
    void staleRequestIsRejected() throws Exception {
        String body = "{\"header\":{\"messageId\":\"msg-3\",\"type\":\"ys.alarm\"}}";
        String timestamp = "1";

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.handle(body, timestamp, sign(body, timestamp), "ys.alarm"));

        assertEquals(BusinessCode.UNAUTHORIZED, exception.getBusinessCode());
        verify(alarmService, never()).receiveEzvizWebhook(any(), any());
    }

    @Test
    void nonAlarmTestMessageIsAcknowledgedWithoutPersistence() throws Exception {
        String body = "{\"header\":{\"messageId\":\"test-1\",\"type\":\"ys.test.msg\"},\"body\":{}}";
        String timestamp = String.valueOf(System.currentTimeMillis());

        assertEquals("test-1", service.handle(body, timestamp, sign(body, timestamp), "ys.test.msg"));
        verify(alarmService, never()).receiveEzvizWebhook(any(), any());
    }

    private String sign(String body, String timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] digest = mac.doFinal((body + timestamp).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) {
            hex.append(String.format("%02x", value & 0xff));
        }
        return hex.toString();
    }
}
