package com.nexus.notifications.consumer;

import com.nexus.notifications.NotificationApplication;
import com.nexus.notifications.handler.NotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;

@SpringBootTest(classes = NotificationApplication.class)
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@TestPropertySource(properties = {
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@ActiveProfiles("test")
class NotificationEventConsumerTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private NotificationDispatcher dispatcher;

    @Test
    void shouldConsumeEventAndDispatch() throws Exception {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        TicketStatusChangedMessage message = new TicketStatusChangedMessage(
                tenantId, ticketId, "NEW", "ESCALATED"
        );

        // Act
        kafkaTemplate.send("nexus.tickets.status-changed", ticketId.toString(), message);

        // Assert - wait up to 5 seconds for the consumer to process and call dispatcher
        verify(dispatcher, timeout(5000)).dispatch(any(TicketStatusChangedMessage.class));
    }
}