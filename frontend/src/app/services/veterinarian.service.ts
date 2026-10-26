import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import { Treatment } from '../models/treatment.model';
import { Veterinarian } from '../models/veterinarian.model';

// Cliente del API de veterinarios. Cada método devuelve un Observable frío:
// la petición HTTP solo se envía cuando alguien se suscribe.
@Injectable({
  providedIn: 'root'
})
export class VeterinarianService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_BASE_URL);

  // GET /veterinarians/{id}: devuelve un veterinario; el API responde 404 si no existe.
  getById(id: number): Observable<Veterinarian> {
    return this.http.get<Veterinarian>(`${this.apiUrl}/veterinarians/${id}`);
  }

  // GET /veterinarians/{id}/treatments: tratamientos registrados por un veterinario.
  getTreatmentsByVeterinarianId(id: number): Observable<Treatment[]> {
    return this.http.get<Treatment[]>(`${this.apiUrl}/veterinarians/${id}/treatments`);
  }
}
