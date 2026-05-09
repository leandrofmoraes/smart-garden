import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { IotDevice } from '../models/iot-device.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class IotDeviceService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  public getAll() {
    return this.http.get<IotDevice[]>(`${this.apiUrl}/devices`);
  }

  public getById(deviceId: string) {
    return this.http.get<IotDevice>(`${this.apiUrl}/devices/${deviceId}`);
  }

  public getStatus(deviceId: string) {
    return this.http.get<any>(`${this.apiUrl}/devices/${deviceId}/status`);
  }

  public sendCommand(deviceId: string, command: any) {
    return this.http.post<any>(`${this.apiUrl}/devices/${deviceId}/commands`, command);
  }
}
