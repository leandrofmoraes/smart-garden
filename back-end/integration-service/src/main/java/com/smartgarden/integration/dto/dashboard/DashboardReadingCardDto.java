package com.smartgarden.integration.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardReadingCardDto {
    private String deviceId;
    private String deviceName;
    private Double humidity;
    private Boolean regando;
    private Integer espRssi;
    private String espIp;
    private Instant lastUpdated;
}
