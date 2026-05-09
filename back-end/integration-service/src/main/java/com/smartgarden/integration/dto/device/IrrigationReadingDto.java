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
public class IrrigationReadingDto {

  private String id;

  @JsonProperty("_id")
  private String mongoId;

  private Double humidity;
  private Instant timestamp;
  private Boolean regando;

  @JsonProperty("rega_pulsos")
  private Integer regaPulsos;

  @JsonProperty("rega_volume_l")
  private Double regaVolumeL;

  @JsonProperty("volume_total_l")
  private Double volumeTotalL;

  @JsonProperty("rega_duracao_s")
  private Integer regaDuracaoS;

  @JsonProperty("esp_ip")
  private String espIp;

  @JsonProperty("esp_rssi")
  private Integer espRssi;

  @JsonProperty("device_ts_ms")
  private Long deviceTsMs;

  private Instant createdAt;
  private Instant updatedAt;

  /** Preenchido pelo MqttSubscriber com base no tópico MQTT */
  private String deviceId;
}
