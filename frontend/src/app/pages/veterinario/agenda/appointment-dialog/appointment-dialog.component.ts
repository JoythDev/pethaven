import { Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { BehaviorSubject, Observable, combineLatest, finalize, map, of, switchMap } from 'rxjs';

import { BookableDay, apiDateTime, bookableDays, dayLabel } from '../../../../core/client-appointments/client-appointments';
import { getErrorMessage } from '../../../../core/http/http-error';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { formatTime } from '../../../../core/time/calendar';
import { CLOCK } from '../../../../core/time/clock';
import { skeletonVisible } from '../../../../core/ui/skeleton-timing';
import { VET_REASON_OPTIONS, pickerRange, reasonLabel } from '../../../../core/vet-appointments/vet-appointments';
import { Appointment, VeterinarianReason } from '../../../../models/appointment.model';
import { Clinic } from '../../../../models/clinic.model';
import { Pet, isHospitalized } from '../../../../models/pet.model';
import { AppointmentService } from '../../../../services/appointment.service';
import { PetService } from '../../../../services/pet.service';
import { FormFieldComponent } from '../../../shared/form-field/form-field.component';
import { InlineErrorComponent } from '../../../shared/inline-error/inline-error.component';
import { SkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { SlotPickerComponent } from '../slot-picker/slot-picker.component';

interface PetOption {
  label: string;
  value: number;
}

/**
 * Diálogo de la Agenda (10.20) que elige una franja: «Nueva cita» (alta y entrega o reunión informativa
 * con el dueño de una mascota hospitalizada, S16.1) o «Reprogramar» una cita programada (S16.8). Las
 * franjas son las que el veterinario tiene libres. Un fallo del API queda como alerta dentro del diálogo.
 * Se crea al abrirlo y se destruye al cerrar; `saved` avisa para que la Agenda recargue.
 */
@Component({
  selector: 'app-appointment-dialog',
  imports: [
    FormsModule,
    ButtonModule,
    DialogModule,
    MessageModule,
    SelectModule,
    SelectButtonModule,
    LucideCircleAlert,
    FormFieldComponent,
    InlineErrorComponent,
    SkeletonComponent,
    SlotPickerComponent,
  ],
  templateUrl: './appointment-dialog.component.html',
})
export class AppointmentDialogComponent {
  private readonly appointmentService = inject(AppointmentService);
  private readonly petService = inject(PetService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly clock = inject(CLOCK);

  readonly veterinarianId = input.required<number>();
  readonly clinic = input.required<Clinic>();
  /** Con una cita, la reprograma; sin ella, agenda una nueva. */
  readonly appointment = input<Appointment | null>(null);
  /** Franja elegida al abrir (una franja libre de la línea de tiempo). */
  readonly presetDay = input<string | null>(null);
  readonly presetTime = input<string | null>(null);

  readonly saved = output<Appointment>();
  readonly closed = output<void>();

  protected readonly reasonOptions = [...VET_REASON_OPTIONS];

  protected readonly rescheduling = computed(() => this.appointment() !== null);
  protected readonly title = computed(() => {
    const appointment = this.appointment();
    return appointment ? `Reprogramar la cita de ${appointment.petName}` : 'Nueva cita';
  });
  protected readonly movingText = computed(() => {
    const appointment = this.appointment();
    return appointment
      ? `${reasonLabel(appointment)} del ${dayLabel(appointment.start).toLowerCase()} a las ${formatTime(appointment.start)}. Elige la nueva franja: la cita conserva su motivo y sus notas.`
      : null;
  });

  // Lo elegido; undefined mientras no se toca, para partir de la franja inicial.
  private readonly pickedDay = signal<string | null | undefined>(undefined);
  private readonly pickedTime = signal<string | null | undefined>(undefined);
  protected readonly day = computed(() => {
    const picked = this.pickedDay();
    return picked === undefined ? this.presetDay() : picked;
  });
  protected readonly time = computed(() => {
    const picked = this.pickedTime();
    return picked === undefined ? this.presetTime() : picked;
  });
  protected readonly reason = signal<VeterinarianReason | null>(null);
  protected readonly petId = signal<number | null>(null);
  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);

  private readonly availabilityRetry = new BehaviorSubject<void>(undefined);
  private readonly petsRetry = new BehaviorSubject<void>(undefined);

  protected readonly availability = toSignal(
    combineLatest([toObservable(this.veterinarianId), this.availabilityRetry]).pipe(
      switchMap(([id]) => {
        const { from, to } = pickerRange(this.clock());
        return this.appointmentService.getVeterinarianAvailability(id, from, to).pipe(map(bookableDays), toViewState<BookableDay[]>());
      }),
    ),
    { initialValue: { status: 'loading' } as ViewState<BookableDay[]> },
  );

  // Solo para una cita nueva: las mascotas hospitalizadas, por nombre.
  protected readonly pets = toSignal(
    this.petsRetry.pipe(
      switchMap(() =>
        this.rescheduling()
          ? of<ViewState<Pet[]>>({ status: 'ready', data: [] })
          : this.petService.getAll().pipe(
              map((pets) => pets.filter(isHospitalized).sort((a, b) => a.name.localeCompare(b.name, 'es'))),
              toViewState<Pet[]>(),
            ),
      ),
    ),
    { initialValue: { status: 'loading' } as ViewState<Pet[]> },
  );
  protected readonly petOptions = computed<PetOption[]>(() => {
    const state = this.pets();
    return state.status === 'ready' ? state.data.map((pet) => ({ label: `${pet.name} · ${pet.ownerName}`, value: pet.id })) : [];
  });

  protected readonly loading = computed(
    () => this.availability().status === 'loading' || (!this.rescheduling() && this.pets().status === 'loading'),
  );
  protected readonly skeleton = skeletonVisible(this.loading);

  // Errores de campo: aparecen al intentar guardar (11.4).
  protected readonly reasonError = computed(() =>
    this.submitted() && !this.rescheduling() && !this.reason() ? 'Elige el motivo.' : null,
  );
  protected readonly petError = computed(() =>
    this.submitted() && !this.rescheduling() && this.petId() === null ? 'Elige la mascota hospitalizada.' : null,
  );
  protected readonly slotError = computed(() =>
    this.submitted() && (!this.day() || !this.time()) ? 'Elige el día y la hora.' : null,
  );

  protected pickDay(key: string): void {
    if (key !== this.day()) {
      this.pickedDay.set(key);
      this.pickedTime.set(null);
    }
  }

  protected pickTime(time: string): void {
    this.pickedTime.set(time);
  }

  protected retryAvailability(): void {
    this.availabilityRetry.next();
  }

  protected retryPets(): void {
    this.petsRetry.next();
  }

  protected save(): void {
    this.submitted.set(true);
    this.saveError.set(null);
    const day = this.day();
    const time = this.time();
    if (!day || !time || this.reasonError() || this.petError() || this.saving()) {
      return;
    }
    const startTime = apiDateTime(day, time);
    const appointment = this.appointment();
    const request$: Observable<Appointment> = appointment
      ? // El veterinario no cambia las notas del dueño: van en null (S16.10).
        this.appointmentService.reschedule(appointment.id, { startTime, ownerNotes: null })
      : this.appointmentService.bookForVeterinarian(this.veterinarianId(), {
          petId: this.petId()!,
          reason: this.reason()!,
          startTime,
        });
    this.saving.set(true);
    request$
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (result) => {
          this.notifications.success(
            appointment ? 'Cita reprogramada' : 'Cita agendada',
            `${reasonLabel(result)} de ${result.petName}, ${dayLabel(result.start).toLowerCase()} a las ${time}. Avisamos al dueño por correo.`,
          );
          this.saved.emit(result);
          this.closed.emit();
        },
        error: (error: unknown) => {
          this.saveError.set(getErrorMessage(error, 'No se pudo guardar la cita.'));
          // La franja pudo ocuparse entretanto: se vuelve a pedir la disponibilidad.
          this.availabilityRetry.next();
        },
      });
  }
}
