package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.AuthLoginRequest;
import com.yzh.yingshi.vo.AuthLoginVO;
import com.yzh.yingshi.vo.UserInfoVO;

public interface AuthService {
    AuthLoginVO login(AuthLoginRequest request);

    UserInfoVO me();

    Void logout();
}

