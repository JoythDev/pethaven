import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import {
  Appointment,
  AppointmentRequest,
  AppointmentRescheduleRequest,
  AppointmentResponse,
  AvailabilityDay,
} from '../models/appointment.model';

// Una fecha sin zona del API ('2026-11-24T09:30:00') se interpreta en hora local, que es la de la clínica.
export const toAppointment = ({ startTime, endTime, ...rest }: AppointmentResponse): Appointment => ({
  ...rest,
  start: new Date(startTime),
  end: new Date(endTime),
});

// Cliente del API de citas. Cada método devuelve un Observable frío:
// la petición HTTP solo se envía cuando alguien se suscribe.
@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_BASE_URL);

  // GET /appointments/availability: franjas libres de cada día de la ventana de reserva del dueño.
  getAvailability(): Observable<AvailabilityDay[]> {
    return this.http.get<AvailabilityDay[]>(`${this.apiUrl}/appointments/availability`);
  }

  // GET /owners/{id}/appointments: todas las citas del dueño, de la más antigua a la más reciente.
  getByOwnerId(ownerId: number): Observable<Appointment[]> {
    return this.http
      .get<AppointmentResponse[]>(`${this.apiUrl}/owners/${ownerId}/appointments`)
      .pipe(map((list) => list.map(toAppointment)));
  }

  // POST /owners/{id}/appointments: reserva una franja; el API asigna el veterinario.
  book(ownerId: number, request: AppointmentRequest): Observable<Appointment> {
    return this.http
      .post<AppointmentResponse>(`${this.apiUrl}/owners/${ownerId}/appointments`, request)
      .pipe(map(toAppointment));
  }

  // PUT /appointments/{id}/cancel: cancela una cita programada y libera su franja.
  cancel(id: number): Observable<Appointment> {
    return this.http.put<AppointmentResponse>(`${this.apiUrl}/appointments/${id}/cancel`, null).pipe(map(toAppointment));
  }

  // PUT /appointments/{id}/reschedule: mueve la misma cita a otra franja.
  reschedule(id: number, request: AppointmentRescheduleRequest): Observable<Appointment> {
    return this.http
      .put<AppointmentResponse>(`${this.apiUrl}/appointments/${id}/reschedule`, request)
      .pipe(map(toAppointment));
  }
}
