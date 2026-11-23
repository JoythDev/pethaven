import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import {
  Appointment,
  AppointmentCompletionRequest,
  AppointmentRequest,
  AppointmentRescheduleRequest,
  AppointmentResponse,
  AvailabilityDay,
  VeterinarianAppointmentRequest,
} from '../models/appointment.model';
import { PetRequest } from '../models/pet.model';

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

  // GET /appointments/{id}: una cita del dueño o del veterinario asignado.
  getById(id: number): Observable<Appointment> {
    return this.http.get<AppointmentResponse>(`${this.apiUrl}/appointments/${id}`).pipe(map(toAppointment));
  }

  // GET /veterinarians/{id}/appointments?from&to: agenda del veterinario en cualquier estado ('YYYY-MM-DD',
  // ambas incluidas, máximo 31 días de diferencia).
  getVeterinarianAgenda(veterinarianId: number, from: string, to: string): Observable<Appointment[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http
      .get<AppointmentResponse[]>(`${this.apiUrl}/veterinarians/${veterinarianId}/appointments`, { params })
      .pipe(map((list) => list.map(toAppointment)));
  }

  // GET /veterinarians/{id}/availability?from&to: franjas futuras que el veterinario tiene libres.
  getVeterinarianAvailability(veterinarianId: number, from: string, to: string): Observable<AvailabilityDay[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<AvailabilityDay[]>(`${this.apiUrl}/veterinarians/${veterinarianId}/availability`, { params });
  }

  // POST /veterinarians/{id}/appointments: alta y entrega o reunión informativa de una mascota hospitalizada.
  bookForVeterinarian(veterinarianId: number, request: VeterinarianAppointmentRequest): Observable<Appointment> {
    return this.http
      .post<AppointmentResponse>(`${this.apiUrl}/veterinarians/${veterinarianId}/appointments`, request)
      .pipe(map(toAppointment));
  }

  // PUT /appointments/{id}/start: inicia la consulta (solo el día de la cita).
  start(id: number): Observable<Appointment> {
    return this.http.put<AppointmentResponse>(`${this.apiUrl}/appointments/${id}/start`, null).pipe(map(toAppointment));
  }

  // PUT /appointments/{id}/no-show: el dueño no llegó (solo cuando ya pasó la hora).
  noShow(id: number): Observable<Appointment> {
    return this.http.put<AppointmentResponse>(`${this.apiUrl}/appointments/${id}/no-show`, null).pipe(map(toAppointment));
  }

  // PUT /appointments/{id}/complete: cierra una cita en curso según su motivo.
  complete(id: number, request: AppointmentCompletionRequest): Observable<Appointment> {
    return this.http
      .put<AppointmentResponse>(`${this.apiUrl}/appointments/${id}/complete`, request)
      .pipe(map(toAppointment));
  }

  // POST /appointments/{id}/pet: registra la mascota provisional de una cita en curso y la enlaza.
  registerPet(id: number, request: PetRequest): Observable<Appointment> {
    return this.http.post<AppointmentResponse>(`${this.apiUrl}/appointments/${id}/pet`, request).pipe(map(toAppointment));
  }
}
