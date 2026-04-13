package com.yzh.yingshi.service;

import com.yzh.yingshi.common.api.PageResult;
import com.yzh.yingshi.dto.InternalFrameBatchReportRequest;
import com.yzh.yingshi.vo.FrameResultVO;
import com.yzh.yingshi.vo.SavedCountVO;

public interface FrameService {
    PageResult<FrameResultVO> listTaskFrames(Long taskId, Integer page, Integer size);

    SavedCountVO batchReport(Long taskId, InternalFrameBatchReportRequest request);
}

