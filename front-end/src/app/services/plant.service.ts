import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Plant } from '../models/plant.model';
import { environment } from '../../environments/environment';

// Payload que o integration-service espera no POST /api/plants
export interface CreatePlantPayload {
  name: string;             // obrigatório
  scientificName?: string;  // opcional — usado para buscar na API externa
  imageUrl?: string;
  deviceKey?: string;       // identificador MQTT do device, ex: "esp-01"
}

@Injectable({
  providedIn: 'root'
})
export class PlantService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  public getAll() {
    return this.http.get<Plant[]>(`${this.apiUrl}/plants`);
  }

  public getById(id: string) {
    return this.http.get<Plant>(`${this.apiUrl}/plants/${id}`);
  }

  public create(payload: CreatePlantPayload) {
    return this.http.post<Plant>(`${this.apiUrl}/plants`, payload);
  }

  public update(id: string, payload: Partial<Plant>) {
    return this.http.put<Plant>(`${this.apiUrl}/plants/${id}`, payload);
  }

  public delete(id: string) {
    return this.http.delete<void>(`${this.apiUrl}/plants/${id}`);
  }
}
