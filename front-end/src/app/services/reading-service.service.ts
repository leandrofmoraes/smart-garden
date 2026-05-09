import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Reading } from '../models/reading.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ReadingService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** Busca todas as leituras de uma planta específica */
  public getByPlant(plantId: string) {
    return this.http.get<Reading[]>(`${this.apiUrl}/plants/${plantId}/readings`);
  }

  /** Busca todas as leituras de um dispositivo específico */
  public getByDevice(deviceId: string) {
    return this.http.get<Reading[]>(`${this.apiUrl}/devices/${deviceId}/readings`);
  }
}
