package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.InternalEventReportRequest;
import com.yzh.yingshi.vo.BehaviorEventDetailVO;
import com.yzh.yingshi.vo.BehaviorEventVO;
import com.yzh.yingshi.vo.EventIdVO;

import java.util.List;

public interface EventService {
    List<BehaviorEventVO> listTaskEvents(Long taskId);

    BehaviorEventDetailVO detail(Long eventId);

    Boolean ack(Long eventId);

    EventIdVO internalReport(Long taskId, InternalEventReportRequest request);
}

