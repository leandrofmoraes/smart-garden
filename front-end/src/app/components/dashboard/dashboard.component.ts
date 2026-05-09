import { Component, Input, OnInit, AfterViewInit, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Reading } from '../../models/reading.model';
import { Plant } from '../../models/plant.model';
import { ReadingService } from '../../services/reading-service.service';
import { PlantService } from '../../services/plant.service';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,
})
export class DashboardComponent implements OnInit, AfterViewInit {
  @ViewChild('trendCanvas') trendCanvas!: ElementRef<HTMLCanvasElement>;

  plant: Plant | null = null;
  plantLoading = true;
  plantError = false;

  readings: Reading[] = [];
  humidities: { humidity: number; timestampIso: string | null; tsMs: number | null }[] = [];

  umidadeMedia = 0;
  umidadeMinima = 0;
  umidadeMaxima = 0;

  lastHumidity: number | null = null;
  lastTimestampIso: string | null = null;
  lastSyncIso: string | null = null;   // horário em que os dados foram carregados
  lastRegaVolumeL: number | null = null;
  lastVolumeTotalL: number | null = null;
  lastRegaDuracaoS: number | null = null;

  // pontos para o mini-gráfico (últimas N leituras)
  trendPoints: { humidity: number; label: string }[] = [];
  readingsLoaded = false;

  @Input() plantId = '';

  constructor(
    private readingService: ReadingService,
    private plantService: PlantService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // plantId recebido via @Input do NavComponent
    // fallback: tenta ler da rota (quando dashboard é carregado diretamente)
    if (!this.plantId) {
      this.plantId = this.route.snapshot.paramMap.get('id') ?? '';
    }
    if (!this.plantId) { this.router.navigate(['/plants']); return; }
    this.loadPlant(this.plantId);
    this.loadReadings(this.plantId);
  }

  ngAfterViewInit(): void {
    // Se os dados chegaram antes da view estar pronta, desenha agora
    if (this.readingsLoaded && this.trendCanvas) {
      this.drawTrend();
    }
  }

  private loadPlant(id: string): void {
    this.plantService.getById(id).subscribe({
      next: (data) => { this.plant = data; this.plantLoading = false; },
      error: () => { this.plantError = true; this.plantLoading = false; }
    });
  }

  private loadReadings(plantId: string): void {
    this.readingService.getByPlant(plantId).subscribe({
      next: (response) => {
        this.lastSyncIso = new Date().toISOString(); // marca horário do fetch
        this.readings = (response || []).map(r => ({ ...r }));
        this.normalizeAndSort();
        this.buildHumidities();
        this.calcStats();
        this.calcLastReading();
        this.buildTrendPoints();
        this.readingsLoaded = true;
        this.cdr.detectChanges(); // garante que *ngIf renderiza o canvas
        setTimeout(() => this.drawTrend(), 0); // aguarda o canvas no DOM
      },
      error: (err) => console.error('Erro ao buscar leituras:', err)
    });
  }

  goBack(): void { this.router.navigate(['/plants']); }

  private normalizeAndSort(): void {
    this.readings.forEach(r => {
      const ms = this.toMs(r.timestamp ?? (r as any).device_ts_ms ?? (r as any).createdAt);
      (r as any).__tsMs = Number.isFinite(ms) ? ms : 0;
    });
    this.readings.sort((a, b) => ((a as any).__tsMs || 0) - ((b as any).__tsMs || 0));
  }

  private buildHumidities(): void {
    this.humidities = this.readings.map(r => {
      let ms = this.toMs((r as any).timestamp ?? (r as any).device_ts_ms ?? (r as any).createdAt);
      if (typeof (r as any).__tsMs === 'number') ms = (r as any).__tsMs;
      return {
        humidity: typeof r.humidity === 'number' ? r.humidity : (Number(r.humidity) || 0),
        timestampIso: this.msToIso(ms),
        tsMs: Number.isFinite(ms) ? ms : null,
      };
    });
  }

  private calcStats(): void {
    const vals = this.humidities.map(h => h.humidity).filter(v => isFinite(v));
    if (!vals.length) { this.umidadeMedia = this.umidadeMinima = this.umidadeMaxima = 0; return; }
    this.umidadeMedia = parseFloat((vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(2));
    this.umidadeMinima = Math.min(...vals);
    this.umidadeMaxima = Math.max(...vals);
  }

  private calcLastReading(): void {
    const last = this.readings.at(-1) ?? null;
    if (!last) return;
    const ms = (last as any).__tsMs ?? this.toMs(last.timestamp ?? (last as any).device_ts_ms ?? (last as any).createdAt);
    this.lastTimestampIso = this.msToIso(ms);
    this.lastHumidity = typeof last.humidity === 'number' ? last.humidity : (Number(last.humidity) || null);
    this.lastRegaVolumeL = last.rega_volume_l ?? null;
    this.lastVolumeTotalL = last.volume_total_l ?? null;
    this.lastRegaDuracaoS = last.rega_duracao_s ?? null;
  }

  /** Prepara os pontos para o mini-gráfico: usa todas as leituras (máx 50) */
  private buildTrendPoints(): void {
    const slice = this.humidities.slice(-50);
    this.trendPoints = slice.map(h => {
      const label = h.timestampIso
        ? new Date(h.timestampIso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
        : '';
      return { humidity: h.humidity, label };
    });
  }

  /** Desenha o mini-gráfico de tendência no canvas */
  drawTrend(): void {
    if (!this.trendCanvas?.nativeElement || !this.trendPoints.length) return;

    const canvas = this.trendCanvas.nativeElement;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // dimensões responsivas
    const W = canvas.offsetWidth || 400;
    const H = canvas.offsetHeight || 90;
    canvas.width  = W * window.devicePixelRatio;
    canvas.height = H * window.devicePixelRatio;
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio);

    ctx.clearRect(0, 0, W, H);

    const pts = this.trendPoints;
    const vals = pts.map(p => p.humidity);
    const minV = Math.max(0, Math.min(...vals) - 5);
    const maxV = Math.min(100, Math.max(...vals) + 5);
    const range = maxV - minV || 1;

    const padL = 6, padR = 6, padT = 8, padB = 20;
    const plotW = W - padL - padR;
    const plotH = H - padT - padB;

    const xOf = (i: number) => padL + (i / (pts.length - 1 || 1)) * plotW;
    const yOf = (v: number) => padT + (1 - (v - minV) / range) * plotH;

    // Faixa ideal de umidade (se disponível)
    if (this.plant?.care?.soilMoisture) {
      const yIdealTop = yOf(Math.min(this.plant.care.soilMoisture.max, maxV));
      const yIdealBot = yOf(Math.max(this.plant.care.soilMoisture.min, minV));
      ctx.fillStyle = 'rgba(0, 121, 107, 0.10)';
      ctx.fillRect(padL, yIdealTop, plotW, yIdealBot - yIdealTop);
    }

    // Linha de grade horizontal (meio)
    ctx.strokeStyle = 'rgba(0,0,0,0.06)';
    ctx.lineWidth = 1;
    ctx.setLineDash([3, 3]);
    const yMid = yOf((minV + maxV) / 2);
    ctx.beginPath(); ctx.moveTo(padL, yMid); ctx.lineTo(W - padR, yMid); ctx.stroke();
    ctx.setLineDash([]);

    // Área sob a linha (gradiente)
    const grad = ctx.createLinearGradient(0, padT, 0, padT + plotH);
    grad.addColorStop(0, 'rgba(51, 154, 240, 0.35)');
    grad.addColorStop(1, 'rgba(51, 154, 240, 0.02)');

    ctx.beginPath();
    ctx.moveTo(xOf(0), yOf(pts[0].humidity));
    for (let i = 1; i < pts.length; i++) {
      const x0 = xOf(i - 1), y0 = yOf(pts[i - 1].humidity);
      const x1 = xOf(i),     y1 = yOf(pts[i].humidity);
      const cpx = (x0 + x1) / 2;
      ctx.bezierCurveTo(cpx, y0, cpx, y1, x1, y1);
    }
    ctx.lineTo(xOf(pts.length - 1), H - padB);
    ctx.lineTo(xOf(0), H - padB);
    ctx.closePath();
    ctx.fillStyle = grad;
    ctx.fill();

    // Linha principal
    ctx.beginPath();
    ctx.moveTo(xOf(0), yOf(pts[0].humidity));
    for (let i = 1; i < pts.length; i++) {
      const x0 = xOf(i - 1), y0 = yOf(pts[i - 1].humidity);
      const x1 = xOf(i),     y1 = yOf(pts[i].humidity);
      const cpx = (x0 + x1) / 2;
      ctx.bezierCurveTo(cpx, y0, cpx, y1, x1, y1);
    }
    ctx.strokeStyle = '#339af0';
    ctx.lineWidth = 2;
    ctx.stroke();

    // Ponto final (última leitura)
    const lastX = xOf(pts.length - 1);
    const lastY = yOf(pts[pts.length - 1].humidity);
    ctx.beginPath();
    ctx.arc(lastX, lastY, 4, 0, Math.PI * 2);
    ctx.fillStyle = '#339af0';
    ctx.fill();
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 1.5;
    ctx.stroke();

    // Labels de horário (primeiro e último)
    ctx.fillStyle = '#999';
    ctx.font = `${10 * (window.devicePixelRatio > 1 ? 1 : 1)}px "Segoe UI", sans-serif`;
    ctx.textAlign = 'left';
    ctx.fillText(pts[0].label, padL, H - 4);
    ctx.textAlign = 'right';
    ctx.fillText(pts[pts.length - 1].label, W - padR, H - 4);

    // Label "Trend" no canto superior esquerdo
    ctx.fillStyle = '#aaa';
    ctx.font = '10px "Segoe UI", sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText('Trend', padL, padT + 2);
  }

  private toMs(value: unknown): number {
    if (value == null) return NaN;
    if (typeof value === 'number') return value < 1e12 ? value * 1000 : value;
    if (typeof value === 'string') {
      const s = value.trim();
      if (/^\d+$/.test(s)) { const n = +s; return n < 1e12 ? n * 1000 : n; }
      return new Date(s).getTime();
    }
    if (value instanceof Date) return value.getTime();
    return NaN;
  }

  private msToIso(ms: number): string | null {
    if (!ms || !isFinite(ms)) return null;
    const d = new Date(ms);
    return isNaN(d.getTime()) ? null : d.toISOString();
  }
}
