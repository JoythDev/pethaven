import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import { Drug, DrugImportSummary, DrugRequest } from '../models/drug.model';

// Cliente del API de medicamentos. Cada método devuelve un Observable frío:
// la petición HTTP solo se envía cuando alguien se suscribe.
@Injectable({
  providedIn: 'root'
})
export class DrugService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_BASE_URL);

  // GET /drugs: lista todos los medicamentos registrados.
  getAll(): Observable<Drug[]> {
    return this.http.get<Drug[]>(`${this.apiUrl}/drugs`);
  }

  // GET /drugs?available=true: lista solo los medicamentos con unidades en inventario.
  getAvailable(): Observable<Drug[]> {
    const params = new HttpParams().set('available', true);
    return this.http.get<Drug[]>(`${this.apiUrl}/drugs`, { params });
  }

  // GET /drugs/{id}: detalle de un medicamento.
  getById(id: number): Observable<Drug> {
    return this.http.get<Drug>(`${this.apiUrl}/drugs/${id}`);
  }

  // POST /drugs: registra un medicamento (las unidades vendidas empiezan en 0).
  create(request: DrugRequest): Observable<Drug> {
    return this.http.post<Drug>(`${this.apiUrl}/drugs`, request);
  }

  // PUT /drugs/{id}: reemplaza los datos del medicamento (las unidades vendidas no cambian).
  update(id: number, request: DrugRequest): Observable<Drug> {
    return this.http.put<Drug>(`${this.apiUrl}/drugs/${id}`, request);
  }

  // DELETE /drugs/{id}: el API responde 409 si el medicamento tiene tratamientos asociados.
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/drugs/${id}`);
  }

  // POST /drugs/import: carga masiva desde Excel (multipart, campo "file"). Es todo o nada.
  importExcel(file: File): Observable<DrugImportSummary> {
    const body = new FormData();
    body.append('file', file, file.name);
    return this.http.post<DrugImportSummary>(`${this.apiUrl}/drugs/import`, body);
  }
}
