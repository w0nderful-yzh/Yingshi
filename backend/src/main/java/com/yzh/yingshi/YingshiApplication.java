package com.yzh.yingshi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.yzh.yingshi.mapper")
@EnableScheduling
@EnableAsync
public class YingshiApplication {

    public static void main(String[] args) {
        SpringApplication.run(YingshiApplication.class, args);
    }

}
