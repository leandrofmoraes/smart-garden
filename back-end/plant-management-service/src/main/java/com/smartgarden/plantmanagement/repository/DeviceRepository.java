package com.smartgarden.plantmanagement.repository;

import com.smartgarden.plantmanagement.model.DeviceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceModel, UUID> {

  /**
   * Busca device pelo deviceKey (identificador MQTT) e já faz JOIN FETCH na
   * planta.
   * Os cuidados da planta (care) serão carregados lazy dentro da transação ativa.
   * Evita problemas de LEFT JOIN FETCH encadeado quando a planta é nula.
   */
  @Query("""
      SELECT d FROM DeviceModel d
      LEFT JOIN FETCH d.plant
      WHERE d.deviceKey = :deviceKey
      """)
  Optional<DeviceModel> findByDeviceKeyWithPlant(@Param("deviceKey") String deviceKey);

  /**
   * Busca device apenas pelo deviceKey, sem carregar a planta.
   * Útil para atualizações simples da FK.
   */
  Optional<DeviceModel> findByDeviceKey(String deviceKey);

  @Modifying
  @Query("UPDATE DeviceModel d SET d.plant = null WHERE d.plant.id = :plantId")
  void unlinkPlantFromDevices(@Param("plantId") UUID plantId);
}
