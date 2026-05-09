package com.smartgarden.plantmanagement.service;

import com.smartgarden.plantmanagement.dto.irrigation.IrrigationDecisionDto;
import com.smartgarden.plantmanagement.model.PlantCareModel;
import com.smartgarden.plantmanagement.model.ReadingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Regras de negócio para decisão de irrigação.
 *
 * <p>
 * Avalia se a umidade medida pelo sensor justifica acionar a rega,
 * usando os parâmetros de cuidado da planta associada ao dispositivo.
 *
 * <p>
 * O resultado inclui {@code targetHumidity} — o limite de parada —
 * para que o dispositivo IoT saiba quando interromper a rega.
 */
@Slf4j
@Service
public class IrrigationService {

  @Value("${irrigation.default-min-soil-moisture:30.0}")
  private double defaultMinSoilMoisture;

  @Value("${irrigation.default-max-soil-moisture:70.0}")
  private double defaultMaxSoilMoisture;

  /**
   * Avalia a necessidade de irrigação e retorna uma decisão completa.
   *
   * @param reading leitura persistida com humidity e deviceKey
   * @param care    parâmetros de cuidado da planta associada (pode ser null)
   * @param plantId id da planta para inclusão no resultado
   * @return decisão de irrigação com todos os campos necessários para o IoT
   */
  public IrrigationDecisionDto evaluate(ReadingModel reading,
      PlantCareModel care,
      String plantId) {

    String deviceId = reading.getDeviceKey();
    Double humidity = reading.getHumidity();

    // Sem dados de umidade: sem decisão possível
    if (humidity == null) {
      log.debug("No humidity data for device={} — returning NO_DATA", deviceId);
      return IrrigationDecisionDto.builder()
          .deviceId(deviceId)
          .plantId(plantId)
          .currentHumidity(null)
          .shouldIrrigate(false)
          .reason("NO_DATA")
          .build();
    }

    // Sem dados de cuidado: usa limites padrão e documenta
    if (care == null || care.getSoilMoistureMin() == null) {
      log.warn("No care data for plant={} — using defaults [device={}]", plantId, deviceId);
      return buildDecision(deviceId, plantId, humidity,
          defaultMinSoilMoisture, defaultMaxSoilMoisture, "NO_CARE_DATA");
    }

    double min = care.getSoilMoistureMin();
    double max = care.getSoilMoistureMax() != null ? care.getSoilMoistureMax() : defaultMaxSoilMoisture;

    return buildDecision(deviceId, plantId, humidity, min, max, null);
  }

  private IrrigationDecisionDto buildDecision(String deviceId, String plantId,
      double humidity,
      double min, double max,
      String forcedReason) {
    String reason;
    boolean shouldIrrigate;

    if (humidity < min) {
      reason = forcedReason != null ? forcedReason : "LOW_MOISTURE";
      shouldIrrigate = true;
      log.info("LOW_MOISTURE [device={}, humidity={}, min={}]", deviceId, humidity, min);
    } else if (humidity > max) {
      reason = "HIGH_MOISTURE";
      shouldIrrigate = false;
      log.info("HIGH_MOISTURE [device={}, humidity={}, max={}]", deviceId, humidity, max);
    } else {
      reason = forcedReason != null ? forcedReason : "ADEQUATE";
      shouldIrrigate = false;
      log.debug("ADEQUATE [device={}, humidity={}]", deviceId, humidity);
    }

    // Target = 60% da faixa ideal — para a rega antes de atingir o máximo
    double target = min + (max - min) * 0.6;

    return IrrigationDecisionDto.builder()
        .deviceId(deviceId)
        .plantId(plantId)
        .currentHumidity(humidity)
        .minHumidity(min)
        .maxHumidity(max)
        .targetHumidity(Math.round(target * 10.0) / 10.0)
        .shouldIrrigate(shouldIrrigate)
        .reason(reason)
        .build();
  }
}
