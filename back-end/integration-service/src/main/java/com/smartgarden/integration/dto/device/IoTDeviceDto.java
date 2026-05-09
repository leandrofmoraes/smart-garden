package com.smartgarden.integration.dto.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IoTDeviceDto {
  private String id;
  private String name;
  private String ip;
  private String description;
  private DeviceStatusDto status;
}
