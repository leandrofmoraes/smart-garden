import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Reading } from '../models/reading.model';



@Injectable({
  providedIn: 'root'
})
export class ReadingService {
  private baseUrl = "http://localhost:3000"
  private apiRoute = "/reading"

  constructor(private http: HttpClient) { }

  public getAll() {
    return this.http.get<Reading[]>(this.baseUrl + this.apiRoute)
  }

  public getById(id: number) {
    return this.http.get<Reading>(`${this.baseUrl}/${this.apiRoute}/${id}`)
  }

}
