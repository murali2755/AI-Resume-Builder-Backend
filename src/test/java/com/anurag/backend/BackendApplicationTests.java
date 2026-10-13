package com.anurag.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic Spring context load test.
 * Verifies the application context starts correctly with all beans.
 */
@SpringBootTest
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, all Spring beans (Security, JPA, JWT, etc.) loaded correctly
    }
}
