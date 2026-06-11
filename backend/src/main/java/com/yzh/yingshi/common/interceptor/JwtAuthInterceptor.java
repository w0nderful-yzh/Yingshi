package com.yzh.yingshi.common.interceptor;

import com.yzh.yingshi.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 萤石服务器可能直接 GET 回调此地址，只有该请求不依赖登录态。
        // 前端提交授权结果的 POST 请求仍必须经过 JWT 校验。
        if ("GET".equalsIgnoreCase(request.getMethod())
                && "/api/ezviz/oauth/callback".equals(request.getRequestURI())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":40100,\"message\":\"用户未登录\"}");
            return false;
        }

        try {
            Map<String, Object> claims = jwtUtil.parseToken(token.substring(7));
            // JWT反序列化小数字为Integer, 统一转为Long避免下游ClassCastException
            Object userIdObj = claims.get("userId");
            Long userId = userIdObj instanceof Number ? ((Number) userIdObj).longValue() : null;
            request.setAttribute("userId", userId);
            request.setAttribute("username", claims.get("username"));
            request.setAttribute("role", claims.get("role"));
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":40100,\"message\":\"凭证已过期或无效\"}");
            return false;
        }
    }
}
