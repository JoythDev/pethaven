import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import { Treatment } from '../models/treatment.model';

@Injectable({
  providedIn: 'root'
})
export class TreatmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_BASE_URL);

  // Obtiene el historial de tratamientos de una mascota.
  getByPetId(petId: number): Observable<Treatment[]> {
    return this.http.get<Treatment[]>(`${this.apiUrl}/pets/${petId}/treatments`);
  }
}
