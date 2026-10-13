import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Pet, PetRequest } from '../models/pet.model';

// Cliente del API de mascotas. Cada método devuelve un Observable frío:
// la petición HTTP solo se envía cuando alguien se suscribe.
@Injectable({
  providedIn: 'root'
})
export class PetService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_BASE_URL);

  // GET /pets: lista todas las mascotas (activas e inactivas).
  getAll(): Observable<Pet[]> {
    return this.http.get<Pet[]>(`${this.apiUrl}/pets`);
  }

  // GET /pets/{id}: devuelve una mascota; el API responde 404 si no existe.
  getById(id: number): Observable<Pet> {
    return this.http.get<Pet>(`${this.apiUrl}/pets/${id}`);
  }

  // GET /owners/{id}/pets: lista las mascotas registradas a nombre de un dueño.
  getByOwnerId(ownerId: number): Observable<Pet[]> {
    return this.http.get<Pet[]>(`${this.apiUrl}/owners/${ownerId}/pets`);
  }

  // POST /pets: registra una mascota nueva (el API la crea activa).
  create(request: PetRequest): Observable<Pet> {
    return this.http.post<Pet>(`${this.apiUrl}/pets`, request);
  }

  // PUT /pets/{id}: reemplaza los datos de la mascota y permite reasignar su dueño.
  update(id: number, request: PetRequest): Observable<Pet> {
    return this.http.put<Pet>(`${this.apiUrl}/pets/${id}`, request);
  }

  // PUT /pets/{id}/active?active=...: activa o desactiva la mascota.
  // El API no permite borrar mascotas directamente, solo cambiar este estado.
  setActive(id: number, active: boolean): Observable<Pet> {
    const params = new HttpParams().set('active', active);
    return this.http.put<Pet>(`${this.apiUrl}/pets/${id}/active`, null, { params });
  }
}
