package com.basera.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB settings. The connection itself needs no code: Spring Boot builds it from compose.yaml (dev)
 * or spring.mongodb.uri (prod) - the job config/db.js did by hand.
 *
 * @EnableMongoAuditing makes Spring Data fill in the @CreatedDate / @LastModifiedDate fields on save,
 * which is Mongoose's `{ timestamps: true }`.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
