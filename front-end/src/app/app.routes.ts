import { Routes } from '@angular/router';
import { PlantListComponent } from './components/plant-list/plant-list.component';
import { NavComponent } from './components/nav/nav.component';

export const routes: Routes = [
  { path: '', redirectTo: 'plants', pathMatch: 'full' },
  { path: 'plants', component: PlantListComponent },
  { path: 'plants/:id', component: NavComponent },
];
