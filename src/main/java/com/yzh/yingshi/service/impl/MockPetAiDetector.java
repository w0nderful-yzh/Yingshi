package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.service.PetAiDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock宠物AI检测器
 * 用于MVP阶段测试, 模拟AI检测结果
 * 后续可替换为YOLOv8等真实AI模型实现
 */
@Slf4j
@Component
public class MockPetAiDetector implements PetAiDetector {

    private final Random random = new Random();

    @Override
    public List<PetDetection> detect(String imageUrl) {
        log.info("Mock AI检测 imageUrl={}", imageUrl);

        List<PetDetection> results = new ArrayList<>();

        if (imageUrl == null || imageUrl.isBlank()) {
            log.warn("图片URL为空, 跳过检测");
            return results;
        }

        // 模拟检测到一只宠物, 位置随机
        // 约50%概率宠物在安全区域外, 用于测试告警逻辑
        double x = 10 + random.nextDouble() * 70;  // 10-80
        double y = 10 + random.nextDouble() * 70;  // 10-80
        double w = 5 + random.nextDouble() * 15;   // 5-20
        double h = 5 + random.nextDouble() * 15;   // 5-20

        PetDetection detection = new PetDetection("pet", 0.85 + random.nextDouble() * 0.1, x, y, w, h);
        results.add(detection);

        log.info("Mock AI检测结果: x={}, y={}, w={}, h={}, confidence={}",
                String.format("%.1f", detection.getX()),
                String.format("%.1f", detection.getY()),
                String.format("%.1f", detection.getWidth()),
                String.format("%.1f", detection.getHeight()),
                String.format("%.2f", detection.getConfidence()));

        return results;
    }
}
