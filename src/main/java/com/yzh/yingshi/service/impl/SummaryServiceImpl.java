package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.service.SummaryService;
import com.yzh.yingshi.vo.TaskSummaryVO;
import org.springframework.stereotype.Service;

@Service
public class SummaryServiceImpl implements SummaryService {
    @Override
    public TaskSummaryVO taskSummary(Long taskId) {
        return new TaskSummaryVO();
    }
}

