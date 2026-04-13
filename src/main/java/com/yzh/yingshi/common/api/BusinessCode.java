package com.yzh.yingshi.common.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessCode {
    SUCCESS(0),
    PARAM_INVALID(40001),
    RESOURCE_NOT_FOUND(40004),
    STATUS_CONFLICT(40009),
    UNAUTHORIZED(40100),
    FORBIDDEN(40300),
    INTERNAL_ERROR(50000),
    MODEL_SERVICE_ERROR(50010);

    private final int code;
}
