package com.smartgarden.plantmanagement.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Envelope de request recebido do integration-service via RabbitMQ.
 *
 * <p>
 * Contrato alinhado com {@code AmqpPlantRequestDto} do integration-service:
 * campos idênticos para garantir deserialização sem erros.
 *
 * <p>
 * Campos:
 * <ul>
 * <li>{@code correlationId} — UUID gerado pelo integration-service para
 * correlacionar resposta</li>
 * <li>{@code action} — CREATE | UPDATE | DELETE | GET | LIST</li>
 * <li>{@code plantId} — presente em GET, UPDATE, DELETE; null em CREATE e
 * LIST</li>
 * <li>{@code payload} — {@code PlantRequestDto} serializado; null em GET,
 * DELETE, LIST</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmqpRequestDto {
  private String correlationId;
  private String action;
  private String plantId;
  private Object payload;
}
