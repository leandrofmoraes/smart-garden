import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import Chart from 'chart.js/auto';
import { ReadingService } from '../../services/reading-service.service';
import { Reading } from '../../models/reading.model';
import { FormsModule } from '@angular/forms';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-graphs',
  imports: [CommonModule, FormsModule, NgbDropdownModule],
  templateUrl: './graphs.component.html',
  styleUrls: ['./graphs.component.scss'],
  standalone: true,
})
export class GraphsComponent implements OnInit {
  public chart: any;
  readings: Reading[] = [];

  // array normalizado para gráficos: { humidity, tsMs, timestampIso }
  humidities: { humidity: number; tsMs: number | null; timestampIso: string | null }[] = [];

  sidebarAberto = false;

  filtros = {
    tipo: 'humidity', // default para o seu caso
    inicio: '',
    fim: '',
    min: null as number | null,
    max: null as number | null,
  };

  constructor(
    private readingService: ReadingService,
    private route: ActivatedRoute,
    private router: Router
  ) { }

  async ngOnInit(): Promise<void> {
    // lê query params e carrega dados
    this.route.queryParams.subscribe((params) => {
      this.filtros.tipo = params['tipo'] || 'humidity';
      this.filtros.inicio = params['inicio'] || '';
      this.filtros.fim = params['fim'] || '';
      this.filtros.min = params['min'] ? +params['min'] : null;
      this.filtros.max = params['max'] ? +params['max'] : null;
    });

    await this.getReadingValues();
  }

  toggleSidebar() {
    this.sidebarAberto = !this.sidebarAberto;
  }

  async getReadingValues() {
    this.readingService.getAll().subscribe((response) => {
      this.readings = response || [];
      this.normalizeReadings();
      this.objetosLeituras();
      this.createChart(this.filtros.tipo);
    }, (err) => {
      console.error('Erro ao obter leituras', err);
    });
  }

  /** Normaliza e adiciona campo __tsMs para ordenação */
  private normalizeReadings() {
    this.readings.forEach(r => {
      const raw = (r as any).timestamp ?? (r as any).device_ts_ms ?? (r as any).createdAt ?? null;
      const ms = this.timestampToMs(raw);
      (r as any).__tsMs = Number.isFinite(ms) ? ms : 0;
    });

    // ordenar ascendendo
    this.readings.sort((a, b) => ((a as any).__tsMs || 0) - ((b as any).__tsMs || 0));
  }

  objetosLeituras() {
    this.humidities = this.readings.map(r => {
      const source = (r as any).timestamp ?? (r as any).device_ts_ms ?? (r as any).createdAt ?? null;
      let tsMs = this.timestampToMs(source);
      if (typeof (r as any).__tsMs === 'number' && !isNaN((r as any).__tsMs)) tsMs = (r as any).__tsMs;
      const iso = this.msToIsoString(tsMs);
      return {
        humidity: typeof r.humidity === 'number' ? r.humidity : (r.humidity ? Number(r.humidity) : 0),
        tsMs: Number.isFinite(tsMs) ? tsMs : null,
        timestampIso: iso,
      };
    });
  }

  onFiltroChange() {
    // atualiza URL (query params) — útil para links/compartilhamento
    this.router.navigate([], {
      queryParams: {
        tipo: this.filtros.tipo,
        inicio: this.filtros.inicio,
        fim: this.filtros.fim,
        min: this.filtros.min,
        max: this.filtros.max,
      },
    });
    // recria gráfico
    this.createChart(this.filtros.tipo);
  }

  setTipo(tipo: string) {
    this.filtros.tipo = tipo;
    this.onFiltroChange();
  }

  createChart(tipo: string) {
    // selecionar dados (no momento só umidade)
    let dados = [];
    let labelText = '';
    switch (tipo) {
      case 'humidity':
      default:
        dados = this.humidities;
        labelText = 'Umidade (%)';
        break;
    }

    // Aplica filtros
    const inicioMs = this.filtros.inicio ? new Date(this.filtros.inicio).getTime() : null;
    const fimMs = this.filtros.fim ? new Date(this.filtros.fim).getTime() : null;

    const dadosFiltrados = dados.filter(d => {
      const ts = d.tsMs ?? null;
      const valor = d.humidity;
      if (inicioMs && ts !== null && ts < inicioMs) return false;
      if (fimMs && ts !== null && ts > fimMs) return false;
      if (this.filtros.min !== null && valor < (this.filtros.min as number)) return false;
      if (this.filtros.max !== null && valor > (this.filtros.max as number)) return false;
      // se timestamp faltando mas há filtro por período, descarta (não se sabe quando ocorreu)
      if ((inicioMs || fimMs) && ts === null) return false;
      return true;
    });

    // labels: use timestampIso (legível) ou fallback para tsMs
    const labels = dadosFiltrados.map(d => d.timestampIso ?? (d.tsMs ? new Date(d.tsMs).toISOString() : ''));
    const valores = dadosFiltrados.map(d => d.humidity);

    if (this.chart) {
      try { this.chart.destroy(); } catch (e) { /* ignore */ }
    }

    if (!labels.length) {
      // nada para mostrar: limpa chart e sai
      this.chart = null;
      return;
    }

    this.chart = new Chart("MyChart", {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: labelText,
          data: valores,
          backgroundColor: '#0d6efd33',
          borderColor: '#0d6efd',
          tension: 0.2,
          pointRadius: 3,
        }]
      },
      options: {
        aspectRatio: 2.5,
        scales: {
          x: {
            ticks: {
              maxRotation: 45,
              minRotation: 0,
              autoSkip: true,
              maxTicksLimit: 12
            }
          },
          y: { beginAtZero: true }
        },
        plugins: {
          legend: { display: true }
        }
      }
    });
  }

  /** Converte timestamps (ISO string / digits / number / Date) para epoch ms */
  private timestampToMs(value: unknown): number {
    if (value === null || value === undefined) return NaN;

    if (typeof value === 'number') {
      // se for muito pequeno, provavelmente segundos -> converte para ms
      if (value < 1e12) return Math.floor(value * 1000);
      return Math.floor(value);
    }

    if (typeof value === 'string') {
      const s = value.trim();
      if (!s) return NaN;
      if (/^\d+$/.test(s)) {
        const n = parseInt(s, 10);
        if (n < 1e12) return Math.floor(n * 1000); // segundos -> ms
        return n; // já ms
      }
      const d = new Date(s);
      const ms = d.getTime();
      return isNaN(ms) ? NaN : ms;
    }

    if (value instanceof Date) {
      const ms = value.getTime();
      return isNaN(ms) ? NaN : ms;
    }

    return NaN;
  }

  private msToIsoString(ms: number | null | undefined): string | null {
    if (ms === null || ms === undefined) return null;
    if (!isFinite(ms) || isNaN(ms)) return null;
    const d = new Date(ms);
    if (isNaN(d.getTime())) return null;
    return d.toISOString();
  }
}
