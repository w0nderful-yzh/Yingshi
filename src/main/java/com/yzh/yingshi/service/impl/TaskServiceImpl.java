package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.common.api.PageResult;
import com.yzh.yingshi.dto.TaskCreateRequest;
import com.yzh.yingshi.service.TaskService;
import com.yzh.yingshi.vo.TaskStatusVO;
import com.yzh.yingshi.vo.TaskVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {
    @Override
    public TaskStatusVO create(TaskCreateRequest request) {
        return new TaskStatusVO();
    }

    @Override
    public PageResult<TaskVO> list(Long deviceId, String status, Integer page, Integer size) {
        return new PageResult<>(0L, List.of());
    }

    @Override
    public TaskVO detail(Long taskId) {
        return new TaskVO();
    }

    @Override
    public TaskStatusVO start(Long taskId) {
        return new TaskStatusVO();
    }

    @Override
    public TaskStatusVO cancel(Long taskId) {
        return new TaskStatusVO();
    }
}

