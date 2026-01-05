package com.gotcha;

import com.gotcha.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class GotchaServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
