package com.yzh.yingshi.controller;

import com.yzh.yingshi.common.api.ApiResponse;
import com.yzh.yingshi.common.auth.CurrentUserService;
import com.yzh.yingshi.config.EzvizProperties;
import com.yzh.yingshi.dto.EzvizOAuthCallbackDTO;
import com.yzh.yingshi.service.EzvizOAuthService;
import com.yzh.yingshi.vo.EzvizAuthUrlVO;
import com.yzh.yingshi.vo.UserDeviceVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ezviz/oauth")
@RequiredArgsConstructor
public class EzvizOAuthController {

    private final EzvizOAuthService ezvizOAuthService;
    private final EzvizProperties ezvizProperties;
    private final HttpServletRequest request;
    private final CurrentUserService currentUserService;

    @GetMapping("/auth-url")
    public ApiResponse<EzvizAuthUrlVO> getAuthUrl() {
        currentUserService.requireWriteAccess();
        Long userId = getCurrentUserId();
        return ApiResponse.success(ezvizOAuthService.generateAuthUrl(userId));
    }

    @PostMapping("/callback")
    public ApiResponse<List<UserDeviceVO>> handleCallback(@RequestBody EzvizOAuthCallbackDTO dto) {
        currentUserService.requireWriteAccess();
        Long userId = getCurrentUserId();
        return ApiResponse.success(ezvizOAuthService.handleCallback(userId, dto));
    }

    /**
     * 萤石 OAuth 后端直接回调（Ezviz redirect 到此端点）
     * 处理完 token 换取和设备绑定后，302 回前端
     */
    @GetMapping("/callback")
    public void handleOAuthCallback(
            @RequestParam(required = false) String authCode,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String deviceSerials,
            @RequestParam(required = false) String deviceTrustId,
            HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/json;charset=UTF-8");

        // 萤石验证回调地址时会无参数GET
        if (authCode == null || state == null) {
            response.getWriter().write("{\"code\":\"200\"}");
            return;
        }

        try {
            Long userId = ezvizOAuthService.parseUserIdFromState(state);

            EzvizOAuthCallbackDTO dto = new EzvizOAuthCallbackDTO();
            dto.setAuthCode(authCode);
            dto.setState(state);
            dto.setDeviceSerials(deviceSerials);
            dto.setDeviceTrustId(deviceTrustId);

            ezvizOAuthService.handleCallback(userId, dto);

            log.info("萤石OAuth后端回调成功, userId={}", userId);
            response.getWriter().write("{\"code\":\"200\"}");
        } catch (Exception e) {
            log.error("萤石OAuth后端回调失败", e);
            response.getWriter().write("{\"code\":\"500\",\"msg\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/devices")
    public ApiResponse<List<UserDeviceVO>> listUserDevices() {
        Long userId = getCurrentUserId();
        return ApiResponse.success(ezvizOAuthService.listUserDevices(userId));
    }

    @DeleteMapping("/devices/{id}")
    public ApiResponse<Void> unbindDevice(@PathVariable Long id) {
        currentUserService.requireWriteAccess();
        Long userId = getCurrentUserId();
        ezvizOAuthService.unbindDevice(userId, id);
        return ApiResponse.success(null);
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        Long userId = getCurrentUserId();
        boolean hasAccount = ezvizOAuthService.hasOAuthAccount(userId);
        Map<String, Object> status = new HashMap<>();
        status.put("authorized", hasAccount);
        return ApiResponse.success(status);
    }

    @DeleteMapping("/revoke")
    public ApiResponse<Void> revokeOAuth() {
        currentUserService.requireWriteAccess();
        Long userId = getCurrentUserId();
        ezvizOAuthService.revokeOAuth(userId);
        return ApiResponse.success(null);
    }

    private Long getCurrentUserId() {
        return (Long) request.getAttribute("userId");
    }
}
