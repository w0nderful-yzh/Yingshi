package com.yzh.yingshi.service;

import com.yzh.yingshi.common.api.PageResult;
import com.yzh.yingshi.dto.TaskCreateRequest;
import com.yzh.yingshi.vo.TaskStatusVO;
import com.yzh.yingshi.vo.TaskVO;

public interface TaskService {
    TaskStatusVO create(TaskCreateRequest request);

    PageResult<TaskVO> list(Long deviceId, String status, Integer page, Integer size);

    TaskVO detail(Long taskId);

    TaskStatusVO start(Long taskId);

    TaskStatusVO cancel(Long taskId);
}

