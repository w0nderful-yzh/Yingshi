package com.yzh.yingshi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.config.EzvizProperties;
import com.yzh.yingshi.entity.UserEzvizAccount;
import com.yzh.yingshi.mapper.UserEzvizAccountMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class EzvizTokenResolver {

    private final EzvizTokenService ezvizTokenService;
    private final UserEzvizAccountMapper userEzvizAccountMapper;
    private final EzvizProperties ezvizProperties;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    /**
     * 从请求上下文自动解析 token：有用户 token 就用，否则回退 app-level
     */
    public String resolve() {
        Long userId = getCurrentUserId();
        if (userId != null) {
            try {
                String userToken = getUserAccessToken(userId);
                if (userToken != null) {
                    return userToken;
                }
            } catch (Exception e) {
                log.warn("获取用户萤石token失败, 回退到应用级token, userId={}: {}", userId, e.getMessage());
            }
        }
        return ezvizTokenService.getAccessToken();
    }

    /**
     * 显式按 userId 解析，用于后台任务
     */
    public String resolveForUser(Long userId) {
        if (userId != null) {
            try {
                String userToken = getUserAccessToken(userId);
                if (userToken != null) {
                    return userToken;
                }
            } catch (Exception e) {
                log.warn("获取用户萤石token失败, userId={}: {}", userId, e.getMessage());
            }
        }
        return ezvizTokenService.getAccessToken();
    }

    /**
     * 强制 app-level token，用于管理员操作（设备同步等）
     */
    public String resolveAppLevel() {
        return ezvizTokenService.getAccessToken();
    }

    /**
     * 强制刷新 token 后返回，用于 10002 重试
     */
    public String resolveWithRefresh() {
        Long userId = getCurrentUserId();
        if (userId != null) {
            try {
                String userToken = refreshToken(userId);
                if (userToken != null) {
                    return userToken;
                }
            } catch (Exception e) {
                log.warn("刷新用户萤石token失败, 回退到应用级, userId={}: {}", userId, e.getMessage());
            }
        }
        return ezvizTokenService.refreshToken();
    }

    /**
     * 获取用户的有效 accessToken，过期则自动刷新
     */
    private String getUserAccessToken(Long userId) {
        UserEzvizAccount account = userEzvizAccountMapper.selectOne(
                new LambdaQueryWrapper<UserEzvizAccount>()
                        .eq(UserEzvizAccount::getUserId, userId)
                        .eq(UserEzvizAccount::getStatus, 1));
        if (account == null || account.getAccessToken() == null) {
            return null;
        }
        // 5 分钟缓冲，与 EzvizTokenService 一致
        if (account.getExpireTime() != null
                && System.currentTimeMillis() < account.getExpireTime() - 5 * 60 * 1000) {
            return account.getAccessToken();
        }
        // 过期，尝试刷新
        return refreshToken(userId);
    }

    /**
     * 刷新用户的萤石 token
     */
    private String refreshToken(Long userId) {
        UserEzvizAccount account = userEzvizAccountMapper.selectOne(
                new LambdaQueryWrapper<UserEzvizAccount>()
                        .eq(UserEzvizAccount::getUserId, userId)
                        .eq(UserEzvizAccount::getStatus, 1));
        if (account == null || account.getAccessToken() == null) {
            return null;
        }

        String url = ezvizProperties.getBaseUrl() + "/api/lapp/token/v2/refresh";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("appKey", ezvizProperties.getAppKey());
        params.add("appSecret", ezvizProperties.getAppSecret());
        params.add("accessToken", account.getAccessToken());

        HttpEntity<MultiValueMap<String, String>> httpRequest = new HttpEntity<>(params, headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.postForObject(url, httpRequest, String.class);
            JsonNode root = objectMapper.readTree(response);

            String code = root.get("code").asText();
            if (!"200".equals(code)) {
                log.warn("刷新用户萤石token失败, userId={}, code={}, msg={}",
                        userId, code, root.has("msg") ? root.get("msg").asText() : "");
                // 刷新失败，标记为撤销
                account.setStatus(0);
                userEzvizAccountMapper.updateById(account);
                return null;
            }

            JsonNode data = root.get("data");
            account.setAccessToken(data.get("accessToken").asText());
            account.setRefreshToken(data.get("refreshToken").asText());
            account.setExpireTime(data.get("expireTime").asLong());
            userEzvizAccountMapper.updateById(account);

            log.info("刷新用户萤石token成功, userId={}", userId);
            return account.getAccessToken();
        } catch (Exception e) {
            log.error("调用萤石token刷新接口异常, userId={}", userId, e);
            account.setStatus(0);
            userEzvizAccountMapper.updateById(account);
            return null;
        }
    }

    private Long getCurrentUserId() {
        try {
            Object attr = request.getAttribute("userId");
            if (attr instanceof Long) {
                return (Long) attr;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
