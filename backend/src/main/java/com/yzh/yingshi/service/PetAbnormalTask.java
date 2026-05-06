package com.yzh.yingshi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetAbnormalTask {

    private final PetAbnormalAnalysisService petAbnormalAnalysisService;

    /**
     * 每60秒分析一次异常行为
     */
    @Scheduled(fixedDelay = 60000)
    public void analyzeAbnormalBehavior() {
        try {
            petAbnormalAnalysisService.analyzeAll();
        } catch (Exception e) {
            log.warn("异常行为分析任务异常: {}", e.getMessage());
        }
    }
}
