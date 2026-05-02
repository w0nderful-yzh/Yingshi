package com.yzh.yingshi.service;

import com.yzh.yingshi.config.PetDetectionProperties;
import com.yzh.yingshi.service.impl.PetDetectionServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetDetectTask {

    private final PetDetectionServiceImpl petDetectionService;
    private final PetDetectionProperties detectionProperties;

    @Scheduled(fixedDelayString = "${pet.detection.interval-ms:30000}")
    public void runPetDetection() {
        try {
            petDetectionService.runAllDetections();
        } catch (Exception e) {
            log.warn("定时宠物检测任务异常: {}", e.getMessage());
        }
    }
}
