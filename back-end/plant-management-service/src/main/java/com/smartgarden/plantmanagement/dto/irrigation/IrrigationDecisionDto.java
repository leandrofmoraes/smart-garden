package com.smartgarden.plantmanagement.dto.irrigation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado da avaliação de irrigação gerado pelo domínio.
 *
 * <p>
 * Este DTO é publicado ao {@code integration-service} via AMQP (fila de
 * alertas)
 * e por ele repassado ao dispositivo IoT via MQTT como comando de rega.
 *
 * <p>
 * O campo {@code targetHumidity} é o limite de parada da irrigação,
 * evitando que o dispositivo regue em loop infinito.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IrrigationDecisionDto {

  /** Identificador da leitura que originou esta decisão */
  private String correlationId;

  /** Identificador MQTT do dispositivo (deviceKey) */
  private String deviceId;

  /** ID da planta monitorada */
  private String plantId;

  /** Umidade atual medida pelo sensor (%) */
  private Double currentHumidity;

  /**
   * Limite mínimo de umidade da planta — rega necessária se abaixo deste valor
   */
  private Double minHumidity;

  /** Limite máximo de umidade da planta */
  private Double maxHumidity;

  /**
   * Umidade alvo para parada da rega.
   * Calculado como: {@code minHumidity + (maxHumidity - minHumidity) * 0.6}
   * para atingir 60% da faixa ideal — evita superumedecimento.
   */
  private Double targetHumidity;

  /** true se a rega deve ser acionada agora */
  private Boolean shouldIrrigate;

  /**
   * Motivo da decisão: LOW_MOISTURE, ADEQUATE, HIGH_MOISTURE, NO_CARE_DATA,
   * NO_DATA.
   */
  private String reason;
}
