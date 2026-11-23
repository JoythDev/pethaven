import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import { Clinic } from '../models/clinic.model';

// Datos públicos de la sede. No cambian mientras la aplicación está abierta, así que se piden una sola vez
// (un error no se guarda: el siguiente suscriptor vuelve a intentarlo).
@Injectable({
  providedIn: 'root'
})
export class ClinicService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_BASE_URL);

  private readonly clinic$ = this.http.get<Clinic>(`${this.apiUrl}/clinic`).pipe(shareReplay({ bufferSize: 1, refCount: false }));

  // GET /clinic: nombre, dirección, teléfono y horario de atención.
  getClinic(): Observable<Clinic> {
    return this.clinic$;
  }
}
