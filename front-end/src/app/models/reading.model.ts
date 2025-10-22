// src/app/models/reading.model.ts

export interface Reading {
  _id?: string;                // pode vir do Mongo, opcional em payloads locais
  humidity: number;
  timestamp?: string | number; // backend envia ISO string ou epoch ms (ou campo device_ts_ms)
  timestamp_iso?: string;      // opcional (se você gerar um iso extra)
  device_ts_ms?: number;
  regando?: boolean | number;      // pode vir true/false ou 1/0 do gateway
  rega_pulsos?: number;
  rega_volume_l?: number;
  volume_total_l?: number;
  rega_duracao_s?: number;
  esp_ip?: string;
  esp_rssi?: number;
  createdAt?: string;          // mongoose timestamps (opcional)
  updatedAt?: string;
  __v?: number;
}

/**
 * NormalizedReading - formato útil internamente no front (após conversão)
 * Ex.: timestamp convertido para ms e ISO, evita checar vários formatos toda hora.
 */
export interface NormalizedReading {
  _id?: string;
  humidity: number;
  tsMs: number | null;         // epoch ms (ou null se não disponível)
  timestampIso: string | null; // ISO string (ou null)
  regando?: boolean;
  rega_pulsos?: number;
  rega_volume_l?: number;
  volume_total_l?: number;
  rega_duracao_s?: number;
  device_ts_ms?: number;
  esp_ip?: string;
  esp_rssi?: number;
  // mantém o original caso precise
  original?: Reading;
}

/**
 * Classe utilitária para normalizar/parsing dos dados recebidos da API.
 * Use ReadingModel.fromApi(obj) para garantir tipos corretos.
 */
export class ReadingModel implements Reading {
  _id: string = '';
  humidity: number = 0;
  timestamp: string = '';
  regando?: boolean;
  rega_pulsos?: number;
  rega_volume_l?: number;
  volume_total_l?: number;
  rega_duracao_s?: number;
  device_ts_ms?: number;
  esp_ip?: string;
  esp_rssi?: number;
  createdAt?: string;
  updatedAt?: string;
  __v?: number;

  constructor(init?: Partial<Reading>) {
    if (init) Object.assign(this, init);
  }

  /** factory: normaliza o payload bruto vindo do backend/gateway */
  static fromApi(raw: any): ReadingModel {
    const r = new ReadingModel();

    // id
    if (raw && raw._id) r._id = String(raw._id);

    // humidity: aceita string/number
    if (raw && raw.humidity != null) r.humidity = Number(raw.humidity);

    // timestamp: pode ser number (epoch-ms) ou ISO string; armazenamos string
    if (raw && raw.timestamp != null) r.timestamp = String(raw.timestamp);
    else if (raw && raw.timestamp_iso) r.timestamp = String(raw.timestamp_iso);
    else if (raw && raw.createdAt) r.timestamp = String(raw.createdAt);

    // regando: aceita 0/1, "0"/"1", true/false
    if (raw && raw.regando != null) {
      const v = raw.regando;
      r.regando = (v === true || v === 'true' || v === 1 || v === '1');
    }

    // campos numéricos opcionais
    const numFields = ['rega_pulsos', 'rega_volume_l', 'volume_total_l', 'rega_duracao_s', 'device_ts_ms', 'esp_rssi', '__v'];
    numFields.forEach((f) => {
      if (raw && raw[f] != null) {
        const n = Number(raw[f]);
        if (!Number.isNaN(n)) (r as any)[f] = n;
      }
    });

    // esp_ip (string)
    if (raw && raw.esp_ip) r.esp_ip = String(raw.esp_ip);

    // createdAt / updatedAt
    if (raw && raw.createdAt) r.createdAt = String(raw.createdAt);
    if (raw && raw.updatedAt) r.updatedAt = String(raw.updatedAt);

    return r;
  }

  /** retorna timestamp como Date — tenta parsear ISO ou epoch-ms */
  getTimestampAsDate(): Date {
    if (!this.timestamp) return new Date(NaN);
    // se só dígitos -> epoch ms
    if (/^\d+$/.test(this.timestamp)) return new Date(parseInt(this.timestamp, 10));
    // se ISO (ou string legível) -> Date
    const d = new Date(this.timestamp);
    return d;
  }

  /** serializa para formato que o backend espera (útil para PUT/POST) */
  toApi(): any {
    const out: any = {
      humidity: this.humidity,
      timestamp: this.timestamp,
    };
    if (this.regando != null) out.regando = this.regando;
    if (this.rega_pulsos != null) out.rega_pulsos = this.rega_pulsos;
    if (this.rega_volume_l != null) out.rega_volume_l = this.rega_volume_l;
    if (this.volume_total_l != null) out.volume_total_l = this.volume_total_l;
    if (this.rega_duracao_s != null) out.rega_duracao_s = this.rega_duracao_s;
    if (this.device_ts_ms != null) out.device_ts_ms = this.device_ts_ms;
    if (this.esp_ip) out.esp_ip = this.esp_ip;
    if (this.esp_rssi != null) out.esp_rssi = this.esp_rssi;
    return out;
  }
}
