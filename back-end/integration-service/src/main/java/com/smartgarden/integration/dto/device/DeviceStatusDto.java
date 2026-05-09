package com.smartgarden.integration.dto.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatusDto {
  private String deviceId;
  private String ip;
  private Integer rssi;
  private Boolean online;
  private Instant lastSeen;
}
