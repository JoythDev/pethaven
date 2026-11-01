import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import { Drug } from '../models/drug.model';

// Cliente del API de medicamentos. Cada método devuelve un Observable frío:
// la petición HTTP solo se envía cuando alguien se suscribe.
@Injectable({
  providedIn: 'root'
})
export class DrugService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_BASE_URL);

  // GET /drugs?available=true: lista solo los medicamentos con unidades en inventario.
  getAvailable(): Observable<Drug[]> {
    const params = new HttpParams().set('available', true);
    return this.http.get<Drug[]>(`${this.apiUrl}/drugs`, { params });
  }
}
