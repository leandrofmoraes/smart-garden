package com.smartgarden.plantmanagement.mapper;

import com.smartgarden.plantmanagement.dto.reading.IrrigationReadingDto;
import com.smartgarden.plantmanagement.model.DeviceModel;
import com.smartgarden.plantmanagement.model.ReadingModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Converte IrrigationReadingDto para ReadingModel.
 * Garante que {@code deviceKey} seja sempre preenchido (coluna NOT NULL).
 */
@Component
public class ReadingMapper {

  public ReadingModel toModel(IrrigationReadingDto dto, DeviceModel device) {
    String deviceKey = resolveDeviceKey(dto, device);
    return ReadingModel.builder()
        .externalId(dto.getId())
        .device(device)
        .deviceKey(deviceKey)
        .humidity(dto.getHumidity())
        .regando(dto.getRegando())
        .regaPulsos(dto.getRegaPulsos())
        .regaVolumeL(dto.getRegaVolumeL())
        .volumeTotalL(dto.getVolumeTotalL())
        .regaDuracaoS(dto.getRegaDuracaoS())
        .espIp(dto.getEspIp())
        .espRssi(dto.getEspRssi())
        .deviceTsMs(dto.getDeviceTsMs())
        .readAt(dto.getTimestamp())
        .build();
  }

  private String resolveDeviceKey(IrrigationReadingDto dto, DeviceModel device) {
    // Preferência: deviceKey do device registrado (canônico)
    if (device != null && StringUtils.hasText(device.getDeviceKey())) {
      return device.getDeviceKey();
    }
    // Fallback: deviceId do DTO (string enviada pelo integration-service)
    if (StringUtils.hasText(dto.getDeviceId())) {
      return dto.getDeviceId();
    }
    throw new IllegalStateException("Cannot persist reading without deviceKey");
  }
}
