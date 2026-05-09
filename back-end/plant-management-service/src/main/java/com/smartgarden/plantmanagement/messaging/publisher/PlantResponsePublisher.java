package com.smartgarden.plantmanagement.messaging.publisher;

import com.smartgarden.plantmanagement.dto.messaging.AmqpResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publica respostas de operações de planta de volta ao integration-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlantResponsePublisher {

  private final RabbitTemplate rabbitTemplate;

  @Value("${amqp.exchange}")
  private String exchange;

  @Value("${amqp.routing-key.plant-response}")
  private String plantResponseKey;

  public void publish(AmqpResponseDto response) {
    log.info("AMQP → integration-service [correlationId={}, success={}]",
        response.getCorrelationId(), response.isSuccess());
    rabbitTemplate.convertAndSend(exchange, plantResponseKey, response);
  }
}
