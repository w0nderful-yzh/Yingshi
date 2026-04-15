package com.yzh.yingshi.vo;

import lombok.Data;

@Data
public class AuthLoginVO {
    private String token;
    private String tokenType;
    private Long expiresIn;
    private Long userId;
    private String username;
    private String role;
}
