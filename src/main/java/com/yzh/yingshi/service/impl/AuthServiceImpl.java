package com.yzh.yingshi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.common.util.JwtUtil;
import com.yzh.yingshi.dto.AuthLoginRequest;
import com.yzh.yingshi.entity.SysUser;
import com.yzh.yingshi.mapper.SysUserMapper;
import com.yzh.yingshi.service.AuthService;
import com.yzh.yingshi.vo.AuthLoginVO;
import com.yzh.yingshi.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest httpServletRequest;

    @Override
    public AuthLoginVO login(AuthLoginRequest request) {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        qw.eq("username", request.getUsername());
        SysUser user = sysUserMapper.selectOne(qw);
        if (user == null) {
            throw new BusinessException(BusinessCode.UNAUTHORIZED, "用户名或密码错误");
        }

        boolean matches = request.getPassword().equals(user.getPasswordHash());
        if (!matches) {
            throw new BusinessException(BusinessCode.UNAUTHORIZED, "用户名或密码错误");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());

        String token = jwtUtil.generateToken(claims);

        AuthLoginVO vo = new AuthLoginVO();
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setExpiresIn(jwtUtil.getExpireSeconds());
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        return vo;
    }

    @Override
    public UserInfoVO me() {
        String token = httpServletRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                Map<String, Object> claims = jwtUtil.parseToken(token);
                Long userId = Long.valueOf(claims.get("userId").toString());
                SysUser user = sysUserMapper.selectById(userId);
                if (user != null) {
                    UserInfoVO vo = new UserInfoVO();
                    vo.setId(user.getId());
                    vo.setUsername(user.getUsername());
                    vo.setNickname(user.getNickname());
                    vo.setRole(user.getRole());
                    return vo;
                }
            } catch (Exception e) {
                throw new BusinessException(BusinessCode.UNAUTHORIZED, "凭证已过期或无效");
            }
        }
        throw new BusinessException(BusinessCode.UNAUTHORIZED, "用户未登录");
    }

    @Override
    public Void logout() {
        // 第一阶段轻量实现：
        // 由于使用 JWT 无状态 Token，且暂时不引入 Redis 做黑名单，
        // 在此处只要返回成功即可，要求前端主动清除本地存储的 Token。
        return null;
    }
}

