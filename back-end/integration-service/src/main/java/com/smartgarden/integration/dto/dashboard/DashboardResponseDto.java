package com.smartgarden.integration.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardResponseDto {
    private List<DashboardPlantCardDto> plants;
    private List<DashboardReadingCardDto> latestReadings;
    private int onlineDevices;
    private int totalDevices;
    private Instant generatedAt;
}
