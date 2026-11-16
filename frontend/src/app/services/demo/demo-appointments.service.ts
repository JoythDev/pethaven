import { Injectable, inject, signal } from '@angular/core';
import { Observable, defer, forkJoin, map, of, switchMap, tap, throwError } from 'rxjs';
import {
  appointmentsForVet,
  availableSlots,
  bookingToAppointment,
  buildDayAgenda,
  countsByDay,
  mergeBooked,
  ownerAppointments,
  vetsFromTreatments,
} from '../../core/demo/appointments';
import { addDays, dayNumber, startOfDay } from '../../core/demo/demo-date';
import {
  Appointment,
  AvailableSlot,
  BookingRequest,
  DayAgenda,
  DayCount,
  OwnerAppointments,
  VetRef,
} from '../../models/demo/appointment.model';
import { OwnerContext } from '../../models/demo/owner-context.model';
import { Pet } from '../../models/pet.model';
import { ClienteTreatmentService } from '../treatment-cliente.service';
import { PetService } from '../pet.service';
import { VeterinarianService } from '../veterinarian.service';
import { DEMO_NOW, demoLatency } from './demo-latency';

// Demo (S3): las citas no existen en el API. Se generan de forma estable a partir de mascotas,
// dueños y veterinarios reales; lo agendado en la sesión vive solo en memoria.
//
// Limitación del API: un dueño no puede listar mascotas ni veterinarios, así que sus citas se
// calculan con sus propias mascotas y con los veterinarios de sus tratamientos.
@Injectable({ providedIn: 'root' })
export class DemoAppointmentsService {
  private readonly petService = inject(PetService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly treatmentService = inject(ClienteTreatmentService);
  private readonly now = inject(DEMO_NOW);

  private readonly bookedState = signal<Appointment[]>([]);
  // Citas agendadas en esta sesión.
  readonly booked = this.bookedState.asReadonly();

  // Agenda del veterinario en un día (usa su perfil y todas las mascotas: portal veterinario).
  getDayAgenda(vetId: number, date: Date): Observable<DayAgenda> {
    return this.loadVetData(vetId).pipe(
      map(({ vet, pets }) => this.agendaFor(vet, pets, date)),
      demoLatency(),
    );
  }

  // Citas por día de las siete jornadas de la semana (lunes primero) que contiene la fecha.
  getWeekOverview(vetId: number, date: Date): Observable<DayCount[]> {
    const monday = addDays(startOfDay(date), -((date.getDay() + 6) % 7));
    return this.getOverview(vetId, monday, 7);
  }

  // Citas por día de todos los días del mes de la fecha (puntos del calendario).
  getMonthOverview(vetId: number, date: Date): Observable<DayCount[]> {
    const first = new Date(date.getFullYear(), date.getMonth(), 1);
    const days = new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
    return this.getOverview(vetId, first, days);
  }

  // Próximas y pasadas de las mascotas del dueño; mostrar solo las primeras de cada lista.
  getOwnerAppointments(ownerId: number): Observable<OwnerAppointments> {
    return this.loadOwnerContext(ownerId).pipe(
      map(({ pets, vets }) => this.ownerAppointmentsFrom(pets, vets)),
      demoLatency(),
    );
  }

  // Veterinarios que el dueño puede elegir: los que ya atendieron a sus mascotas.
  getBookableVeterinarians(ownerId: number): Observable<VetRef[]> {
    return this.loadOwnerContext(ownerId).pipe(
      map(({ vets }) => vets),
      demoLatency(),
    );
  }

  getAvailableSlots(ownerId: number, vet: VetRef, date: Date): Observable<AvailableSlot[]> {
    return this.loadOwnerContext(ownerId).pipe(
      map(({ pets }) => availableSlots(vet, pets, date, this.now(), this.bookedState())),
      demoLatency(),
    );
  }

  // Agenda una cita en memoria; falla si la franja ya fue agendada en esta sesión.
  book(request: BookingRequest): Observable<Appointment> {
    return defer(() => {
      const appointment = bookingToAppointment(request);
      if (this.bookedState().some((a) => a.id === appointment.id)) {
        return throwError(() => new Error('La franja ya está ocupada'));
      }
      return of(appointment).pipe(
        demoLatency(),
        tap((created) => this.bookedState.update((list) => [...list, created])),
      );
    });
  }

  // Agenda de un veterinario ya conocido, sin pedir nada al API ni simular espera.
  agendaFor(vet: VetRef, pets: Pet[], date: Date): DayAgenda {
    return buildDayAgenda(date, this.vetAppointments(vet, pets, date), this.now());
  }

  ownerAppointmentsFrom(pets: Pet[], vets: VetRef[]): OwnerAppointments {
    return ownerAppointments(vets, pets, this.now(), this.bookedState());
  }

  // Mascotas, tratamientos y veterinarios conocidos de un dueño, sin simular espera.
  loadOwnerContext(ownerId: number): Observable<OwnerContext> {
    return this.petService.getByOwnerId(ownerId).pipe(
      switchMap((pets) =>
        (pets.length
          ? forkJoin(pets.map((pet) => this.treatmentService.getByPetId(pet.id)))
          : of([])
        ).pipe(
          map((lists) => {
            const petTreatments = pets.map((pet, i) => ({ pet, treatments: lists[i] }));
            return { pets, petTreatments, vets: vetsFromTreatments(lists.flat()) };
          }),
        ),
      ),
    );
  }

  private loadVetData(vetId: number): Observable<{ vet: VetRef; pets: Pet[] }> {
    return forkJoin({ vet: this.veterinarianService.getById(vetId), pets: this.petService.getAll() });
  }

  private vetAppointments(vet: VetRef, pets: Pet[], date: Date): Appointment[] {
    const booked = this.bookedState().filter((a) => a.veterinarianId === vet.id);
    return mergeBooked(appointmentsForVet(vet, pets, date, this.now()), booked, this.now());
  }

  private getOverview(vetId: number, from: Date, days: number): Observable<DayCount[]> {
    return this.loadVetData(vetId).pipe(
      map(({ vet, pets }) => {
        const appointments = Array.from({ length: days }, (_, i) => addDays(from, i)).flatMap((date) =>
          this.vetAppointments(vet, pets, date),
        );
        return countsByDay(appointments, from, days);
      }),
      demoLatency(),
    );
  }
}
