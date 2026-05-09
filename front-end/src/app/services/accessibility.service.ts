import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AccessibilityService {
  private readonly STORAGE_KEY = 'high-contrast';
  highContrast = false;

  constructor() {
    // persiste a preferência do usuário entre sessões
    this.highContrast = localStorage.getItem(this.STORAGE_KEY) === 'true';
    this.applyClass();
  }

  toggleHighContrast(): void {
    this.highContrast = !this.highContrast;
    localStorage.setItem(this.STORAGE_KEY, String(this.highContrast));
    this.applyClass();
  }

  private applyClass(): void {
    document.body.classList.toggle('high-contrast', this.highContrast);
  }
}
