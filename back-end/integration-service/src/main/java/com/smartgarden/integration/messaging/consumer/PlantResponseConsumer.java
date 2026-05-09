package com.smartgarden.integration.messaging.consumer;

import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import com.smartgarden.integration.messaging.correlation.PendingRequestRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome respostas de operações de plantas do plant-management-service.
 * Resolve o {@link java.util.concurrent.CompletableFuture} registrado pelo
 * {@link com.smartgarden.integration.service.PlantService} via
 * {@link PendingRequestRegistry}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlantResponseConsumer {

    private final PendingRequestRegistry pendingRequestRegistry;

    @RabbitListener(queues = "${amqp.queue.plant-response}")
    public void consume(AmqpPlantResponseDto response) {
        log.info("AMQP ← plant-management-service [correlationId={}, success={}]",
                response.getCorrelationId(), response.isSuccess());

        if (response.getCorrelationId() == null) {
            log.warn("Received plant response without correlationId — ignoring message");
            return;
        }

        boolean resolved = pendingRequestRegistry.resolve(response.getCorrelationId(), response);

        if (!resolved) {
            log.warn("No pending request for correlationId={} — response may have arrived after timeout",
                    response.getCorrelationId());
        }
    }
}
