import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { RouterModule, ActivatedRoute } from '@angular/router';

import { TableComponent } from '../table/table.component';
import { GraphsComponent } from '../graphs/graphs.component';
import { DashboardComponent } from '../dashboard/dashboard.component';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [CommonModule, NgbNavModule, RouterModule, DashboardComponent, TableComponent, GraphsComponent],
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.scss']
})
export class NavComponent implements OnInit {
  active = 1;
  plantId = '';

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.plantId = this.route.snapshot.paramMap.get('id') ?? '';
  }
}
