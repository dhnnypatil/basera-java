package com.basera;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Smoke test: the app's configuration is valid and every class Spring manages can be created.
 * It runs against the throw-away test MongoDB (TestMongo), with seeding and map lookups switched off.
 */
@SpringBootTest(properties = { "basera.seed=never", "basera.geocoding.enabled=false" })
@Import(TestMongo.class)
class BaseraApplicationTests {

    @Test
    void contextLoads() {
    }

}
