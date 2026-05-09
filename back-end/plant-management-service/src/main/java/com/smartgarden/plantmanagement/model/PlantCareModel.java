package com.smartgarden.plantmanagement.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plant_care")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "plant") // evita StackOverflow no OneToOne bidirecional
public class PlantCareModel {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plant_id", nullable = false)
  private PlantModel plant;

  @Column(name = "light_lux_min")
  private Double lightLuxMin;
  @Column(name = "light_lux_max")
  private Double lightLuxMax;
  @Column(name = "temp_min")
  private Double tempMin;
  @Column(name = "temp_max")
  private Double tempMax;
  @Column(name = "env_humidity_min")
  private Double envHumidityMin;
  @Column(name = "env_humidity_max")
  private Double envHumidityMax;
  @Column(name = "soil_moisture_min")
  private Double soilMoistureMin;
  @Column(name = "soil_moisture_max")
  private Double soilMoistureMax;

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
