import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import { ClienteTreatment } from '../models/treatment-cliente.model';

// Cliente del API de tratamientos para el área del cliente. Cada método devuelve
// un Observable frío: la petición HTTP solo se envía cuando alguien se suscribe.
// El nombre lleva el sufijo Cliente para no colisionar con el servicio que use
// el área veterinaria.
@Injectable({
  providedIn: 'root'
})
export class ClienteTreatmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_BASE_URL);

  // GET /pets/{petId}/treatments: historial de tratamientos de una mascota.
  getByPetId(petId: number): Observable<ClienteTreatment[]> {
    return this.http.get<ClienteTreatment[]>(`${this.apiUrl}/pets/${petId}/treatments`);
  }
}
