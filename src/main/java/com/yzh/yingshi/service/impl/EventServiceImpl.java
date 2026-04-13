package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.dto.InternalEventReportRequest;
import com.yzh.yingshi.service.EventService;
import com.yzh.yingshi.vo.BehaviorEventDetailVO;
import com.yzh.yingshi.vo.BehaviorEventVO;
import com.yzh.yingshi.vo.EventIdVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventServiceImpl implements EventService {
    @Override
    public List<BehaviorEventVO> listTaskEvents(Long taskId) {
        return List.of();
    }

    @Override
    public BehaviorEventDetailVO detail(Long eventId) {
        return new BehaviorEventDetailVO();
    }

    @Override
    public Boolean ack(Long eventId) {
        return Boolean.TRUE;
    }

    @Override
    public EventIdVO internalReport(Long taskId, InternalEventReportRequest request) {
        return new EventIdVO();
    }
}

