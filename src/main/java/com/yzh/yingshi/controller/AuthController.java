package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.dto.AuthLoginRequest;
import com.yzh.yingshi.dto.AuthRegisterRequest;
import com.yzh.yingshi.service.AuthService;
import com.yzh.yingshi.vo.AuthLoginVO;
import com.yzh.yingshi.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthLoginVO> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthLoginVO> login(@Valid @RequestBody AuthLoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoVO> me() {
        return ApiResponse.success(authService.me());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success(null);
    }
}
