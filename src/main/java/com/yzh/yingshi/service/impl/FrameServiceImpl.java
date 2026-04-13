package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.common.api.PageResult;
import com.yzh.yingshi.dto.InternalFrameBatchReportRequest;
import com.yzh.yingshi.service.FrameService;
import com.yzh.yingshi.vo.FrameResultVO;
import com.yzh.yingshi.vo.SavedCountVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FrameServiceImpl implements FrameService {
    @Override
    public PageResult<FrameResultVO> listTaskFrames(Long taskId, Integer page, Integer size) {
        return new PageResult<>(0L, List.of());
    }

    @Override
    public SavedCountVO batchReport(Long taskId, InternalFrameBatchReportRequest request) {
        SavedCountVO vo = new SavedCountVO();
        vo.setSavedCount(request.getRecords().size());
        return vo;
    }
}

