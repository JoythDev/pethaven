import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import { Treatment, TreatmentRequest } from '../models/treatment.model';

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

  // Registra un tratamiento con sus medicamentos. El API responde 409 si la mascota o el
  // veterinario están inactivos, si no hay medicamentos disponibles o si falta stock.
  create(request: TreatmentRequest): Observable<Treatment> {
    return this.http.post<Treatment>(`${this.apiUrl}/treatments`, request);
  }
}
