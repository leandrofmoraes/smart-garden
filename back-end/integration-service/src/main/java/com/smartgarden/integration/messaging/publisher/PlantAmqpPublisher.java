package com.smartgarden.integration.messaging.publisher;

import com.smartgarden.integration.dto.messaging.AmqpPlantRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publica requests de operações de plantas no RabbitMQ para processamento
 * pelo plant-management-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlantAmqpPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${amqp.exchange}")
    private String exchange;

    @Value("${amqp.routing-key.plant-request}")
    private String plantRequestKey;

    public void publish(AmqpPlantRequestDto request) {
        log.info("AMQP → plant-management-service [action={}, correlationId={}]",
                request.getAction(), request.getCorrelationId());
        rabbitTemplate.convertAndSend(exchange, plantRequestKey, request);
    }
}
