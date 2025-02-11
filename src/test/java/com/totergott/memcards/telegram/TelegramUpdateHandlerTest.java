package com.totergott.memcards.telegram;

import com.totergott.memcards.BaseTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@SpringBootTest
@TestConstructor(autowireMode = AutowireMode.ALL)
class TelegramUpdateHandlerTest extends BaseTest {

}