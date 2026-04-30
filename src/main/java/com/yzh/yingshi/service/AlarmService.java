package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.AlarmQueryDTO;
import com.yzh.yingshi.dto.AlarmSyncResultDTO;
import com.yzh.yingshi.vo.AlarmMessageVO;

import java.util.List;

public interface AlarmService {

    AlarmSyncResultDTO syncFromEzviz();

    List<AlarmMessageVO> listAlarms(AlarmQueryDTO dto);

    long countUnread();

    void markRead(Long id);

    void markAllRead(Long deviceId);

    void deleteAlarm(Long id);
}
