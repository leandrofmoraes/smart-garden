// Contrato de Leitura do integration-service
export interface Reading {
  id: string;              // obrigatório no contrato do backend
  _id?: string;            // campo legado do Mongo, pode vir em alguns payloads
  humidity: number;
  timestamp?: string;
  device_ts_ms?: number;
  regando?: boolean;
  rega_pulsos?: number;
  rega_volume_l?: number;
  volume_total_l?: number;
  rega_duracao_s?: number;
  esp_ip?: string;
  esp_rssi?: number;
  createdAt?: string;
  updatedAt?: string;
}
