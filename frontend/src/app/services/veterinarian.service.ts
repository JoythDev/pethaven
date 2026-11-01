import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import { Treatment } from '../models/treatment.model';
import {
  Veterinarian,
  VeterinarianRequest,
  VeterinarianUpdateRequest,
} from '../models/veterinarian.model';

// Cliente del API de veterinarios. Cada método devuelve un Observable frío:
// la petición HTTP solo se envía cuando alguien se suscribe.
@Injectable({
  providedIn: 'root'
})
export class VeterinarianService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_BASE_URL);

  // GET /veterinarians: lista todos los veterinarios (activos e inactivos).
  getAll(): Observable<Veterinarian[]> {
    return this.http.get<Veterinarian[]>(`${this.apiUrl}/veterinarians`);
  }

  // GET /veterinarians/{id}: devuelve un veterinario; el API responde 404 si no existe.
  getById(id: number): Observable<Veterinarian> {
    return this.http.get<Veterinarian>(`${this.apiUrl}/veterinarians/${id}`);
  }

  // POST /veterinarians: registra un veterinario (el API lo crea activo). 409 si el documento o correo ya existen.
  create(request: VeterinarianRequest): Observable<Veterinarian> {
    return this.http.post<Veterinarian>(`${this.apiUrl}/veterinarians`, request);
  }

  // PUT /veterinarians/{id}: reemplaza sus datos; con password en null conserva la actual.
  update(id: number, request: VeterinarianUpdateRequest): Observable<Veterinarian> {
    return this.http.put<Veterinarian>(`${this.apiUrl}/veterinarians/${id}`, request);
  }

  // PUT /veterinarians/{id}/active?active=...: activa o desactiva (vacaciones, incapacidad...).
  // El API no permite borrar veterinarios: sus tratamientos se conservan como historial.
  setActive(id: number, active: boolean): Observable<Veterinarian> {
    const params = new HttpParams().set('active', active);
    return this.http.put<Veterinarian>(`${this.apiUrl}/veterinarians/${id}/active`, null, { params });
  }

  // GET /veterinarians/{id}/treatments: tratamientos registrados por un veterinario.
  getTreatmentsByVeterinarianId(id: number): Observable<Treatment[]> {
    return this.http.get<Treatment[]>(`${this.apiUrl}/veterinarians/${id}/treatments`);
  }
}
