package com.smartgarden.plantmanagement.service;

import com.smartgarden.plantmanagement.dto.reading.IrrigationReadingDto;
import com.smartgarden.plantmanagement.mapper.ReadingMapper;
import com.smartgarden.plantmanagement.model.DeviceModel;
import com.smartgarden.plantmanagement.model.ReadingModel;
import com.smartgarden.plantmanagement.repository.DeviceRepository;
import com.smartgarden.plantmanagement.repository.ReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingService {

  private final ReadingRepository readingRepository;
  private final DeviceRepository deviceRepository;
  private final ReadingMapper readingMapper;

  /**
   * Persiste uma leitura IoT recebida via AMQP.
   *
   * <p>
   * Resolve o device pelo {@code deviceId} do DTO (que corresponde ao
   * {@code deviceKey} no banco — identificador MQTT do dispositivo).
   * Se o device não existir, é criado automaticamente sem planta vinculada.
   *
   * @return leitura persistida com device resolvido
   */
  @Transactional
  public ReadingModel saveReading(IrrigationReadingDto dto) {
    DeviceModel device = resolveOrCreateDevice(dto.getDeviceId(), dto.getEspIp());
    ReadingModel reading = readingMapper.toModel(dto, device);
    ReadingModel saved = readingRepository.save(reading);
    log.info("Reading saved [device={}, humidity={}, id={}]",
        dto.getDeviceId(), dto.getHumidity(), saved.getId());
    return saved;
  }

  /**
   * Localiza o device pelo deviceKey com planta já carregada (JOIN FETCH).
   * Se não encontrar, cria um novo device sem planta — a associação deve
   * ser feita explicitamente via endpoint admin.
   *
   * <p>
   * Usa {@code REQUIRES_NEW} para que a criação do device seja comitada
   * independentemente da transação do caller.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public DeviceModel resolveOrCreateDevice(String deviceKey, String ip) {
    return deviceRepository.findByDeviceKeyWithPlant(deviceKey)
        .map(device -> {
          device.setLastSeen(Instant.now());
          device.setOnline(true);
          if (ip != null && !ip.isBlank())
            device.setIp(ip);
          return deviceRepository.save(device);
        })
        .orElseGet(() -> {
          log.info("Auto-registering new device: deviceKey={}", deviceKey);
          return deviceRepository.save(DeviceModel.builder()
              .deviceKey(deviceKey)
              .name(deviceKey)
              .ip(ip)
              .online(true)
              .lastSeen(Instant.now())
              .build());
        });
  }

  /**
   * Retorna as últimas leituras de um device pelo deviceKey.
   * Chamado via AMQP pelo integration-service quando o ReadingCache está vazio.
   */
  @Transactional(readOnly = true)
  public List<IrrigationReadingDto> getRecentByDeviceKey(String deviceKey) {
    return readingRepository.findLatestByDeviceKey(deviceKey)
        .stream()
        .map(this::toDto)
        .toList();
  }

  private IrrigationReadingDto toDto(ReadingModel r) {
    return IrrigationReadingDto.builder()
        .id(r.getId() != null ? r.getId().toString() : null)
        .humidity(r.getHumidity())
        .timestamp(r.getReadAt())
        .regando(r.getRegando())
        .regaPulsos(r.getRegaPulsos())
        .regaVolumeL(r.getRegaVolumeL())
        .volumeTotalL(r.getVolumeTotalL())
        .regaDuracaoS(r.getRegaDuracaoS())
        .espIp(r.getEspIp())
        .espRssi(r.getEspRssi())
        .deviceTsMs(r.getDeviceTsMs())
        .deviceId(r.getDeviceKey())
        .build();
  }
}
