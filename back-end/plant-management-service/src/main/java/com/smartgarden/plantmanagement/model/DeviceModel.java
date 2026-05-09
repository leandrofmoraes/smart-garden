package com.smartgarden.plantmanagement.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Representa um dispositivo IoT cadastrado no sistema.
 *
 * <p>
 * Cada device monitora uma planta específica ({@link #plant}).
 * Esse vínculo é o elo entre leitura e regras de cuidado:
 * ao chegar uma leitura do device, o sistema sabe qual planta avaliar.
 *
 * <p>
 * O campo {@code deviceKey} é o identificador MQTT enviado pelo
 * {@code integration-service} no campo {@code deviceId} da
 * {@code IrrigationReadingDto}.
 */
@Entity
@Table(name = "devices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "plant")
public class DeviceModel {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /**
   * Identificador MQTT do dispositivo — corresponde ao campo {@code deviceId}
   * da {@code IrrigationReadingDto} do integration-service.
   */
  @Column(name = "device_key", nullable = false, unique = true)
  private String deviceKey;

  private String name;
  private String ip;
  private String description;

  /**
   * Planta monitorada por este dispositivo.
   * Nullable — um device recém-descoberto via MQTT ainda não tem planta
   * associada.
   * A associação é feita manualmente via endpoint admin ou via AMQP.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plant_id")
  private PlantModel plant;

  @Column(name = "last_seen")
  private Instant lastSeen;

  private Boolean online;

  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
    updatedAt = Instant.now();
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }
}
