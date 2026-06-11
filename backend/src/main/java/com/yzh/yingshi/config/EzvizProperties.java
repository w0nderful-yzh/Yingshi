package com.yzh.yingshi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ezviz")
public class EzvizProperties {

    private String appKey;

    private String appSecret;

    private String baseUrl;

    /**
     * 设备视频加密验证码。单设备部署可通过 EZVIZ_DEVICE_CODE 配置。
     */
    private String deviceCode;

    private OAuth oauth = new OAuth();

    @Data
    public static class OAuth {
        private String redirectUri;
        private String frontendUrl;
    }
}
