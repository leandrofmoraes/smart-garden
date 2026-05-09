package com.smartgarden.plantmanagement.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "care") // evita StackOverflow no OneToOne bidirecional
public class PlantModel {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(name = "scientific_name")
  private String scientificName;

  @Column(name = "image_url")
  private String imageUrl;

  /** pid retornado pela Open PlantBook para evitar buscas duplicadas */
  @Column(name = "plantbook_pid")
  private String plantbookPid;

  @OneToOne(mappedBy = "plant", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  private PlantCareModel care;

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
