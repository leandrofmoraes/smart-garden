package com.smartgarden.plantmanagement.dto.plant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantResponseDto {
  private String id;
  private String name;
  private String scientificName;
  private String imageUrl;
  private PlantCareDto care;
}
