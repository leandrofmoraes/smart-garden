package com.smartgarden.plantmanagement.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "readings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "device")
public class ReadingModel {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "external_id")
  private String externalId;

  /**
   * Device que gerou esta leitura. FK nullable — device pode não estar
   * registrado no momento do ingest (auto-criado em seguida).
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "device_id")
  private DeviceModel device;

  /**
   * Identificador MQTT do device — sempre preenchido (NOT NULL).
   * Corresponde ao campo {@code deviceId} da IrrigationReadingDto.
   */
  @Column(name = "device_key", nullable = false)
  private String deviceKey;

  private Double humidity;
  private Boolean regando;

  @Column(name = "rega_pulsos")
  private Integer regaPulsos;
  @Column(name = "rega_volume_l")
  private Double regaVolumeL;
  @Column(name = "volume_total_l")
  private Double volumeTotalL;
  @Column(name = "rega_duracao_s")
  private Integer regaDuracaoS;
  @Column(name = "esp_ip")
  private String espIp;
  @Column(name = "esp_rssi")
  private Integer espRssi;
  @Column(name = "device_ts_ms")
  private Long deviceTsMs;

  @Column(name = "read_at")
  private Instant readAt;

  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
  }
}
