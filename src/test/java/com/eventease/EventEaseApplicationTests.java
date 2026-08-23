package com.eventease;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventEaseApplicationTests {

    @Test
    void contextLoads() {
        EventEaseApplication application = new EventEaseApplication();
        assertThat(application).isNotNull();
    }
}
