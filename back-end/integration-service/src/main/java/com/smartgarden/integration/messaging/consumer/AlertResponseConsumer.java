package com.smartgarden.integration.messaging.consumer;

import com.fasterxml.jackson.databind.json.JsonMapper;
//import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgarden.integration.cache.AlertCache;
import com.smartgarden.integration.dto.irrigation.IrrigationDecisionDto;
import com.smartgarden.integration.dto.messaging.AmqpPlantResponseDto;
import com.smartgarden.integration.service.IrrigationDecisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome mensagens da fila {@code alert.response} enviadas pelo
 * plant-management-service.
 *
 * <p>
 * <b>Garantias imutáveis:</b>
 * <ol>
 * <li>O {@link AlertCache} é sempre alimentado — antes de qualquer tentativa
 * de roteamento, independente do tipo de payload ou de falha posterior.</li>
 * <li>Falha de deserialização nunca interrompe o consumo — o alerta já foi
 * salvo no cache antes do try/catch.</li>
 * <li>Um payload só é qualificado como decisão de irrigação quando os três
 * campos obrigatórios estiverem presentes e válidos:
 * {@code shouldIrrigate}, {@code deviceId} e {@code targetHumidity}.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertResponseConsumer {

  private final AlertCache alertCache;
  private final IrrigationDecisionService irrigationDecisionService;
  // private final ObjectMapper objectMapper;
  private final JsonMapper objectMapper = JsonMapper.builder()
      .findAndAddModules()
      .build();

  @RabbitListener(queues = "${amqp.queue.alert}")
  public void consume(AmqpPlantResponseDto alert) {
    log.info("AMQP alert ← plant-management-service [correlationId={}]",
        alert.getCorrelationId());

    // Garantia 1: cache sempre recebe, aconteça o que acontecer depois.
    alertCache.addAlert(alert);

    // Garantias 2 e 3: isoladas em método separado com try/catch próprio.
    tryRouteIrrigationDecision(alert);
  }

  /* ------------------------------------------------------------------ */

  private void tryRouteIrrigationDecision(AmqpPlantResponseDto alert) {
    if (alert.getPayload() == null) {
      return;
    }

    try {
      IrrigationDecisionDto decision = objectMapper.convertValue(alert.getPayload(), IrrigationDecisionDto.class);

      if (!isValidIrrigationDecision(decision)) {
        return;
      }

      irrigationDecisionService.process(decision);

    } catch (Exception e) {
      // Cache já foi alimentado. Apenas loga e deixa o listener ACK a mensagem.
      log.warn("Could not route alert payload as irrigation decision — alert preserved in cache. Cause: {}",
          e.getMessage());
    }
  }

  /**
   * Um payload qualifica como decisão de irrigação somente se os três campos
   * estiverem presentes e válidos. Isso evita que alertas genéricos com
   * estrutura parcialmente parecida sejam interpretados como irrigação.
   *
   * @return {@code true} apenas se shouldIrrigate, deviceId E targetHumidity
   *         estiverem todos presentes e não-nulos/não-vazios
   */
  private boolean isValidIrrigationDecision(IrrigationDecisionDto decision) {
    if (decision.getShouldIrrigate() == null) {
      log.debug("Payload has no shouldIrrigate field — treating as generic alert");
      return false;
    }
    if (decision.getDeviceId() == null || decision.getDeviceId().isBlank()) {
      log.warn("Irrigation decision has shouldIrrigate but missing deviceId — skipping MQTT");
      return false;
    }
    if (decision.getTargetHumidity() == null) {
      log.warn(
          "Irrigation decision has shouldIrrigate and deviceId but missing targetHumidity — skipping MQTT [device={}]",
          decision.getDeviceId());
      return false;
    }
    return true;
  }
}
