package com.smartgarden.integration.dto.plant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantCareDto {
  private CareRangeDto lightLux;
  private CareRangeDto temperature;
  private CareRangeDto envHumidity;
  private CareRangeDto soilMoisture;
}
