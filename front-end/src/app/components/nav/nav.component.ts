import { Component } from '@angular/core';
import { TableComponent } from '../table/table.component';
import { GraphsComponent } from '../graphs/graphs.component';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { DashboardComponent } from "../dashboard/dashboard.component";

@Component({
  selector: 'app-nav',
  imports: [NgbNavModule, TableComponent, GraphsComponent, DashboardComponent],
  templateUrl: './nav.component.html',
  styleUrl: './nav.component.scss'
})
export class NavComponent {
  active = 1;
}
