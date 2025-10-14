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
  loading = false; // Adiciona um indicador de loading

  @ViewChildren(NgbdSortableHeader) headers!: QueryList<NgbdSortableHeader>;

  sidebarOpen = false;

  filters = {
    startDate: null,
    endDate: null,
    //minTemperature: null,
    //maxTemperature: null,
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
          this.readings = response;
          this.filteredReadings = [...this.readings]; // inicia sem filtro
          this.totalReadings = this.filteredReadings.length;
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
      const readingDate = new Date(reading.timestamp);
      const startDate = this.filters.startDate ? new Date(this.filters.startDate) : null;
      const endDate = this.filters.endDate ? new Date(this.filters.endDate) : null;

      const isWithinDateRange =
        (!startDate || readingDate >= startDate) &&
        (!endDate || readingDate <= endDate);

      // const isWithinTemperatureRange =
      //   (this.filters.minTemperature === null || reading.temperature >= this.filters.minTemperature) &&
      //   (this.filters.maxTemperature === null || reading.temperature <= this.filters.maxTemperature);

      return isWithinDateRange; //&& isWithinTemperatureRange;
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
      //minTemperature: null,
      //maxTemperature: null,
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

    if (direction === '' || column === '') {
      this.filteredReadings = [...this.filteredReadings];
    } else {
      this.filteredReadings = [...this.filteredReadings].sort((a, b) => {
        const res = compare(a[column], b[column]);
        return direction === 'asc' ? res : -res;
      });
    }

    this.paginateReadings();
  }
}
