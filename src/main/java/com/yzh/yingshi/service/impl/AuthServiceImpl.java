package com.yzh.yingshi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.common.util.JwtUtil;
import com.yzh.yingshi.dto.AuthLoginRequest;
import com.yzh.yingshi.dto.AuthRegisterRequest;
import com.yzh.yingshi.entity.SysUser;
import com.yzh.yingshi.mapper.SysUserMapper;
import com.yzh.yingshi.service.AuthService;
import com.yzh.yingshi.vo.AuthLoginVO;
import com.yzh.yingshi.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest httpServletRequest;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public AuthLoginVO register(AuthRegisterRequest request) {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        qw.eq("username", request.getUsername());
        if (sysUserMapper.selectOne(qw) != null) {
            throw new BusinessException(BusinessCode.STATUS_CONFLICT, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);

        return buildLoginVO(user);
    }

    @Override
    public AuthLoginVO login(AuthLoginRequest request) {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        qw.eq("username", request.getUsername());
        SysUser user = sysUserMapper.selectOne(qw);
        if (user == null) {
            throw new BusinessException(BusinessCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(BusinessCode.FORBIDDEN, "账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(BusinessCode.UNAUTHORIZED, "用户名或密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateById(user);

        return buildLoginVO(user);
    }

    @Override
    public UserInfoVO me() {
        Long userId = (Long) httpServletRequest.getAttribute("userId");
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BusinessCode.UNAUTHORIZED, "用户不存在");
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }

    @Override
    public void logout() {
        // 无状态 JWT，客户端清除 token 即可
    }

    private AuthLoginVO buildLoginVO(SysUser user) {
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

}
