package com.yzh.yingshi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pet.detection")
public class PetDetectionProperties {

    /** 检测间隔(毫秒), 默认30000 (30秒) */
    private long intervalMs = 30000L;

    /** 帧差像素阈值 (0-255), 默认25. 越小越敏感 */
    private int pixelThreshold = 25;

    /** 运动检测网格大小(像素), 默认20 */
    private int gridSize = 20;

    /** 最小运动区域面积(像素), 低于此值忽略, 默认400 */
    private int minAreaPixels = 400;

    /** 默认冷却时间(秒), 默认300 (5分钟) */
    private int defaultCooldownSeconds = 300;
}
