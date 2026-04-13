package com.yzh.yingshi.vo;

import lombok.Data;

@Data
public class AuthLoginVO {
    private String token;
    private Long expiresIn;
    private UserInfoVO user;
}
