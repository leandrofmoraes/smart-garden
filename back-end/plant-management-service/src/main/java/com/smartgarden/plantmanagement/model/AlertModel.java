package com.smartgarden.plantmanagement.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "plant")
public class AlertModel {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plant_id")
  private PlantModel plant;

  @Column(name = "device_key")
  private String deviceKey;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private String message;

  @Column(nullable = false)
  private String severity;

  private Boolean resolved;

  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
    if (resolved == null)
      resolved = false;
    if (severity == null)
      severity = "WARNING";
  }
}
