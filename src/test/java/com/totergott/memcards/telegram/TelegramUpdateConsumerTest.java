package com.totergott.memcards.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import com.totergott.memcards.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TelegramUpdateConsumerTest extends BaseTest {

    @Test
    public void test() {
        assertThat(true).isTrue();
    }
}