package com.yzh.yingshi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@MapperScan("com.yzh.yingshi.mapper")
@EnableScheduling
@EnableAsync
public class YingshiApplication {

    private static final String DEFAULT_TIME_ZONE = "Asia/Shanghai";

    public static void main(String[] args) {
        configureTimeZone();
        SpringApplication.run(YingshiApplication.class, args);
    }

    static void configureTimeZone() {
        String configuredTimeZone = System.getenv().getOrDefault("APP_TIME_ZONE", DEFAULT_TIME_ZONE);
        try {
            ZoneId zoneId = ZoneId.of(configuredTimeZone);
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
            System.setProperty("user.timezone", zoneId.getId());
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Invalid APP_TIME_ZONE: " + configuredTimeZone, exception);
        }
    }

}
