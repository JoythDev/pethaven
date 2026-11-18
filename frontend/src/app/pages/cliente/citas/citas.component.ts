import { Component, DestroyRef, ElementRef, computed, effect, inject, signal, untracked, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideCheck } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { BehaviorSubject, combineLatest, finalize, map, of, switchMap } from 'rxjs';

import { appointmentWhen } from '../../../core/client-home/client-home';
import {
  BOOKING_DURATION,
  BOOKING_REASONS,
  BOOKING_STEPS,
  BookingDraft,
  EMPTY_DRAFT,
  LAST_STEP,
  NEW_PET,
  bookableDays,
  confirmationText,
  dayLabel,
  isStepValid,
  longDayLabel,
  newPetFrom,
  petPreselection,
  slotOptions,
  vetForPet,
  visibleUpcoming,
  withDay,
} from '../../../core/client-appointments/client-appointments';
import { ownerIdParam } from '../../../core/client-pets/owner-route';
import { atTime } from '../../../core/demo/demo-date';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { NotificationService } from '../../../core/notifications/notification.service';
import { coatKitFor, speciesSticker } from '../../../core/stickers/stickers';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import { Appointment } from '../../../models/demo/appointment.model';
import { OwnerContext } from '../../../models/demo/owner-context.model';
import { Pet, Species } from '../../../models/pet.model';
import { AuthService } from '../../../services/auth.service';
import { DemoAppointmentsService } from '../../../services/demo/demo-appointments.service';
import { DEMO_NOW } from '../../../services/demo/demo-latency';
import { AvatarComponent } from '../../shared/avatar/avatar.component';
import { BlockComponent } from '../../shared/block/block.component';
import { CardComponent } from '../../shared/card/card.component';
import { DefinitionListComponent } from '../../shared/definition-list/definition-list.component';
import { DemoTagComponent } from '../../shared/demo-tag/demo-tag.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { FormFieldComponent } from '../../shared/form-field/form-field.component';
import { InlineErrorComponent } from '../../shared/inline-error/inline-error.component';
import { ListItemComponent } from '../../shared/list-item/list-item.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../shared/sticker/sticker.component';

const LOADING = { status: 'loading' } as const;
const OWNER_NOT_FOUND: ViewState<OwnerContext> = { status: 'error', message: 'Dueño no encontrado.' };
const UPCOMING_LIMIT = 4;

const STEP_HEADINGS = ['¿Para quién es la cita?', '¿Cuál es el motivo?', 'Elige el día', 'Revisa antes de confirmar'] as const;

// Botón de elección (mascota, motivo, día u hora): el relleno lo pone cada uso.
const PICK_CLASS =
  'ph-focus flex min-w-0 cursor-pointer flex-col items-center gap-1.5 rounded-box border-[1.5px] border-line bg-card text-center font-bold text-ink ' +
  'transition-colors duration-[180ms] ease-out-strong hover:bg-sunken motion-reduce:transition-none ' +
  'aria-pressed:border-accent aria-pressed:bg-accent-soft aria-pressed:shadow-[inset_0_0_0_1px_var(--color-accent)] ' +
  'disabled:cursor-not-allowed disabled:opacity-40 disabled:line-through disabled:hover:bg-card';

/**
 * «Agendar cita» del cliente (demo, S3): asistente de cuatro pasos (mascota, motivo, fecha y hora,
 * confirmar) con resumen y «Citas agendadas». Las mascotas son reales; las citas y la disponibilidad
 * salen del servicio demo y lo agendado vive solo en memoria. S21: el primer paso ofrece «Mascota
 * nueva», que el veterinario registra en la primera visita. `?mascota=<id>` preelige la mascota.
 */
@Component({
  selector: 'app-cliente-citas',
  imports: [
    FormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    SelectModule,
    TextareaModule,
    LucideCheck,
    AvatarComponent,
    BlockComponent,
    CardComponent,
    DefinitionListComponent,
    DemoTagComponent,
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
  private readonly auth = inject(AuthService);
  private readonly appointments = inject(DemoAppointmentsService);
  private readonly notifications = inject(NotificationService);
  private readonly clock = inject(DEMO_NOW);
  private readonly destroyRef = inject(DestroyRef);

  private readonly ownerId$ = ownerIdParam(this.route);
  private readonly ownerId = toSignal(this.ownerId$, { initialValue: null });
  private readonly mascotaParam = toSignal(this.route.queryParamMap.pipe(map((params) => params.get('mascota'))), {
    initialValue: null,
  });
  private readonly retry$ = new BehaviorSubject<void>(undefined);

  protected readonly state = toSignal(
    combineLatest([this.ownerId$, this.retry$]).pipe(
      switchMap(([ownerId]) =>
        ownerId === null ? of(OWNER_NOT_FOUND) : this.appointments.loadOwnerContext(ownerId).pipe(toViewState<OwnerContext>()),
      ),
    ),
    { initialValue: LOADING as ViewState<OwnerContext> },
  );
  protected readonly skeleton = skeletonVisible(computed(() => this.state().status === 'loading'));

  private readonly context = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? state.data : null;
  });
  // Una mascota inactiva no se agenda.
  protected readonly pets = computed(() => (this.context()?.pets ?? []).filter((pet) => pet.active));

  protected readonly step = signal(0);
  protected readonly draft = signal<BookingDraft>(EMPTY_DRAFT);
  protected readonly saving = signal(false);
  protected readonly confirmed = signal<Appointment | null>(null);
  private readonly now = signal(this.clock());

  protected readonly steps = BOOKING_STEPS;
  protected readonly reasons = BOOKING_REASONS;
  protected readonly duration = BOOKING_DURATION;
  protected readonly newPet = NEW_PET;
  protected readonly pickClass = PICK_CLASS;
  protected readonly speciesOptions = [
    { label: 'Perro', value: Species.DOG },
    { label: 'Gato', value: Species.CAT },
  ];
  protected readonly lastStep = LAST_STEP;

  protected readonly stepHeading = computed(() => STEP_HEADINGS[this.step()]);
  protected readonly valid = computed(() => isStepValid(this.step(), this.draft()));
  protected readonly isNewPet = computed(() => this.draft().pet === NEW_PET);

  protected readonly selectedPet = computed<Pet | null>(() => this.pets().find((pet) => pet.id === this.draft().pet) ?? null);
  protected readonly petName = computed(() => {
    const pet = this.selectedPet();
    if (pet) {
      return pet.name;
    }
    return this.isNewPet() ? this.draft().newPetName.trim() || 'Mascota nueva' : null;
  });
  protected readonly reason = computed(() => BOOKING_REASONS.find((r) => r.kind === this.draft().kind) ?? null);

  protected readonly days = computed(() => bookableDays(this.now()));
  protected readonly selectedDay = computed(() => this.days().find((day) => day.key === this.draft().day) ?? null);
  private readonly start = computed(() => {
    const day = this.selectedDay();
    const time = this.draft().time;
    return day && time ? atTime(day.date, time) : null;
  });

  // Veterinaria de la cita (demo): la que atendió a la mascota o la primera que el dueño conoce.
  protected readonly vet = computed(() => {
    const context = this.context();
    const petId = this.selectedPet()?.id ?? null;
    return vetForPet(petId, context?.petTreatments.flatMap((entry) => entry.treatments) ?? [], context?.vets ?? []);
  });

  protected readonly slots = computed(() => {
    const day = this.selectedDay();
    if (!day) {
      return [];
    }
    return slotOptions(day.date, this.appointments.availableSlotsFrom(this.vet(), this.context()?.pets ?? [], day.date));
  });
  // Un día abierto sin franjas libres (por ejemplo, hoy ya tarde).
  protected readonly dayFull = computed(() => this.slots().length > 0 && this.slots().every((slot) => slot.disabled));

  protected readonly reviewItems = computed(() => {
    const start = this.start();
    return [
      { term: 'Mascota', value: this.petName() },
      { term: 'Motivo', value: this.reason()?.label ?? null },
      { term: 'Fecha', value: start ? longDayLabel(start) : null },
      { term: 'Hora', value: this.draft().time },
      { term: 'Veterinaria', value: this.vet().name },
    ];
  });

  // «Citas agendadas»: se recalcula sola al agendar porque el servicio guarda las citas en una señal.
  protected readonly upcoming = computed(() => {
    const context = this.context();
    if (!context) {
      return [];
    }
    const { upcoming } = this.appointments.ownerAppointmentsFrom(context.pets, context.vets, this.ownerId() ?? undefined);
    return visibleUpcoming(upcoming, new Set(this.appointments.booked().map((a) => a.id)), UPCOMING_LIMIT);
  });

  protected readonly confirmationText = computed(() => {
    const appointment = this.confirmed();
    return appointment ? confirmationText(appointment.petName, appointment.start) : '';
  });
  protected readonly confirmedSticker = computed(() => {
    const appointment = this.confirmed();
    return appointment && appointment.petId > 0 ? speciesSticker(appointment.species) : 'paw';
  });
  protected readonly confirmedKit = computed(() => coatKitFor(this.confirmed()?.petId ?? 1));

  protected readonly homeLink = computed(() => ['/cliente', this.ownerId() ?? 'x']);

  private readonly heading = viewChild<ElementRef<HTMLElement>>('stepTitle');

  protected readonly dayLabel = dayLabel;

  constructor() {
    // Preelige la mascota de `?mascota=` una sola vez: «Agendar otra» deja el asistente en blanco.
    effect(() => {
      const id = petPreselection(this.mascotaParam(), this.pets());
      if (id !== null) {
        untracked(() => this.draft.update((draft) => (draft.pet === null ? { ...draft, pet: id } : draft)));
      }
    });
  }

  protected retry(): void {
    this.retry$.next();
  }

  protected speciesLabel(species: Species): string {
    return species === Species.CAT ? 'Gato' : 'Perro';
  }

  protected kit(pet: Pet) {
    return coatKitFor(pet.id);
  }

  protected when(appointment: Appointment): string {
    return appointmentWhen(appointment.start, this.clock());
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
    if (this.step() > 0 && !this.saving()) {
      this.goTo(this.step() - 1);
    }
  }

  protected next(): void {
    if (!this.valid() || this.saving()) {
      return;
    }
    if (this.step() < LAST_STEP) {
      this.goTo(this.step() + 1);
    } else {
      this.confirm();
    }
  }

  protected reset(): void {
    this.draft.set(EMPTY_DRAFT);
    this.confirmed.set(null);
    this.now.set(this.clock());
    this.goTo(0);
  }

  protected reschedule(): void {
    this.notifications.success('Te enviamos tres horarios alternativos', '');
  }

  private goTo(step: number): void {
    this.step.set(step);
    if (step === 2) {
      this.now.set(this.clock());
    }
    // El foco sigue al contenido nuevo del paso.
    setTimeout(() => this.heading()?.nativeElement.focus());
  }

  private confirm(): void {
    const draft = this.draft();
    const start = this.start();
    const kind = draft.kind;
    const pet = draft.pet === NEW_PET ? this.newPetFor(draft) : this.selectedPet();
    if (!start || !kind || !pet) {
      return;
    }
    this.saving.set(true);
    this.appointments
      .book({ pet, veterinarian: this.vet(), kind, start })
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (appointment) => this.confirmed.set(appointment),
        error: (error: unknown) => {
          // La franja se ocupó mientras tanto: se vuelve a elegir la hora.
          this.notifications.error('No se pudo agendar la cita', error);
          this.set({ time: null });
          this.goTo(2);
        },
      });
  }

  private newPetFor(draft: BookingDraft): Pet {
    const name = this.auth.user()?.name ?? this.context()?.pets[0]?.ownerName ?? '';
    return newPetFrom(draft, { id: this.ownerId() ?? 0, name });
  }
}
