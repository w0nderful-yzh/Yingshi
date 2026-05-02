package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.PetDetectionConfigRequest;
import com.yzh.yingshi.dto.PetDetectionRecordQueryDTO;
import com.yzh.yingshi.dto.PetSafeZoneRequest;
import com.yzh.yingshi.vo.PetDetectionConfigVO;
import com.yzh.yingshi.vo.PetDetectionRecordVO;
import com.yzh.yingshi.vo.PetDetectionResultVO;
import com.yzh.yingshi.vo.PetSafeZoneVO;

import java.util.List;

public interface PetDetectionService {

    // ---- 检测配置 ----

    PetDetectionConfigVO createConfig(PetDetectionConfigRequest request);

    PetDetectionConfigVO updateConfig(Long id, PetDetectionConfigRequest request);

    void deleteConfig(Long id);

    PetDetectionConfigVO getConfigById(Long id);

    List<PetDetectionConfigVO> listConfigs();

    // ---- 安全区域 ----

    PetSafeZoneVO createSafeZone(PetSafeZoneRequest request);

    PetSafeZoneVO updateSafeZone(Long id, PetSafeZoneRequest request);

    void deleteSafeZone(Long id);

    List<PetSafeZoneVO> listSafeZones(Long detectionConfigId);

    // ---- 检测记录 ----

    List<PetDetectionRecordVO> listRecords(PetDetectionRecordQueryDTO dto);

    // ---- 手动触发检测 ----

    PetDetectionResultVO triggerDetection(Long configId);
}
