package com.smartgarden.integration.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmqpPlantResponseDto {
  private String correlationId;
  private boolean success;
  private String errorMessage;
  private Object payload; // PlantResponseDto ou List<PlantResponseDto>
}
