import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

import { TableComponent } from '../table/table.component';
import { GraphsComponent } from '../graphs/graphs.component';
import { DashboardComponent } from '../dashboard/dashboard.component';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [CommonModule, NgbNavModule, DashboardComponent, TableComponent, GraphsComponent],
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.scss']
})
export class NavComponent {
  active = 1;
}
