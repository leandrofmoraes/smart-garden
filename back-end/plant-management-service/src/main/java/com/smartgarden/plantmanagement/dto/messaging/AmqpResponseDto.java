package com.smartgarden.plantmanagement.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Envelope de response enviado de volta ao integration-service via RabbitMQ.
 *
 * <p>
 * Contrato alinhado com {@code AmqpPlantResponseDto} do integration-service:
 * campos idênticos para garantir deserialização sem erros.
 *
 * <p>
 * Campos:
 * <ul>
 * <li>{@code correlationId} — espelhado do request para matching no
 * PendingRequestRegistry</li>
 * <li>{@code success} — true se a operação foi bem-sucedida</li>
 * <li>{@code errorMessage} — mensagem de erro legível; null em sucesso</li>
 * <li>{@code payload} — objeto de retorno (PlantResponseDto, List,
 * IrrigationDecisionDto, etc.)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmqpResponseDto {
  private String correlationId;
  private boolean success;
  private String errorMessage;
  private Object payload;

  public static AmqpResponseDto ok(String correlationId, Object payload) {
    return AmqpResponseDto.builder()
        .correlationId(correlationId)
        .success(true)
        .payload(payload)
        .build();
  }

  public static AmqpResponseDto error(String correlationId, String message) {
    return AmqpResponseDto.builder()
        .correlationId(correlationId)
        .success(false)
        .errorMessage(message)
        .build();
  }
}
