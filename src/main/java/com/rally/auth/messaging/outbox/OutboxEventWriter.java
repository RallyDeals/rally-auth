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

/**
 * Persists outbox messages inside the same transaction as the business change.
 * Envelope fields (X-Id, X-Type, X-Correlation-Id, X-Causation-Id, X-Trace-Id)
 * live in the headers JSONB; the payload carries business fields only.
 */
@Component
public class OutboxEventWriter {

    private static final String TRACE_MDC_KEY = "traceId";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OutboxJpaRepository outboxJpaRepository;

    public OutboxEventWriter(OutboxJpaRepository outboxJpaRepository) {
        this.outboxJpaRepository = outboxJpaRepository;
    }

    /**
     * Writes a User event (topic {@code user.events}, aggregate type User).
     * Correlation id is generated per request; causation id and trace id are
     * carried from the surrounding context when present.
     */
    public void writeUserEvent(UUID userId, String eventType, JsonNode payload) {
        UUID messageId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        ObjectNode headers = OBJECT_MAPPER.createObjectNode();
        headers.put(MessageHeaders.ID, messageId.toString());
        headers.put(MessageHeaders.TYPE, eventType);
        headers.put(MessageHeaders.CORRELATION_ID, correlationId.toString());
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
                .traceId(traceId)
                .payload(payload)
                .headers(headers)
                .build();

        outboxJpaRepository.save(message);
    }
}