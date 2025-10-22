import { Component, Directive, EventEmitter, Input, OnInit, Output, QueryList, ViewChildren } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { Reading } from '../../models/reading.model';
import { ReadingService } from '../../services/reading-service.service';

export type SortColumn = keyof Reading | '';
export type SortDirection = 'asc' | 'desc' | '';

const rotate: { [key: string]: SortDirection } = { asc: 'desc', desc: '', '': 'asc' };
const compare = (v1: string | number, v2: string | number) => (v1 < v2 ? -1 : v1 > v2 ? 1 : 0);

export interface SortEvent {
  column: SortColumn;
  direction: SortDirection;
}

@Directive({
  selector: 'th[sortable]',
  standalone: true,
  host: {
    '[class.asc]': 'direction === "asc"',
    '[class.desc]': 'direction === "desc"',
    '(click)': 'rotate()',
  },
})
export class NgbdSortableHeader {
  @Input() sortable: SortColumn = '';
  @Input() direction: SortDirection = '';
  @Output() sort = new EventEmitter<SortEvent>();

  rotate() {
    this.direction = rotate[this.direction];
    this.sort.emit({ column: this.sortable, direction: this.direction });
  }
}

@Component({
  selector: 'app-table',
  standalone: true,
  imports: [CommonModule, FormsModule, NgbPaginationModule, NgbdSortableHeader],
  templateUrl: './table.component.html',
  styleUrl: './table.component.scss',
})
export class TableComponent implements OnInit {
  readings: Reading[] = [];
  paginatedReadings: Reading[] = [];
  currentPage = 1;
  pageSize = 10;
  totalReadings = 0;
  loading = false;

  @ViewChildren(NgbdSortableHeader) headers!: QueryList<NgbdSortableHeader>;

  sidebarOpen = false;

  filters: any = {
    startDate: null,
    endDate: null,
    minHumidity: null,
    maxHumidity: null,
  };
  filteredReadings: Reading[] = [];

  constructor(private readingService: ReadingService) { }

  async ngOnInit() {
    this.loadReadings();
  }

  async loadReadings() {
    this.loading = true;
    try {
      this.readingService.getAll().subscribe({
        next: (response) => {
          this.readings = response || [];
          // inicializa filtros com dados
          this.filteredReadings = [...this.readings];
          this.totalReadings = this.filteredReadings.length;
          this.currentPage = 1;
          this.paginateReadings();
        },
        error: (error) => {
          console.error('Erro ao carregar dados:', error);
        },
        complete: () => {
          this.loading = false;
        },
      });
    } catch (error) {
      console.error('Erro inesperado ao carregar dados:', error);
      this.loading = false;
    }
  }

  applyFilters(): void {
    this.filteredReadings = this.readings.filter(reading => {
      const readingDate = this.parseReadingDate(reading); // Date | null
      const startDate = this.filters.startDate ? new Date(this.filters.startDate) : null;
      const endDate = this.filters.endDate ? new Date(this.filters.endDate) : null;

      const isWithinDateRange =
        (!startDate || (readingDate && readingDate >= startDate)) &&
        (!endDate || (readingDate && readingDate <= endDate));

      const humidity = typeof reading.humidity === 'number' ? reading.humidity : Number(reading.humidity);

      const minOk = (this.filters.minHumidity === null || this.filters.minHumidity === undefined) ||
        (humidity !== undefined && !isNaN(humidity) && humidity >= this.filters.minHumidity);
      const maxOk = (this.filters.maxHumidity === null || this.filters.maxHumidity === undefined) ||
        (humidity !== undefined && !isNaN(humidity) && humidity <= this.filters.maxHumidity);

      return !!isWithinDateRange && minOk && maxOk;
    });

    this.totalReadings = this.filteredReadings.length;
    this.currentPage = 1;
    this.paginateReadings();
  }

  paginateReadings(): void {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    this.paginatedReadings = this.filteredReadings.slice(startIndex, endIndex);
  }

  resetFilters(): void {
    this.filters = {
      startDate: null,
      endDate: null,
      minHumidity: null,
      maxHumidity: null,
    };
    this.filteredReadings = [...this.readings];
    this.totalReadings = this.filteredReadings.length;
    this.currentPage = 1;
    this.paginateReadings();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.paginateReadings();
  }

  onSort({ column, direction }: SortEvent) {
    this.headers.forEach((header) => {
      if (header.sortable !== column) {
        header.direction = '';
      }
    });

    if (!column || direction === '') {
      this.filteredReadings = [...this.filteredReadings];
    } else {
      this.filteredReadings = [...this.filteredReadings].sort((a, b) => {
        // obter valores seguros e compatíveis com compare
        let va: any = (a as any)[column];
        let vb: any = (b as any)[column];

        // tratar booleanos -> número
        if (typeof va === 'boolean') va = va ? 1 : 0;
        if (typeof vb === 'boolean') vb = vb ? 1 : 0;

        // tratar datas (se coluna for timestamp)
        if (column === 'timestamp') {
          const ta = this.parseReadingDate(a);
          const tb = this.parseReadingDate(b);
          va = ta ? ta.getTime() : 0;
          vb = tb ? tb.getTime() : 0;
        }

        // fallback para 0 se undefined/null
        if (va === null || va === undefined) va = 0;
        if (vb === null || vb === undefined) vb = 0;

        const res = compare(va as any, vb as any);
        return direction === 'asc' ? res : -res;
      });
    }

    this.paginateReadings();
  }

  /** tenta extrair um Date (ou null) a partir de vários campos possíveis da leitura */
  private parseReadingDate(reading: Reading): Date | null {
    const maybe = (reading as any).timestamp ?? (reading as any).device_ts_ms ?? (reading as any).createdAt ?? (reading as any).created_at;
    if (maybe === null || maybe === undefined) return null;

    // se number: já epoch ms ou segundos (tenta deduzir)
    if (typeof maybe === 'number') {
      const n = maybe;
      // se parece com segundos -> converte
      if (n < 1e12) return new Date(n * 1); // assume já ms (if it's seconds you may adjust multiply by 1000)
      return new Date(n);
    }

    if (typeof maybe === 'string') {
      const s = maybe.trim();
      if (/^\d+$/.test(s)) {
        const n = parseInt(s, 10);
        return new Date(n);
      }
      const d = new Date(s);
      return isNaN(d.getTime()) ? null : d;
    }

    if (maybe instanceof Date) {
      return isNaN(maybe.getTime()) ? null : maybe;
    }

    return null;
  }
}
