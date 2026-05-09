package com.smartgarden.plantmanagement.messaging.consumer;

import com.smartgarden.plantmanagement.dto.irrigation.IrrigationDecisionDto;
import com.smartgarden.plantmanagement.dto.messaging.AmqpResponseDto;
import com.smartgarden.plantmanagement.dto.reading.IrrigationReadingDto;
import com.smartgarden.plantmanagement.messaging.publisher.AlertPublisher;
import com.smartgarden.plantmanagement.model.DeviceModel;
import com.smartgarden.plantmanagement.model.PlantCareModel;
import com.smartgarden.plantmanagement.model.PlantModel;
import com.smartgarden.plantmanagement.model.ReadingModel;
import com.smartgarden.plantmanagement.service.AlertService;
import com.smartgarden.plantmanagement.service.IrrigationService;
import com.smartgarden.plantmanagement.service.ReadingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consome leituras IoT do integration-service e aplica regras de domínio.
 *
 * <p>
 * Fluxo:
 * <ol>
 * <li>Persiste a leitura</li>
 * <li>Resolve o device e a planta associada via FK</li>
 * <li>Avalia a regra de irrigação com os cuidados da planta correta</li>
 * <li>Publica a decisão via AMQP para o integration-service</li>
 * <li>Se necessário, persiste um alerta</li>
 * </ol>
 *
 * <p>
 * <b>Não usa mais a "primeira planta encontrada".</b>
 * A planta é obtida exclusivamente pelo vínculo {@code device.plant}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReadingIngestConsumer {

  private final ReadingService readingService;
  private final IrrigationService irrigationService;
  private final AlertService alertService;
  private final AlertPublisher alertPublisher;

  @RabbitListener(queues = "${amqp.queue.reading}")
  @Transactional
  public void consume(IrrigationReadingDto dto) {
    log.info("AMQP reading ← integration-service [device={}, humidity={}]",
        dto.getDeviceId(), dto.getHumidity());

    try {
      // Persiste leitura e resolve device com planta (JOIN FETCH)
      ReadingModel reading = readingService.saveReading(dto);
      DeviceModel device = reading.getDevice();

      // Obtém planta e cuidados associados ao device
      PlantModel plant = device != null ? device.getPlant() : null;
      PlantCareModel care = plant != null ? plant.getCare() : null;
      String plantId = plant != null ? plant.getId().toString() : null;

      if (plant == null) {
        log.warn("Device '{}' has no plant associated — evaluating with defaults",
            dto.getDeviceId());
      }

      // Avalia irrigação com a planta correta
      IrrigationDecisionDto decision = irrigationService.evaluate(reading, care, plantId);

      // Publica decisão ao integration-service (que repassa ao IoT via MQTT)
      alertPublisher.publish(AmqpResponseDto.ok(null, decision));

      // Persiste alerta se rega ou excesso de água
      if (Boolean.TRUE.equals(decision.getShouldIrrigate())
          || "HIGH_MOISTURE".equals(decision.getReason())) {
        alertService.createIrrigationAlert(reading, plant, decision);
      }

    } catch (Exception e) {
      log.error("Error processing reading [device={}]: {}",
          dto.getDeviceId(), e.getMessage(), e);
    }
  }
}
