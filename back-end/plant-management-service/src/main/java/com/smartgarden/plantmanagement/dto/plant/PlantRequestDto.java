package com.smartgarden.plantmanagement.dto.plant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantRequestDto {

  @NotBlank(message = "Plant name is required")
  private String name;

  private String scientificName;
  private String imageUrl;

  private String deviceKey;

  @Valid
  private PlantCareDto care;
}
