package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.AlarmQueryDTO;
import com.yzh.yingshi.dto.AlarmSyncResultDTO;
import com.yzh.yingshi.vo.AlarmMessageVO;

import java.util.List;
import java.util.Map;

public interface AlarmService {

    AlarmSyncResultDTO syncFromEzviz();

    boolean receiveEzvizWebhook(Map<String, Object> alarm, String rawJson);

    List<AlarmMessageVO> listAlarms(AlarmQueryDTO dto);

    AlarmMessageVO getAlarmDetail(Long id);

    long countUnread();

    void markRead(Long id);

    void markAllRead(Long deviceId);

    void deleteAlarm(Long id);
}
