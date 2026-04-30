package com.yzh.yingshi.service;

import com.yzh.yingshi.common.api.BusinessCode;
import com.yzh.yingshi.common.exception.BusinessException;
import com.yzh.yingshi.config.EzvizProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EzvizAlarmService {

    private final EzvizTokenService ezvizTokenService;
    private final EzvizProperties ezvizProperties;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Map<String, Object>> listDeviceAlarms(String deviceSerial, Long startTime, Long endTime) {
        String accessToken = ezvizTokenService.getAccessToken();
        return doListDeviceAlarms(accessToken, deviceSerial, startTime, endTime, true);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> doListDeviceAlarms(String accessToken, String deviceSerial,
                                                          Long startTime, Long endTime, boolean allowRetry) {
        String url = ezvizProperties.getBaseUrl() + "/api/lapp/alarm/device/list";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("accessToken", accessToken);
        params.add("deviceSerial", deviceSerial);
        params.add("startTime", String.valueOf(startTime));
        params.add("endTime", String.valueOf(endTime));
        params.add("status", "2");
        params.add("pageStart", "0");
        params.add("pageSize", "50");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            Map<String, Object> result = restTemplate.postForObject(url, request, Map.class);

            if (result == null) {
                throw new BusinessException(BusinessCode.INTERNAL_ERROR, "萤石告警接口无响应");
            }

            String code = String.valueOf(result.get("code"));

            if ("10002".equals(code) && allowRetry) {
                log.warn("萤石token过期, code=10002, 刷新重试");
                ezvizTokenService.clearToken();
                String newToken = ezvizTokenService.refreshToken();
                return doListDeviceAlarms(newToken, deviceSerial, startTime, endTime, false);
            }

            if (!"200".equals(code)) {
                String msg = result.containsKey("msg") ? String.valueOf(result.get("msg")) : "unknown error";
                log.error("萤石告警接口调用失败, code={}, msg={}", code, msg);
                throw new BusinessException(BusinessCode.INTERNAL_ERROR, "萤石告警接口调用失败: " + msg);
            }

            Object data = result.get("data");
            if (data == null) {
                return Collections.emptyList();
            }

            if (data instanceof List) {
                return (List<Map<String, Object>>) data;
            }

            return Collections.emptyList();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用萤石告警接口异常", e);
            throw new BusinessException(BusinessCode.INTERNAL_ERROR, "调用萤石告警接口异常");
        }
    }
}
