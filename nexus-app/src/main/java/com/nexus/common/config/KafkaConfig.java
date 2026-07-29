package com.nexus.common.config;

import com.nexus.ticket.infrastructure.messaging.TicketKafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic auto-creation for local development.
 *
 * <p>Spring's {@code KafkaAdmin} bean detects {@code NewTopic} beans on startup
 * and creates them if they don't already exist. In production, topics would be
 * pre-provisioned by the platform team with proper partition counts and
 * replication factors — these dev defaults are intentionally minimal.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic ticketCreatedTopic() {
        return TopicBuilder.name(TicketKafkaTopics.TICKET_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ticketStatusChangedTopic() {
        return TopicBuilder.name(TicketKafkaTopics.TICKET_STATUS_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
