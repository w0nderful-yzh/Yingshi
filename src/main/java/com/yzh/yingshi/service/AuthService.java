package com.yzh.yingshi.service;

import com.yzh.yingshi.dto.AuthLoginRequest;
import com.yzh.yingshi.dto.AuthRegisterRequest;
import com.yzh.yingshi.vo.AuthLoginVO;
import com.yzh.yingshi.vo.UserInfoVO;

public interface AuthService {
    AuthLoginVO login(AuthLoginRequest request);

    AuthLoginVO register(AuthRegisterRequest request);

    UserInfoVO me();

    void logout();
}
