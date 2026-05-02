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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EzvizDeviceService {

    private final EzvizProperties ezvizProperties;
    private final EzvizTokenService ezvizTokenService;
    private final ObjectMapper objectMapper;

    public List<JsonNode> listEzvizDevices() {
        String token = ezvizTokenService.getAccessToken();
        List<JsonNode> devices = fetchDeviceList(token);

        if (devices == null) {
            log.warn("首次获取设备列表失败，尝试刷新token后重试");
            token = ezvizTokenService.refreshToken();
            devices = fetchDeviceList(token);
            if (devices == null) {
                throw new BusinessException(BusinessCode.INTERNAL_ERROR, "获取萤石设备列表失败");
            }
        }

        return devices;
    }

    private List<JsonNode> fetchDeviceList(String accessToken) {
        String url = ezvizProperties.getBaseUrl() + "/api/lapp/device/list";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("accessToken", accessToken);
        params.add("pageStart", "0");
        params.add("pageSize", "50");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.postForObject(url, request, String.class);
            JsonNode root = objectMapper.readTree(response);

            String code = root.get("code").asText();

            if ("10002".equals(code)) {
                log.warn("萤石token过期或异常, code=10002");
                ezvizTokenService.clearToken();
                return null;
            }

            if (!"200".equals(code)) {
                String msg = root.has("msg") ? root.get("msg").asText() : "unknown error";
                log.error("获取萤石设备列表失败, code={}, msg={}", code, msg);
                return null;
            }

            JsonNode data = root.get("data");
            List<JsonNode> result = new ArrayList<>();
            if (data != null && data.isArray()) {
                for (JsonNode device : data) {
                    result.add(device);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("调用萤石设备列表接口异常", e);
            return null;
        }
    }
}
