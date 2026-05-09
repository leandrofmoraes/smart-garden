package com.smartgarden.plantmanagement.messaging.publisher;

import com.smartgarden.plantmanagement.dto.messaging.AmqpResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publica alertas de domínio ao integration-service via fila de alertas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertPublisher {

  private final RabbitTemplate rabbitTemplate;

  @Value("${amqp.exchange}")
  private String exchange;

  @Value("${amqp.routing-key.alert-response}")
  private String alertResponseKey;

  public void publish(AmqpResponseDto alert) {
    log.info("AMQP alert → integration-service [correlationId={}, type={}]",
        alert.getCorrelationId(), alert.getPayload());
    rabbitTemplate.convertAndSend(exchange, alertResponseKey, alert);
  }
}
