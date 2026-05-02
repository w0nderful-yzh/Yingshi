package com.yzh.yingshi.service;

/**
 * 宠物异常行为分析服务
 * 定时检测三种异常情况并触发告警
 */
public interface PetAbnormalAnalysisService {

    /**
     * 分析所有已启用配置的异常行为
     */
    void analyzeAll();

    /**
     * 手动触发指定配置的异常分析
     */
    void analyzeConfig(Long configId);
}
