package com.smartgarden.integration.dto.device;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttTelemetryDto {
  private String deviceId;
  private Double humidity;
  private Double temperature;
  private Double lightLux;
  private Double soilMoisture;
  private Boolean regando;

  @JsonProperty("esp_ip")
  private String espIp;

  @JsonProperty("esp_rssi")
  private Integer espRssi;

  private Instant timestamp;
}
