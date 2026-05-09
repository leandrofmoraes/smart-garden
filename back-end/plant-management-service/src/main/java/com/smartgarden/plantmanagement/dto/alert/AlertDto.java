package com.smartgarden.plantmanagement.dto.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {
  private String id;
  private String plantId;
  private String deviceKey;
  private String type;
  private String message;
  private String severity;
  private Boolean resolved;
  private Instant createdAt;
}
