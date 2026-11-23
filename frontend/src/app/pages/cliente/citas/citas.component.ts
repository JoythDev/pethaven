import { Component, DestroyRef, ElementRef, computed, effect, inject, signal, untracked, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideCalendarClock, LucideCheck, LucidePhone } from '@lucide/angular';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { BehaviorSubject, EMPTY, Observable, Subject, catchError, combineLatest, finalize, forkJoin, map, of, switchMap } from 'rxjs';

import { appointmentWhen } from '../../../core/client-home/client-home';
import {
  BOOKING_REASONS,
  BOOKING_STEPS,
  BookingDraft,
  DATE_STEP,
  EMPTY_DRAFT,
  LAST_STEP,
  MIN_NOTICE_HOURS,
  NEW_PET,
  NEW_PET_NAME_MAX,
  NOTES_MAX,
  apiDateTime,
  bookableDays,
  bookingRequest,
  canChange,
  confirmationText,
  dayLabel,
  draftReasonLabel,
  isStepValid,
  longDayLabel,
  petBlocker,
  petPreselection,
  reasonLabel,
  rescheduleDraft,
  slotOptions,
  splitAppointments,
  withDay,
} from '../../../core/client-appointments/client-appointments';
import { ownerIdParam } from '../../../core/client-pets/owner-route';
import { parseRouteId } from '../../../core/http/route-id';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { NotificationService } from '../../../core/notifications/notification.service';
import { appointmentStatus } from '../../../core/status/appointment-status';
import { coatKitFor, speciesSticker } from '../../../core/stickers/stickers';
import { CLOCK } from '../../../core/time/clock';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import { Appointment, AvailabilityDay } from '../../../models/appointment.model';
import { Clinic } from '../../../models/clinic.model';
import { Pet, Species } from '../../../models/pet.model';
import { AppointmentService } from '../../../services/appointment.service';
import { ClinicService } from '../../../services/clinic.service';
import { PetService } from '../../../services/pet.service';
import { AvatarComponent } from '../../shared/avatar/avatar.component';
import { BlockComponent } from '../../shared/block/block.component';
import { CardComponent } from '../../shared/card/card.component';
import { DefinitionListComponent } from '../../shared/definition-list/definition-list.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { FormFieldComponent } from '../../shared/form-field/form-field.component';
import { InlineErrorComponent } from '../../shared/inline-error/inline-error.component';
import { ListItemComponent } from '../../shared/list-item/list-item.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../shared/sticker/sticker.component';

// Lo que necesita la página: mascotas, citas y disponibilidad del dueño, y el horario de la clínica.
interface BookingContext {
  pets: Pet[];
  appointments: Appointment[];
  availability: AvailabilityDay[];
  clinic: Clinic;
}

const LOADING = { status: 'loading' } as const;
const OWNER_NOT_FOUND: ViewState<BookingContext> = { status: 'error', message: 'Dueño no encontrado.' };
const PAST_LIMIT = 5;

const STEP_HEADINGS = ['¿Para quién es la cita?', '¿Cuál es el motivo?', 'Elige el día', 'Revisa antes de confirmar'] as const;

// Botón de elección (mascota, motivo, día u hora): el relleno lo pone cada uso.
const PICK_CLASS =
  'ph-focus flex min-w-0 cursor-pointer flex-col items-center gap-1.5 rounded-box border-[1.5px] border-line bg-card text-center font-bold text-ink ' +
  'transition-colors duration-[180ms] ease-out-strong hover:bg-sunken motion-reduce:transition-none ' +
  'aria-pressed:border-accent aria-pressed:bg-accent-soft aria-pressed:shadow-[inset_0_0_0_1px_var(--color-accent)] ' +
  'disabled:cursor-not-allowed disabled:opacity-40 disabled:line-through disabled:hover:bg-card';

/**
 * «Agendar cita» del cliente (S16): asistente de cuatro pasos (mascota, motivo, fecha y hora, confirmar)
 * con resumen, «Citas agendadas» e «Historial de citas». Las franjas libres y el veterinario los decide el
 * API. Una mascota nueva solo pide nombre y especie (primera consulta). Reprogramar reutiliza el asistente
 * desde el paso de fecha; cancelar pide confirmación. `?mascota=<id>` preelige la mascota y
 * `?reprogramar=<id>` abre la reprogramación de esa cita.
 */
@Component({
  selector: 'app-cliente-citas',
  imports: [
    FormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    MessageModule,
    SelectModule,
    TagModule,
    TextareaModule,
    LucideCalendarClock,
    LucideCheck,
    LucidePhone,
    AvatarComponent,
    BlockComponent,
    CardComponent,
    DefinitionListComponent,
    EmptyStateComponent,
    FormFieldComponent,
    InlineErrorComponent,
    ListItemComponent,
    PageHeaderComponent,
    SkeletonComponent,
    StickerComponent,
  ],
  templateUrl: './citas.component.html',
})
export class CitasComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly petService = inject(PetService);
  private readonly appointmentService = inject(AppointmentService);
  private readonly clinicService = inject(ClinicService);
  private readonly notifications = inject(NotificationService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly clock = inject(CLOCK);
  private readonly destroyRef = inject(DestroyRef);

  private readonly ownerId$ = ownerIdParam(this.route);
  private readonly ownerId = toSignal(this.ownerId$, { initialValue: null });
  private readonly query = toSignal(this.route.queryParamMap, { initialValue: null });
  private readonly retry$ = new BehaviorSubject<void>(undefined);
  private readonly refresh$ = new Subject<number>();

  protected readonly state = toSignal(
    combineLatest([this.ownerId$, this.retry$]).pipe(
      switchMap(([ownerId]) =>
        ownerId === null ? of(OWNER_NOT_FOUND) : this.loadContext(ownerId).pipe(toViewState<BookingContext>()),
      ),
    ),
    { initialValue: LOADING as ViewState<BookingContext> },
  );
  protected readonly skeleton = skeletonVisible(computed(() => this.state().status === 'loading'));

  private readonly context = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? state.data : null;
  });
  // Citas y disponibilidad recargadas tras agendar, reprogramar o cancelar, sin volver al esqueleto.
  private readonly refreshed = signal<Pick<BookingContext, 'appointments' | 'availability'> | null>(null);
  private readonly appointments = computed(() => this.refreshed()?.appointments ?? this.context()?.appointments ?? []);
  private readonly availability = computed(() => this.refreshed()?.availability ?? this.context()?.availability ?? []);
  protected readonly clinic = computed(() => this.context()?.clinic ?? null);

  protected readonly step = signal(0);
  protected readonly draft = signal<BookingDraft>(EMPTY_DRAFT);
  protected readonly saving = signal(false);
  protected readonly confirmed = signal<Appointment | null>(null);
  // Cita que se está reprogramando; null mientras se agenda una nueva.
  protected readonly rescheduling = signal<Appointment | null>(null);
  protected readonly now = signal(this.clock());

  protected readonly steps = BOOKING_STEPS;
  protected readonly reasons = BOOKING_REASONS;
  protected readonly newPet = NEW_PET;
  protected readonly newPetNameMax = NEW_PET_NAME_MAX;
  protected readonly notesMax = NOTES_MAX;
  protected readonly minNoticeHours = MIN_NOTICE_HOURS;
  protected readonly pickClass = PICK_CLASS;
  protected readonly speciesOptions = [
    { label: 'Perro', value: Species.DOG },
    { label: 'Gato', value: Species.CAT },
  ];
  protected readonly lastStep = LAST_STEP;

  protected readonly stepHeading = computed(() => STEP_HEADINGS[this.step()]);
  protected readonly valid = computed(() => isStepValid(this.step(), this.draft()));
  protected readonly isNewPet = computed(() => this.draft().pet === NEW_PET);
  // Al reprogramar la mascota y el motivo no cambian: el asistente empieza en la fecha.
  protected readonly firstStep = computed(() => (this.rescheduling() ? DATE_STEP : 0));

  // Cada mascota con el motivo por el que no se puede agendar (hospitalizada o con cita activa).
  protected readonly petOptions = computed(() =>
    (this.context()?.pets ?? []).map((pet) => ({ pet, blocker: petBlocker(pet, this.appointments(), this.now()) })),
  );
  private readonly bookablePets = computed(() => this.petOptions().filter((option) => !option.blocker).map((option) => option.pet));

  protected readonly selectedPet = computed<Pet | null>(
    () => (this.context()?.pets ?? []).find((pet) => pet.id === this.draft().pet) ?? null,
  );
  protected readonly petName = computed(() => {
    const pet = this.selectedPet();
    if (pet) {
      return pet.name;
    }
    return this.isNewPet() ? this.draft().newPetName.trim() || 'Mascota nueva' : null;
  });
  protected readonly reasonText = computed(() => draftReasonLabel(this.draft()));

  protected readonly days = computed(() => bookableDays(this.availability()));
  protected readonly selectedDay = computed(() => this.days().find((day) => day.key === this.draft().day) ?? null);
  protected readonly slots = computed(() => {
    const day = this.selectedDay();
    const clinic = this.clinic();
    return day && clinic ? slotOptions(day, clinic) : [];
  });
  // Un día abierto sin franjas libres (por ejemplo, hoy ya tarde o con la agenda llena).
  protected readonly dayFull = computed(() => this.slots().length > 0 && this.slots().every((slot) => slot.disabled));

  protected readonly veterinarianText = computed(() => {
    const appointment = this.rescheduling();
    return appointment ? `${appointment.veterinarianName}, si sigue libre` : 'Lo asigna la clínica al confirmar';
  });

  protected readonly reviewItems = computed(() => {
    const day = this.selectedDay();
    return [
      { term: 'Mascota', value: this.petName() },
      { term: 'Motivo', value: this.reasonText() },
      { term: 'Fecha', value: day ? longDayLabel(day.date) : null },
      { term: 'Hora', value: this.draft().time },
      { term: 'Veterinario', value: this.veterinarianText() },
    ];
  });

  private readonly split = computed(() => splitAppointments(this.appointments(), this.now()));
  protected readonly upcoming = computed(() => this.split().upcoming);
  protected readonly past = computed(() => this.split().past.slice(0, PAST_LIMIT));

  private readonly rescheduledDone = signal(false);
  protected readonly confirmationHeading = computed(() => (this.rescheduledDone() ? 'Cita reprogramada' : 'Cita agendada'));
  protected readonly confirmationText = computed(() => {
    const appointment = this.confirmed();
    return appointment ? confirmationText(appointment) : '';
  });
  protected readonly confirmedSticker = computed(() => {
    const appointment = this.confirmed();
    return appointment && appointment.petId !== null ? speciesSticker(appointment.species) : 'paw';
  });
  protected readonly confirmedKit = computed(() => coatKitFor(this.confirmed()?.petId ?? 1));

  protected readonly homeLink = computed(() => ['/cliente', this.ownerId() ?? 'x']);

  private readonly heading = viewChild<ElementRef<HTMLElement>>('stepTitle');

  protected readonly dayLabel = dayLabel;
  protected readonly reasonLabel = reasonLabel;
  protected readonly status = appointmentStatus;
  protected readonly canChange = (appointment: Appointment) => canChange(appointment, this.now());

  // `?mascota=` y `?reprogramar=` se atienden una sola vez, cuando ya están los datos.
  private mascotaParamHandled = false;
  private rescheduleParamHandled = false;

  constructor() {
    // Preelige la mascota de `?mascota=` una sola vez: «Agendar otra» deja el asistente en blanco.
    effect(() => {
      const id = petPreselection(this.query()?.get('mascota') ?? null, this.bookablePets());
      if (id !== null && !this.mascotaParamHandled) {
        this.mascotaParamHandled = true;
        untracked(() => this.draft.update((draft) => (draft.pet === null && !this.rescheduling() ? { ...draft, pet: id } : draft)));
      }
    });
    // Recarga citas y disponibilidad tras cada cambio; una recarga nueva descarta la anterior.
    this.refresh$
      .pipe(
        switchMap((ownerId) =>
          forkJoin({
            appointments: this.appointmentService.getByOwnerId(ownerId),
            availability: this.appointmentService.getAvailability(),
          }).pipe(
            catchError((error: unknown) => {
              this.notifications.error('No se pudieron actualizar tus citas', error);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((data) => this.refreshed.set(data));
    // Abre la reprogramación de `?reprogramar=` (por ejemplo, desde «Próxima cita» del Inicio).
    effect(() => {
      const id = parseRouteId(this.query()?.get('reprogramar') ?? null);
      const upcoming = this.upcoming();
      if (id === null || this.rescheduleParamHandled || !this.context()) {
        return;
      }
      this.rescheduleParamHandled = true;
      const appointment = upcoming.find((candidate) => candidate.id === id);
      if (appointment && canChange(appointment, this.now())) {
        untracked(() => this.startReschedule(appointment));
      }
    });
  }

  protected retry(): void {
    this.retry$.next();
  }

  // Un paso ya hecho; el de motivo no cuenta para una mascota nueva, que se lo salta.
  protected isDone(index: number): boolean {
    return index < this.step() && !(index === 1 && this.isNewPet() && !this.rescheduling());
  }

  protected speciesLabel(species: Species): string {
    return species === Species.CAT ? 'Gato' : 'Perro';
  }

  protected kit(pet: Pet) {
    return coatKitFor(pet.id);
  }

  // Avatar de una cita: la mascota registrada o, si aún no lo está, un id propio de la cita.
  protected avatarId(appointment: Appointment): number {
    return appointment.petId ?? -appointment.id;
  }

  protected when(appointment: Appointment): string {
    return appointmentWhen(appointment.start, this.now());
  }

  protected valueOf(event: Event): string {
    return (event.target as HTMLInputElement | HTMLTextAreaElement).value;
  }

  protected set(changes: Partial<BookingDraft>): void {
    this.draft.update((draft) => ({ ...draft, ...changes }));
  }

  protected pickDay(key: string): void {
    this.draft.update((draft) => withDay(draft, key));
  }

  protected back(): void {
    if (this.step() > this.firstStep() && !this.saving()) {
      // Una mascota nueva no tiene paso de motivo.
      this.goTo(this.step() === DATE_STEP && this.isNewPet() && !this.rescheduling() ? 0 : this.step() - 1);
    }
  }

  protected next(): void {
    if (!this.valid() || this.saving()) {
      return;
    }
    if (this.step() < LAST_STEP) {
      this.goTo(this.step() === 0 && this.isNewPet() ? DATE_STEP : this.step() + 1);
    } else {
      this.confirm();
    }
  }

  protected reset(): void {
    this.draft.set(EMPTY_DRAFT);
    this.confirmed.set(null);
    this.rescheduling.set(null);
    this.rescheduledDone.set(false);
    this.now.set(this.clock());
    this.goTo(0);
  }

  protected startReschedule(appointment: Appointment): void {
    this.confirmed.set(null);
    this.rescheduledDone.set(false);
    this.rescheduling.set(appointment);
    this.draft.set(rescheduleDraft(appointment));
    this.goTo(DATE_STEP);
  }

  protected cancel(appointment: Appointment): void {
    this.confirmation.confirm({
      header: `¿Cancelar la cita de ${appointment.petName}?`,
      message: `La franja del ${this.when(appointment).toLowerCase()} quedará libre para otra persona.`,
      acceptLabel: 'Cancelar cita',
      rejectLabel: 'Mantener cita',
      rejectButtonProps: { outlined: true, severity: 'secondary' },
      acceptButtonProps: { severity: 'danger' },
      accept: () => this.doCancel(appointment),
    });
  }

  private doCancel(appointment: Appointment): void {
    this.appointmentService
      .cancel(appointment.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notifications.success('Cita cancelada', `La cita de ${appointment.petName} quedó cancelada.`);
          if (this.rescheduling()?.id === appointment.id) {
            this.reset();
          }
          this.refresh();
        },
        error: (error: unknown) => {
          this.notifications.error('No se pudo cancelar la cita', error);
          this.refresh();
        },
      });
  }

  private goTo(step: number): void {
    this.step.set(step);
    if (step === DATE_STEP) {
      this.now.set(this.clock());
    }
    // El foco sigue al contenido nuevo del paso.
    setTimeout(() => this.heading()?.nativeElement.focus());
  }

  private confirm(): void {
    const ownerId = this.ownerId();
    const draft = this.draft();
    const appointment = this.rescheduling();
    if (ownerId === null || !draft.day || !draft.time) {
      return;
    }
    let request$: Observable<Appointment>;
    if (appointment) {
      request$ = this.appointmentService.reschedule(appointment.id, {
        startTime: apiDateTime(draft.day, draft.time),
        // Texto vacío (no null): el API conserva las notas con null y las borra con un texto en blanco.
        ownerNotes: draft.notes.trim(),
      });
    } else {
      const request = bookingRequest(draft);
      if (!request) {
        return;
      }
      request$ = this.appointmentService.book(ownerId, request);
    }
    this.saving.set(true);
    request$
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (result) => {
          this.rescheduledDone.set(appointment !== null);
          this.rescheduling.set(null);
          this.confirmed.set(result);
          this.refresh();
        },
        error: (error: unknown) => {
          // La franja se ocupó o cambió la situación: se vuelve a elegir la hora con datos frescos.
          this.notifications.error(appointment ? 'No se pudo reprogramar la cita' : 'No se pudo agendar la cita', error);
          this.set({ time: null });
          this.refresh();
          this.goTo(DATE_STEP);
        },
      });
  }

  private refresh(): void {
    const ownerId = this.ownerId();
    if (ownerId === null) {
      return;
    }
    this.now.set(this.clock());
    this.refresh$.next(ownerId);
  }

  private loadContext(ownerId: number): Observable<BookingContext> {
    return forkJoin({
      pets: this.petService.getByOwnerId(ownerId),
      appointments: this.appointmentService.getByOwnerId(ownerId),
      availability: this.appointmentService.getAvailability(),
      clinic: this.clinicService.getClinic(),
    }).pipe(map((context) => {
      this.refreshed.set(null);
      return context;
    }));
  }
}
