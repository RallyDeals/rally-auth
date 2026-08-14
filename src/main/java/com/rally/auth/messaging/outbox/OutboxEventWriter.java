package com.rally.auth.messaging.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rally.auth.messaging.contract.MessageHeaders;
import com.rally.auth.messaging.contract.UserEventTypes;
import com.rally.auth.repository.OutboxJpaRepository;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;


@Component
public class OutboxEventWriter {

    private static final String TRACE_MDC_KEY = "traceId";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OutboxJpaRepository outboxJpaRepository;

    public OutboxEventWriter(OutboxJpaRepository outboxJpaRepository) {
        this.outboxJpaRepository = outboxJpaRepository;
    }


    public void writeUserEvent(UUID userId, String eventType, JsonNode payload) {
        writeUserEvent(userId, eventType, payload, null);
    }

    public void writeUserEvent(UUID userId, String eventType, JsonNode payload, UUID causationId) {
        UUID messageId = UUID.randomUUID();
        UUID correlationId = causationId != null ? causationId : UUID.randomUUID();

        ObjectNode headers = OBJECT_MAPPER.createObjectNode();
        headers.put(MessageHeaders.ID, messageId.toString());
        headers.put(MessageHeaders.TYPE, eventType);
        headers.put(MessageHeaders.CORRELATION_ID, correlationId.toString());
        if (causationId != null) {
            headers.put(MessageHeaders.CAUSATION_ID, causationId.toString());
        }
        String traceId = MDC.get(TRACE_MDC_KEY);
        if (traceId != null) {
            headers.put(MessageHeaders.TRACE_ID, traceId);
        }

        OutboxMessage message = OutboxMessage.builder()
                .messageId(messageId)
                .aggregateId(userId)
                .aggregateType(UserEventTypes.AGGREGATE_TYPE)
                .topic(UserEventTypes.TOPIC)
                .messageKey(userId.toString())
                .messageType(eventType)
                .correlationId(correlationId)
                .causationId(causationId != null ? causationId.toString() : null)
                .traceId(traceId)
                .payload(payload)
                .headers(headers)
                .build();

        outboxJpaRepository.save(message);
    }
}