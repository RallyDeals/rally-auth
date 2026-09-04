package com.rally.auth.messaging.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rally.auth.messaging.contract.MessageHeaders;
import com.rally.auth.messaging.contract.UserEventTypes;
import com.rally.auth.repository.OutboxJpaRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;


@Component
public class OutboxEventWriter {

    private static final String TRACE_MDC_KEY = "traceId";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OutboxJpaRepository outboxJpaRepository;
    private final Tracer tracer;

    public OutboxEventWriter(OutboxJpaRepository outboxJpaRepository, Tracer tracer) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.tracer = tracer;
    }


    public void writeUserEvent(UUID userId, String eventType, JsonNode payload) {


        UUID messageId = UUID.randomUUID();
        UUID correlationId = resolveCorrelationId();
        String traceId = currentTraceId();

        ObjectNode headers = OBJECT_MAPPER.createObjectNode();
        headers.put(MessageHeaders.ID, messageId.toString());
        headers.put(MessageHeaders.TYPE, eventType);
        headers.put(MessageHeaders.CORRELATION_ID, correlationId.toString());

        OutboxMessage message = OutboxMessage.builder()
                .messageId(messageId)
                .aggregateId(userId)
                .aggregateType(UserEventTypes.AGGREGATE_TYPE)
                .topic(UserEventTypes.TOPIC)
                .messageKey(userId.toString())
                .messageType(eventType)
                .correlationId(correlationId)
                .causationId(correlationId.toString())
                .traceId(traceId)
                .payload(payload)
                .headers(headers)
                .build();

        outboxJpaRepository.save(message);
    }

    private UUID resolveCorrelationId() {
        String mdcCorrelationId = MDC.get(MessageHeaders.CORRELATION_ID);
        if (mdcCorrelationId != null && !mdcCorrelationId.isBlank()) {
            try {
                return UUID.fromString(mdcCorrelationId);
            } catch (IllegalArgumentException ignored) {
                // fall through to random if MDC value is not a UUID
            }
        }
        return UUID.randomUUID();
    }

    private String currentTraceId() {
        Span span = tracer.currentSpan();
        if (span != null && span.context() != null) {
            String traceId = span.context().traceId();
            if (traceId != null && !traceId.isBlank()) {
                return traceId;
            }
        }
        String mdcTraceId = MDC.get(TRACE_MDC_KEY);
        if (mdcTraceId != null && !mdcTraceId.isBlank()) {
            return mdcTraceId;
        }
        return null;
    }
}