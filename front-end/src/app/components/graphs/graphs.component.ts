import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import Chart from 'chart.js/auto';
import { ReadingService } from '../../services/reading-service.service';
import { Reading } from '../../models/reading.model';
import { FormsModule } from '@angular/forms';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-graphs',
  imports: [FormsModule, NgbDropdownModule],
  templateUrl: './graphs.component.html',
  styleUrl: './graphs.component.scss',
  standalone: true,
})
export class GraphsComponent {
  public chart: any;
  readings: Reading[] = [];
  //temperatures: any[] = [];
  humidities: any[] = [];

  sidebarAberto = false;

  filtros = {
    tipo: 'temperature',
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
    await this.getReadingValues();
    this.route.queryParams.subscribe((params) => {
      this.filtros.tipo = params['tipo'] || 'temperature';
      this.filtros.inicio = params['inicio'] || '';
      this.filtros.fim = params['fim'] || '';
      this.filtros.min = params['min'] ? +params['min'] : null;
      this.filtros.max = params['max'] ? +params['max'] : null;
      this.createChart(this.filtros.tipo);
    });
  }

  toggleSidebar() {
    this.sidebarAberto = !this.sidebarAberto;
  }

  async getReadingValues() {
    this.readingService.getAll().subscribe((response) => {
      this.readings = response;
      this.objetosLeituras();
    });
  }

  objetosLeituras() {
    //this.temperatures = this.readings.map((r) => ({ temperature: r.temperature, timestamp: r.timestamp }));
    this.humidities = this.readings.map((r) => ({ humidity: r.humidity, timestamp: r.timestamp }));
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
  }

  setTipo(tipo: string) {
    this.filtros.tipo = tipo;
    this.onFiltroChange();
  }

  createChart(tipo: string) {
    let dadosFiltrados: any[] = [];
    let labels: string[] = [];
    let valores: number[] = [];
    let labelText = '';

    switch (tipo) {
      //case 'temperature':
      //  dadosFiltrados = this.temperatures;
      //  labelText = 'Temperatura';
      //  break;
      case 'humidity':
        dadosFiltrados = this.humidities;
        labelText = 'Umidade';
        break;
    }

    dadosFiltrados = dadosFiltrados.filter((d) => {
      const valor = d[tipo];
      const data = new Date(d.timestamp);
      const inicio = this.filtros.inicio ? new Date(this.filtros.inicio) : null;
      const fim = this.filtros.fim ? new Date(this.filtros.fim) : null;
      return (
        (!inicio || data >= inicio) &&
        (!fim || data <= fim) &&
        (this.filtros.min === null || valor >= this.filtros.min) &&
        (this.filtros.max === null || valor <= this.filtros.max)
      );
    });

    labels = dadosFiltrados.map((d) => d.timestamp);
    valores = dadosFiltrados.map((d) => d[tipo]);

    if (this.chart) this.chart.destroy();

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
        }]
      },
      options: {
        aspectRatio: 2.5,
        scales: {
          y: { beginAtZero: true }
        }
      }
    });
  }
}
