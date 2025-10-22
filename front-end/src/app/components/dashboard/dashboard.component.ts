import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Reading } from '../../models/reading.model';
import { ReadingService } from '../../services/reading-service.service';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,
})
export class DashboardComponent implements OnInit {
  readings: Reading[] = [];

  // arrays para gráficos/histórico (opcional) — timestampIso e tsMs podem ser nulos
  humidities: { humidity: number; timestampIso: string | null; tsMs: number | null }[] = [];

  // valores para cards
  umidadeMedia = 0;
  umidadeMinima = 0;
  umidadeMaxima = 0;

  // valores da última leitura (mais recente)
  lastHumidity: number | null = null;
  lastTimestampIso: string | null = null;
  lastRegando: boolean | null = null;
  lastRegaVolumeL: number | null = null;
  lastVolumeTotalL: number | null = null;
  lastRegaDuracaoS: number | null = null;
  lastEspIp: string | null = null;
  lastEspRssi: number | null = null;

  constructor(private readingService: ReadingService) { }

  async ngOnInit(): Promise<void> {
    await this.getReadingValues();
  }

  private async getReadingValues() {
    this.readingService.getAll().subscribe({
      next: (response) => {
        // normaliza e ordena as leituras por timestamp (mais antigo -> mais novo)
        this.readings = (response || []).slice().map(r => ({ ...r }));
        this.normalizeAndSortReadings();
        this.separarLeituras();
        this.calcularValoresDosCards();
        this.calcularUltimaLeitura();
      },
      error: (err) => {
        console.error('Erro ao buscar leituras:', err);
      }
    });
  }

  /** Converte timestamp de cada leitura para ms (number) e ordena ascendente */
  private normalizeAndSortReadings() {
    // acrescenta um campo temporário __tsMs para ordenação/uso interno (não modifica backend)
    this.readings.forEach(r => {
      const ms = this.timestampToMs(r.timestamp ?? (r as any).device_ts_ms ?? (r as any).createdAt);
      (r as any).__tsMs = Number.isFinite(ms) ? ms : 0; // fallback 0 para evitar NaN na ordenação
    });

    // ordenar pelo __tsMs (asc) — assim o último elemento será o mais recente
    this.readings.sort((a, b) => {
      const ta = Number((a as any).__tsMs || 0);
      const tb = Number((b as any).__tsMs || 0);
      return ta - tb;
    });
  }

  private separarLeituras() {
    // transforma readings em array de { humidity, timestampIso, tsMs }
    this.humidities = this.readings.map(r => {
      const rawTs = (r as any).timestamp ?? (r as any).device_ts_ms ?? (r as any).createdAt ?? null;
      let tsMs = this.timestampToMs(rawTs);

      // se houver um campo auxiliar __tsMs (cache) usa-o (opcional)
      if (typeof (r as any).__tsMs === 'number' && !isNaN((r as any).__tsMs)) {
        tsMs = (r as any).__tsMs;
      }

      // se timestamp inválido, já tentamos createdAt acima — manter tsMs como NaN se inválido
      const iso = this.msToIsoString(tsMs);

      return {
        humidity: typeof r.humidity === 'number' ? r.humidity : (Number(r.humidity) || 0),
        timestampIso: iso, // pode ser null
        tsMs: Number.isFinite(tsMs) ? tsMs : null, // pode ser null
      };
    });

    // também atualiza valores "última leitura" usados no template
    const last = this.readings.length ? this.readings[this.readings.length - 1] : null;
    if (last) {
      this.lastHumidity = typeof last.humidity === 'number' ? last.humidity : (last.humidity ? Number(last.humidity) : null);
      // timestamp preferencial: timestamp convertido -> ISO
      const lastTsMs = (last as any).__tsMs ?? this.timestampToMs(last.timestamp ?? (last as any).device_ts_ms ?? (last as any).createdAt);
      this.lastTimestampIso = this.msToIsoString(lastTsMs);
      this.lastRegando = typeof last.regando === 'boolean' ? last.regando : (last.regando === 1);
      this.lastRegaVolumeL = typeof last.rega_volume_l === 'number' ? last.rega_volume_l : (last.rega_volume_l ? Number(last.rega_volume_l) : null);
      this.lastVolumeTotalL = typeof last.volume_total_l === 'number' ? last.volume_total_l : (last.volume_total_l ? Number(last.volume_total_l) : null);
      this.lastRegaDuracaoS = typeof last.rega_duracao_s === 'number' ? last.rega_duracao_s : (last.rega_duracao_s ? Number(last.rega_duracao_s) : null);
      this.lastEspIp = (last.esp_ip ?? null) as string | null;
      this.lastEspRssi = typeof last.esp_rssi === 'number' ? last.esp_rssi : (last.esp_rssi ? Number(last.esp_rssi) : null);
    } else {
      this.lastHumidity = null;
      this.lastTimestampIso = null;
      this.lastRegando = null;
      this.lastRegaVolumeL = null;
      this.lastVolumeTotalL = null;
      this.lastRegaDuracaoS = null;
      this.lastEspIp = null;
      this.lastEspRssi = null;
    }
  }

  private calcularValoresDosCards() {
    const umidades = this.humidities.map(h => Number(h.humidity)).filter(v => !Number.isNaN(v));

    if (umidades.length === 0) {
      this.umidadeMedia = this.umidadeMinima = this.umidadeMaxima = 0;
      return;
    }

    this.umidadeMedia = this.media(umidades);
    this.umidadeMinima = Math.min(...umidades);
    this.umidadeMaxima = Math.max(...umidades);
  }

  private calcularUltimaLeitura() {
    if (!this.readings || this.readings.length === 0) {
      this.lastHumidity = null;
      this.lastTimestampIso = null;
      this.lastRegando = null;
      this.lastRegaVolumeL = null;
      this.lastVolumeTotalL = null;
      this.lastRegaDuracaoS = null;
      this.lastEspIp = null;
      this.lastEspRssi = null;
      return;
    }

    const last = this.readings[this.readings.length - 1];

    const tsMs = (last as any).__tsMs ?? this.timestampToMs(last.timestamp ?? (last as any).device_ts_ms ?? (last as any).createdAt);
    this.lastTimestampIso = this.msToIsoString(tsMs);
    this.lastHumidity = typeof last.humidity === 'number' ? last.humidity : (last.humidity ? Number(last.humidity) : null);

    this.lastRegando = typeof last.regando === 'boolean' ? last.regando : (last.regando === 1);
    this.lastRegaVolumeL = typeof last.rega_volume_l === 'number' ? last.rega_volume_l : (last.rega_volume_l ? Number(last.rega_volume_l) : null);
    this.lastVolumeTotalL = typeof last.volume_total_l === 'number' ? last.volume_total_l : (last.volume_total_l ? Number(last.volume_total_l) : null);
    this.lastRegaDuracaoS = typeof last.rega_duracao_s === 'number' ? last.rega_duracao_s : (last.rega_duracao_s ? Number(last.rega_duracao_s) : null);

    this.lastEspIp = last.esp_ip ?? null;
    this.lastEspRssi = typeof last.esp_rssi === 'number' ? last.esp_rssi : (last.esp_rssi ? Number(last.esp_rssi) : null);
  }

  // util
  private media(valores: number[]): number {
    if (!valores || valores.length === 0) return 0;
    const soma = valores.reduce((acc, v) => acc + v, 0);
    return parseFloat((soma / valores.length).toFixed(2));
  }

  /** Converte timestamp (string/number/Date) para epoch ms (number). Retorna NaN se inválido. */
  private timestampToMs(value: unknown): number {
    if (value === null || value === undefined) return NaN;

    // já número (epoch ms ou s)
    if (typeof value === 'number') {
      // se for muito pequeno, provavelmente seconds -> converte para ms
      if (value < 1e12) return Math.floor(value * 1000);
      return Math.floor(value);
    }

    // string contendo apenas dígitos -> epoch (s ou ms)
    if (typeof value === 'string') {
      const s = value.trim();
      if (/^\d+$/.test(s)) {
        const n = parseInt(s, 10);
        if (n < 1e12) return Math.floor(n * 1000); // segundos -> ms
        return n; // já ms
      }
      // tenta parse ISO
      const d = new Date(s);
      const ms = d.getTime();
      return isNaN(ms) ? NaN : ms;
    }

    // Date object
    if (value instanceof Date) {
      const ms = value.getTime();
      return isNaN(ms) ? NaN : ms;
    }

    return NaN;
  }

  /** converte epoch ms para ISO string legível (ou null se inválido) */
  private msToIsoString(ms: number | null | undefined): string | null {
    if (ms === null || ms === undefined) return null;
    if (!isFinite(ms) || isNaN(ms)) return null;
    const d = new Date(ms);
    if (isNaN(d.getTime())) return null;
    return d.toISOString();
  }
}
