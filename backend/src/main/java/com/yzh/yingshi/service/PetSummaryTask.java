package com.yzh.yingshi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 宠物活动总结定时任务
 * 每天 23:00 生成日报，每周一 23:30 生成周报
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PetSummaryTask {

    private final PetSummaryService petSummaryService;

    /**
     * 每天 23:00 生成日报
     */
    @Scheduled(cron = "0 0 23 * * ?")
    public void generateDailySummaries() {
        log.info("开始生成每日活动报告...");
        try {
            petSummaryService.generateDailySummariesForAll();
            log.info("每日活动报告生成完成");
        } catch (Exception e) {
            log.error("每日活动报告生成失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 每周一 23:30 生成周报
     */
    @Scheduled(cron = "0 30 23 ? * MON")
    public void generateWeeklySummaries() {
        log.info("开始生成每周活动报告...");
        try {
            petSummaryService.generateWeeklySummariesForAll();
            log.info("每周活动报告生成完成");
        } catch (Exception e) {
            log.error("每周活动报告生成失败: {}", e.getMessage(), e);
        }
    }
}
