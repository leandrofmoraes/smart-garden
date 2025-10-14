import { Component } from '@angular/core';
import { NavComponent } from './components/nav/nav.component';
import { HttpClientModule } from '@angular/common/http';
import { ReadingService } from './services/reading-service.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [NavComponent, HttpClientModule, CommonModule],
  providers: [ReadingService],
  standalone: true,
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'jardim-inteligente';
}
