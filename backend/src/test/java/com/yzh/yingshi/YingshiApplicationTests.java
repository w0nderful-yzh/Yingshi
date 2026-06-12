package com.yzh.yingshi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-only-key",
        "app.scheduling.enabled=false"
})
class YingshiApplicationTests {

    @Test
    void contextLoads() {
    }

}
