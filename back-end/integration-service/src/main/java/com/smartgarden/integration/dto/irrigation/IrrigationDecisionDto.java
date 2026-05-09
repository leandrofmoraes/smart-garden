package com.smartgarden.integration.dto.irrigation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa a decisão de irrigação produzida pelo plant-management-service
 * e recebida via AMQP na fila {@code alert.response}.
 *
 * <p>
 * Espelha campo a campo o {@code IrrigationDecisionDto} do
 * plant-management-service para garantir deserialização correta pelo Jackson.
 *
 * <p>
 * Os três campos usados como critério de qualificação pelo consumer são:
 * <ul>
 * <li>{@code shouldIrrigate} — presente e não-nulo indica decisão de
 * irrigação</li>
 * <li>{@code deviceId} — destino obrigatório do comando MQTT</li>
 * <li>{@code targetHumidity} — condição de parada, evita loop infinito no
 * device</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IrrigationDecisionDto {

  private String deviceId;
  private String plantId;
  private Double currentHumidity;
  private Double minHumidity;
  private Double maxHumidity;
  private Double targetHumidity;
  private Boolean shouldIrrigate;
  private String reason;
  private String correlationId;
}
