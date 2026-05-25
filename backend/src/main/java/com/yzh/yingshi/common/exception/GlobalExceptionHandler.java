package com.yzh.yingshi.common.exception;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.common.api.BusinessCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        log.warn("业务异常: code={}, message={}", exception.getBusinessCode(), exception.getMessage());
        return ApiResponse.fail(exception.getBusinessCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        return ApiResponse.fail(BusinessCode.PARAM_INVALID, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException exception) {
        log.warn("参数校验失败: {}", exception.getMessage());
        return ApiResponse.fail(BusinessCode.PARAM_INVALID, "参数校验失败");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return ApiResponse.fail(BusinessCode.INTERNAL_ERROR, "系统内部异常，请稍后重试");
    }
}
