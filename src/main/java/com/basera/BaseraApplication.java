package com.basera;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point:  ./mvnw spring-boot:run   (the Node app's server.js)
 *
 * @SpringBootApplication tells Spring to scan this package (com.basera) and everything under it, find our
 * classes marked @Controller, @Service, @Component, @Configuration, ... create one instance of each, wire
 * them together and start the web server. That's why there's no app.js-style list of app.use(...) calls:
 * Spring discovers the pieces itself.
 */
@SpringBootApplication
public class BaseraApplication {

    public static void main(String[] args) {
        // Booking dates are stored as midnight UTC (same as the Node app), so do all date maths in UTC.
        // Without this, a laptop in India would save "16 Oct" as 15 Oct 18:30 UTC.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(BaseraApplication.class, args);
    }

}
