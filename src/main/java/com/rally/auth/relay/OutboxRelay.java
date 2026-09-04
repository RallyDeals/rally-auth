package com.rally.auth.relay;

import com.rally.auth.config.OutboxRelayProperties;
import com.rally.auth.messaging.outbox.OutboxMessage;
import com.rally.auth.repository.OutboxJpaRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@EnableConfigurationProperties(OutboxRelayProperties.class)
public class OutboxRelay {

    static final Pattern OTLP_TRACE_ID_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");

    private final OutboxJpaRepository outboxJpaRepository;
    private final OutboxPublisher outboxPublisher;
    private final OutboxRelayProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Tracer tracer;

    public OutboxRelay(
        OutboxJpaRepository outboxJpaRepository,
        OutboxPublisher outboxPublisher,
        OutboxRelayProperties properties,
        PlatformTransactionManager transactionManager,
        Tracer tracer
    ) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.outboxPublisher = outboxPublisher;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.tracer = tracer;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.poll-interval-ms:1000}")
    public void relayPending() {
        transactionTemplate.executeWithoutResult(status -> relayBatch());
    }

    private void relayBatch() {
        List<OutboxMessage> pending = outboxJpaRepository.findPendingForUpdateSkipLocked(properties.getBatchSize());

        if (pending.isEmpty()) {
            return;
        }

        int published = 0;
        int retried = 0;
        int failed = 0;

        for (OutboxMessage message : pending) {

            Span span = reParentToStoredTrace(message);

            try (Tracer.SpanInScope spanInScope = span != null ? tracer.withSpan(span) : null) {
                boolean ok = outboxPublisher.publish(message);
                if (!ok) {
                    throw new RuntimeException("publish returned false");
                }
                message.setStatus("PUBLISHED");
                message.setPublishedAt(Instant.now());
                message.setLastError(null);
                published++;
            } catch (Exception e) {
                int retries = message.getRetryCount() + 1;
                message.setRetryCount(retries);
                message.setLastError(e.getMessage());
                if (retries >= message.getMaxRetries()) {
                    message.setStatus("FAILED");
                    failed++;
                } else {
                    message.setStatus("PENDING");
                    retried++;
                }
                log.warn("Relay failed for outbox message {} (attempt {} of {}) on topic {}: {}",
                    message.getMessageId(), message.getRetryCount(), message.getMaxRetries(),
                    message.getTopic(), e.getMessage());
            } finally {
                if (span != null) {
                    span.end();
                }
            }

            outboxJpaRepository.save(message);
        }

        log.info("Outbox relay run: {} pending, {} published, {} retried, {} failed",
            pending.size(), published, retried, failed);
    }

    private Span reParentToStoredTrace(OutboxMessage message) {
        String traceId = message.getTraceId();
        if (traceId == null || !OTLP_TRACE_ID_PATTERN.matcher(traceId).matches()) {
            return null;
        }
        return tracer.spanBuilder()
                .name("relay-outbox-message")
                .setParent(tracer.traceContextBuilder()
                        .traceId(traceId)
                        .spanId(randomValidSpanId())
                        .sampled(true)
                        .build())
                .start();
    }

    public static String randomValidSpanId() {
        return String.format("%016x", ThreadLocalRandom.current().nextLong());
    }
}