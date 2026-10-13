import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Owner, OwnerRequest, OwnerUpdateRequest } from '../models/owner.model';

// Cliente del API de dueños. Cada método devuelve un Observable frío:
// la petición HTTP solo se envía cuando alguien se suscribe.
@Injectable({
  providedIn: 'root'
})
export class OwnerService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_BASE_URL);

  // GET /owners: lista todos los dueños (sin contraseña).
  getAll(): Observable<Owner[]> {
    return this.http.get<Owner[]>(`${this.apiUrl}/owners`);
  }

  // GET /owners/{id}: devuelve un dueño; el API responde 404 si no existe.
  getById(id: number): Observable<Owner> {
    return this.http.get<Owner>(`${this.apiUrl}/owners/${id}`);
  }

  // POST /owners: registra un dueño nuevo; el API responde 409 si el documento o el correo ya existen.
  create(request: OwnerRequest): Observable<Owner> {
    return this.http.post<Owner>(`${this.apiUrl}/owners`, request);
  }

  // PUT /owners/{id}: reemplaza los datos del dueño; con password en null conserva la actual.
  update(id: number, request: OwnerUpdateRequest): Observable<Owner> {
    return this.http.put<Owner>(`${this.apiUrl}/owners/${id}`, request);
  }

  // DELETE /owners/{id}: elimina al dueño y sus mascotas en cascada;
  // el API responde 409 si alguna de sus mascotas tiene tratamientos registrados.
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/owners/${id}`);
  }
}
