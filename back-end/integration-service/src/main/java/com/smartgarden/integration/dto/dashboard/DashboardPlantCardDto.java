package com.smartgarden.integration.dto.dashboard;

import com.smartgarden.integration.dto.plant.PlantCareDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardPlantCardDto {
    private String id;
    private String name;
    private String scientificName;
    private String imageUrl;
    private PlantCareDto care;
}
