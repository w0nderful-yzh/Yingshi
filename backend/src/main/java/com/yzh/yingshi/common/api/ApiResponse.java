package com.yzh.yingshi.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;
    private String requestId;
    private String timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(BusinessCode.SUCCESS.getCode())
                .message("success")
                .data(data)
                .requestId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> fail(BusinessCode businessCode, String message) {
        return ApiResponse.<T>builder()
                .code(businessCode.getCode())
                .message(message)
                .requestId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now().toString())
                .build();
    }
}
