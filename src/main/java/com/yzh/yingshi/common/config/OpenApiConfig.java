package com.yzh.yingshi.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI yingshiOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Yingshi API")
                .description("宠物异常行为检测系统第一阶段接口")
                .version("v1.0"));
    }
}
