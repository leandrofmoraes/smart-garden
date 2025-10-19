import { IsNotEmpty, IsNumber, IsOptional, IsBoolean, IsString } from 'class-validator';
import { Transform, Type } from 'class-transformer';

export class CreateReadingDto {
  @IsNotEmpty()
  @IsNumber()
  @Type(() => Number)
  humidity: number;

  @IsNotEmpty()
  @Transform(({ value }) => {
    if (typeof value === 'number') return new Date(value);
    if (typeof value === 'string') {
      // Se for string ISO
      if (value.includes('T')) return new Date(value);
      // Se for string numérica (epoch ms)
      if (/^\d+$/.test(value)) return new Date(parseInt(value, 10));
    }
    return new Date(); // fallback
  })
  timestamp: Date;

  @IsOptional()
  @IsBoolean()
  @Transform(({ value }) => {
    if (typeof value === 'boolean') return value;
    return value === 1 || value === '1' || value === 'true';
  })
  regando?: boolean;

  @IsOptional()
  @IsNumber()
  @Type(() => Number)
  rega_pulsos?: number;

  @IsOptional()
  @IsNumber()
  @Type(() => Number)
  rega_volume_l?: number;

  @IsOptional()
  @IsNumber()
  @Type(() => Number)
  volume_total_l?: number;

  @IsOptional()
  @IsNumber()
  @Type(() => Number)
  rega_duracao_s?: number;

  @IsOptional()
  @IsNumber()
  @Type(() => Number)
  device_ts_ms?: number;

  @IsOptional()
  @IsString()
  esp_ip?: string;

  @IsOptional()
  @IsNumber()
  @Type(() => Number)
  esp_rssi?: number;

  // Campo adicional que o ESP pode enviar
  @IsOptional()
  @IsString()
  timestamp_iso?: string;
}
