package com.yzh.yingshi.service;

import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.config.EzvizProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EzvizTokenService {

    private final EzvizProperties ezvizProperties;
    private final ObjectMapper objectMapper;

    private String cachedToken;
    private long expireTime;

    public String getAccessToken() {
        if (cachedToken != null && System.currentTimeMillis() < expireTime - 5 * 60 * 1000) {
            return cachedToken;
        }
        return refreshToken();
    }

    public String refreshToken() {
        String url = ezvizProperties.getBaseUrl() + "/api/lapp/token/get";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("appKey", ezvizProperties.getAppKey());
        params.add("appSecret", ezvizProperties.getAppSecret());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.postForObject(url, request, String.class);
            JsonNode root = objectMapper.readTree(response);

            String code = root.get("code").asText();
            if (!"200".equals(code)) {
                String msg = root.has("msg") ? root.get("msg").asText() : "unknown error";
                log.error("获取萤石token失败, code={}, msg={}", code, msg);
                clearToken();
                throw new BusinessException(BusinessCode.INTERNAL_ERROR, "获取萤石accessToken失败: " + msg);
            }

            JsonNode data = root.get("data");
            this.cachedToken = data.get("accessToken").asText();
            this.expireTime = data.get("expireTime").asLong();

            log.info("获取萤石token成功, 过期时间: {}", expireTime);
            return cachedToken;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用萤石token接口异常", e);
            clearToken();
            throw new BusinessException(BusinessCode.INTERNAL_ERROR, "调用萤石token接口失败");
        }
    }

    public void clearToken() {
        this.cachedToken = null;
        this.expireTime = 0;
    }
}
