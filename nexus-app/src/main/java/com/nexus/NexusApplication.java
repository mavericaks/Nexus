package com.nexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Nexus application.
 *
 * Placed at the root package (com.nexus) so that Spring's component scan
 * automatically discovers every feature module beneath it:
 * com.nexus.ticket, com.nexus.tenant, com.nexus.ai, com.nexus.notification, com.nexus.shared
 */
@SpringBootApplication
public class NexusApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusApplication.class, args);
    }
}
