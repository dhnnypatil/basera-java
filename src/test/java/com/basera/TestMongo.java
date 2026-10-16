package com.basera;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * A real, throw-away MongoDB for tests, started in Docker by Testcontainers (Docker Desktop must be running).
 * The Node project's mongodb-memory-server played the same role.
 *
 * @ServiceConnection tells Spring Boot to connect to this container instead of the normal database.
 * Test classes that say @Import(TestMongo.class) share one container, because Spring reuses the same test setup.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestMongo {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer("mongo:8.0");
    }
}
