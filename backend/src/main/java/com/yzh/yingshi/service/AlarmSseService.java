package com.yzh.yingshi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.entity.AlarmMessage;
import com.yzh.yingshi.entity.UserDevice;
import com.yzh.yingshi.mapper.UserDeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警SSE推送服务
 * 维护每个用户的SSE连接，新告警时主动推送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmSseService {

    private final ObjectMapper objectMapper;
    private final UserDeviceMapper userDeviceMapper;

    /** userId -> SseEmitter */
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30分钟

    /**
     * 注册用户的SSE连接
     */
    public SseEmitter subscribe(Long userId) {
        // 移除旧连接
        SseEmitter old = emitters.remove(userId);
        if (old != null) {
            old.complete();
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.debug("SSE连接完成 userId={}", userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.debug("SSE连接超时 userId={}", userId);
        });
        emitter.onError(e -> {
            emitters.remove(userId);
            log.debug("SSE连接异常 userId={}: {}", userId, e.getMessage());
        });

        emitters.put(userId, emitter);

        // 发送初始连接成功事件
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"ok\"}"));
        } catch (IOException e) {
            log.debug("SSE发送初始事件失败 userId={}: {}", userId, e.getMessage());
        }

        log.info("用户订阅告警SSE, userId={}, 当前连接数={}", userId, emitters.size());
        return emitter;
    }

    /**
     * 向指定用户推送新告警
     */
    public void pushAlarm(Long userId, Object alarmData) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(alarmData);
            emitter.send(SseEmitter.event()
                    .name("alarm")
                    .data(json));
            log.debug("SSE推送告警成功 userId={}", userId);
        } catch (IOException e) {
            log.debug("SSE推送告警失败 userId={}: {}", userId, e.getMessage());
            emitters.remove(userId);
        }
    }

    public void broadcastAlarm(AlarmMessage alarm) {
        if (alarm == null || alarm.getDeviceSerial() == null) {
            return;
        }
        List<UserDevice> bindings = userDeviceMapper.selectList(
                new LambdaQueryWrapper<UserDevice>()
                        .eq(UserDevice::getDeviceSerial, alarm.getDeviceSerial())
                        .eq(UserDevice::getStatus, 1)
        );
        bindings.stream()
                .map(UserDevice::getUserId)
                .distinct()
                .forEach(userId -> pushAlarm(userId, alarm));
    }

    /**
     * 获取当前连接数
     */
    public int getConnectedCount() {
        return emitters.size();
    }
}
