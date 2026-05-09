package com.smartgarden.integration.dto.device;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCommandDto {

  @NotBlank(message = "Command type is required")
  private String type;

  /** Parâmetros adicionais do comando (ex: durationSeconds, targetMoisture) */
  private Map<String, Object> params;
}
