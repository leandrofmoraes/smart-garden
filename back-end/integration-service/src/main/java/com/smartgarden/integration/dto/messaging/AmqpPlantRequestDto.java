package com.smartgarden.integration.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmqpPlantRequestDto {
  private String correlationId;
  private String action; // CREATE | UPDATE | DELETE | GET | LIST
  private String plantId; // nullable – usado em GET / UPDATE / DELETE
  private Object payload; // PlantRequestDto ou null
}
