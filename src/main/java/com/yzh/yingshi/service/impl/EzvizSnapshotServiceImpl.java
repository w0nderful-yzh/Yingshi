package com.yzh.yingshi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzh.yingshi.config.EzvizProperties;
import com.yzh.yingshi.service.EzvizSnapshotService;
import com.yzh.yingshi.service.EzvizTokenService;
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
public class EzvizSnapshotServiceImpl implements EzvizSnapshotService {

    private final EzvizProperties ezvizProperties;
    private final EzvizTokenService ezvizTokenService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String captureSnapshot(String deviceSerial, Integer channelNo) {
        String token = ezvizTokenService.getAccessToken();
        String url = doCapture(token, deviceSerial, channelNo);

        if (url == null) {
            log.warn("首次截图失败, 尝试刷新token后重试 deviceSerial={}", deviceSerial);
            token = ezvizTokenService.refreshToken();
            url = doCapture(token, deviceSerial, channelNo);
        }

        if (url == null) {
            log.error("截图失败 deviceSerial={}, channelNo={}", deviceSerial, channelNo);
        }
        return url;
    }

    @Override
    public byte[] captureSnapshotBytes(String deviceSerial, Integer channelNo) {
        String picUrl = captureSnapshot(deviceSerial, channelNo);
        if (picUrl == null || picUrl.isBlank()) {
            return null;
        }
        try {
            return restTemplate.getForObject(picUrl, byte[].class);
        } catch (Exception e) {
            log.error("下载截图失败 deviceSerial={}, picUrl={}", deviceSerial, picUrl, e);
            return null;
        }
    }

    private String doCapture(String accessToken, String deviceSerial, Integer channelNo) {
        String url = ezvizProperties.getBaseUrl() + "/api/lapp/device/capture";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("accessToken", accessToken);
        params.add("deviceSerial", deviceSerial);
        params.add("channelNo", String.valueOf(channelNo != null ? channelNo : 1));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            String response = restTemplate.postForObject(url, request, String.class);
            JsonNode root = objectMapper.readTree(response);

            String code = root.has("code") ? root.get("code").asText() : "";
            if ("10002".equals(code)) {
                log.warn("萤石token过期, code=10002");
                ezvizTokenService.clearToken();
                return null;
            }
            if (!"200".equals(code)) {
                String msg = root.has("msg") ? root.get("msg").asText() : "unknown";
                log.warn("萤石截图接口返回非200, code={}, msg={}", code, msg);
                return null;
            }

            JsonNode data = root.get("data");
            if (data != null && data.has("picUrl")) {
                return data.get("picUrl").asText();
            }
            return null;
        } catch (Exception e) {
            log.error("调用萤石截图接口异常 deviceSerial={}", deviceSerial, e);
            return null;
        }
    }
}
