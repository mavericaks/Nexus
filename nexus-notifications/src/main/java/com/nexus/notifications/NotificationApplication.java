package com.nexus.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nexus Notifications Microservice.
 *
 * <p>A standalone Spring Boot application that consumes ticket lifecycle
 * events from Kafka and dispatches notifications (email, Slack, webhook).
 *
 * <p>This service has NO dependency on {@code nexus-app} — it only shares
 * the Kafka topic contract (topic names and JSON event schemas). This
 * allows independent deployment, scaling, and release cycles.
 */
@SpringBootApplication
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}