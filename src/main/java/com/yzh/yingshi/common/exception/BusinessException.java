package com.yzh.yingshi.common.exception;

import com.yzh.yingshi.common.api.BusinessCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final BusinessCode businessCode;

    public BusinessException(BusinessCode businessCode, String message) {
        super(message);
        this.businessCode = businessCode;
    }
}
