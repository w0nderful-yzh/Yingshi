package com.yzh.yingshi.service.impl;

import com.yzh.yingshi.dto.AuthLoginRequest;
import com.yzh.yingshi.service.AuthService;
import com.yzh.yingshi.vo.AuthLoginVO;
import com.yzh.yingshi.vo.UserInfoVO;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    @Override
    public AuthLoginVO login(AuthLoginRequest request) {
        return new AuthLoginVO();
    }

    @Override
    public UserInfoVO me() {
        return new UserInfoVO();
    }

    @Override
    public Void logout() {
        return null;
    }
}

