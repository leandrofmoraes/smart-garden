package com.smartgarden.plantmanagement.repository;

import com.smartgarden.plantmanagement.model.ReadingModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReadingRepository extends JpaRepository<ReadingModel, UUID> {

  /**
   * Leituras paginadas por deviceKey, ordenadas da mais recente para a mais
   * antiga.
   */
  @Query("""
      SELECT r FROM ReadingModel r
      WHERE r.deviceKey = :deviceKey
      ORDER BY r.readAt DESC
      """)
  List<ReadingModel> findByDeviceKey(@Param("deviceKey") String deviceKey,
      Pageable pageable);

  /**
   * Atalho para as 10 leituras mais recentes de um device.
   */
  default List<ReadingModel> findLatestByDeviceKey(String deviceKey) {
    return findByDeviceKey(deviceKey,
        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "readAt")));
  }
}
