package com.yzh.yingshi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmSyncTask {

    private final AlarmService alarmService;

    @Scheduled(fixedDelay = 60000)
    public void syncAlarmMessages() {
        try {
            alarmService.syncFromEzviz();
        } catch (Exception e) {
            log.warn("定时同步萤石告警失败: {}", e.getMessage());
        }
    }
}
