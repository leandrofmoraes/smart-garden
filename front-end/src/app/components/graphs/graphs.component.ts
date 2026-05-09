import { Component, Input, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
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
export class GraphsComponent implements OnInit, AfterViewInit {
  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;

  public chart: any;
  readings: Reading[] = [];
  humidities: { humidity: number; tsMs: number | null; timestampIso: string | null }[] = [];

  sidebarAberto = false;
  dadosCarregados = false;

  filtros = {
    tipo: 'humidity',
    inicio: '',
    fim: '',
    min: null as number | null,
    max: null as number | null,
  };

  @Input() plantId = '';

  constructor(
    private readingService: ReadingService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.filtros.tipo = params['tipo'] || 'humidity';
      this.filtros.inicio = params['inicio'] || '';
      this.filtros.fim = params['fim'] || '';
      this.filtros.min = params['min'] ? +params['min'] : null;
      this.filtros.max = params['max'] ? +params['max'] : null;
    });

    // plantId recebido via @Input do NavComponent
    const plantId = this.plantId || this.route.parent?.snapshot.paramMap.get('id') || '';
    this.readingService.getByPlant(plantId).subscribe({
      next: (response) => {
        this.readings = response || [];
        this.normalizeReadings();
        this.objetosLeituras();
        this.dadosCarregados = true;
        // só cria o gráfico se o canvas já existir no DOM
        if (this.chartCanvas) {
          this.createChart(this.filtros.tipo);
        }
      },
      error: (err) => console.error('Erro ao obter leituras', err),
    });
  }

  ngAfterViewInit(): void {
    // se os dados já chegaram antes do canvas estar pronto, cria agora
    if (this.dadosCarregados && this.chartCanvas) {
      this.createChart(this.filtros.tipo);
    }
  }

  toggleSidebar() {
    this.sidebarAberto = !this.sidebarAberto;
  }

  onFiltroChange() {
    this.router.navigate([], {
      queryParams: {
        tipo: this.filtros.tipo,
        inicio: this.filtros.inicio,
        fim: this.filtros.fim,
        min: this.filtros.min,
        max: this.filtros.max,
      },
    });
    this.createChart(this.filtros.tipo);
  }

  setTipo(tipo: string) {
    this.filtros.tipo = tipo;
    this.onFiltroChange();
  }

  createChart(tipo: string) {
    if (!this.chartCanvas) return;

    let dados = this.humidities;
    let labelText = 'Umidade (%)';

    const inicioMs = this.filtros.inicio ? new Date(this.filtros.inicio).getTime() : null;
    const fimMs = this.filtros.fim ? new Date(this.filtros.fim).getTime() : null;

    const dadosFiltrados = dados.filter(d => {
      const ts = d.tsMs ?? null;
      const valor = d.humidity;
      if (inicioMs && ts !== null && ts < inicioMs) return false;
      if (fimMs && ts !== null && ts > fimMs) return false;
      if (this.filtros.min !== null && valor < (this.filtros.min as number)) return false;
      if (this.filtros.max !== null && valor > (this.filtros.max as number)) return false;
      if ((inicioMs || fimMs) && ts === null) return false;
      return true;
    });

    const labels = dadosFiltrados.map(d =>
      d.timestampIso
        ? new Date(d.timestampIso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
        : ''
    );
    const valores = dadosFiltrados.map(d => d.humidity);

    if (this.chart) {
      try { this.chart.destroy(); } catch (e) {}
      this.chart = null;
    }

    if (!labels.length) return;

    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: labelText,
          data: valores,
          backgroundColor: '#0d6efd33',
          borderColor: '#0d6efd',
          tension: 0.2,
          pointRadius: 4,
          fill: true,
        }]
      },
      options: {
        responsive: true,
        aspectRatio: 2.5,
        scales: {
          x: {
            ticks: { maxRotation: 45, minRotation: 0, autoSkip: true, maxTicksLimit: 12 }
          },
          y: { beginAtZero: true }
        },
        plugins: { legend: { display: true } }
      }
    });
  }

  private normalizeReadings() {
    this.readings.forEach(r => {
      const raw = (r as any).timestamp ?? (r as any).device_ts_ms ?? (r as any).createdAt ?? null;
      const ms = this.timestampToMs(raw);
      (r as any).__tsMs = Number.isFinite(ms) ? ms : 0;
    });
    this.readings.sort((a, b) => ((a as any).__tsMs || 0) - ((b as any).__tsMs || 0));
  }

  private objetosLeituras() {
    this.humidities = this.readings.map(r => {
      const source = (r as any).timestamp ?? (r as any).device_ts_ms ?? (r as any).createdAt ?? null;
      let tsMs = this.timestampToMs(source);
      if (typeof (r as any).__tsMs === 'number' && !isNaN((r as any).__tsMs)) tsMs = (r as any).__tsMs;
      return {
        humidity: typeof r.humidity === 'number' ? r.humidity : (Number(r.humidity) || 0),
        tsMs: Number.isFinite(tsMs) ? tsMs : null,
        timestampIso: this.msToIsoString(tsMs),
      };
    });
  }

  private timestampToMs(value: unknown): number {
    if (value === null || value === undefined) return NaN;
    if (typeof value === 'number') return value < 1e12 ? Math.floor(value * 1000) : Math.floor(value);
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
    if (value instanceof Date) return isNaN(value.getTime()) ? NaN : value.getTime();
    return NaN;
  }

  private msToIsoString(ms: number | null | undefined): string | null {
    if (ms == null || !isFinite(ms) || isNaN(ms)) return null;
    const d = new Date(ms);
    return isNaN(d.getTime()) ? null : d.toISOString();
  }
}
