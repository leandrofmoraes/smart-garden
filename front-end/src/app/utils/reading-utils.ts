import { Reading, NormalizedReading } from '../models/reading.model';

export function timestampToMs(value: unknown): number {
  if (value === null || value === undefined) return NaN;
  if (typeof value === 'number') {
    return value < 1e12 ? Math.floor(value * 1000) : Math.floor(value);
  }
  if (typeof value === 'string') {
    const s = value.trim();
    if (!s) return NaN;
    if (/^\d+$/.test(s)) {
      const n = parseInt(s, 10);
      return n < 1e12 ? Math.floor(n * 1000) : n;
    }
    const d = new Date(s);
    return isNaN(d.getTime()) ? NaN : d.getTime();
  }
  if (value instanceof Date) {
    return isNaN(value.getTime()) ? NaN : value.getTime();
  }
  return NaN;
}

export function normalizeReading(r: Reading): NormalizedReading {
  const source = r.timestamp ?? r.device_ts_ms ?? r.createdAt ?? null;
  let ms = timestampToMs(source);
  if (!Number.isFinite(ms)) ms = timestampToMs(r.device_ts_ms ?? null);
  const iso = Number.isFinite(ms) ? new Date(ms).toISOString() : null;

  return {
    _id: r._id,
    humidity: r.humidity,
    tsMs: Number.isFinite(ms) ? ms : null,
    timestampIso: iso,
    regando: typeof r.regando === 'boolean' ? r.regando : r.regando === 1,
    rega_pulsos: r.rega_pulsos,
    rega_volume_l: r.rega_volume_l,
    volume_total_l: r.volume_total_l,
    rega_duracao_s: r.rega_duracao_s,
    device_ts_ms: r.device_ts_ms,
    esp_ip: r.esp_ip,
    esp_rssi: r.esp_rssi,
    original: r,
  };
}
