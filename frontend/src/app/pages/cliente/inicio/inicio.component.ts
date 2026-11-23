import { Component, DestroyRef, computed, inject, signal, untracked } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideCheck, LucideClock, LucidePlus } from '@lucide/angular';
import { BehaviorSubject, Observable, combineLatest, forkJoin, map, of, shareReplay, switchMap } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import {
  HERO_FALLBACK,
  HeroCopy,
  PetPlan,
  Reminder,
  appointmentWhen,
  buildReminders,
  clinicHoursText,
  heroCopy,
  pickFeaturedPet,
  recentTreatments,
  vaccineHighlights,
  vaccineSummaryText,
  whenTag,
  HERO_NO_PETS,
} from '../../../core/client-home/client-home';
import { canChange, splitAppointments } from '../../../core/client-appointments/client-appointments';
import { parseRouteId } from '../../../core/http/route-id';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { ageLabel, vaccineSeverity } from '../../../core/pet-detail/pet-detail';
import { petStatus } from '../../../core/status/pet-status';
import { coatKitFor, speciesSticker } from '../../../core/stickers/stickers';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import { CLOCK } from '../../../core/time/clock';
import { firstName, longDateEs } from '../../../core/vet-home/vet-home';
import { APPOINTMENT_REASON_LABELS, Appointment } from '../../../models/appointment.model';
import { Clinic } from '../../../models/clinic.model';
import { CareItem } from '../../../models/demo/care-plan.model';
import { DailyTip } from '../../../models/demo/daily-tip.model';
import { VaccineCard } from '../../../models/demo/vaccine.model';
import { Pet } from '../../../models/pet.model';
import { ClienteTreatment } from '../../../models/treatment-cliente.model';
import { AppointmentService } from '../../../services/appointment.service';
import { AuthService } from '../../../services/auth.service';
import { ClinicService } from '../../../services/clinic.service';
import { DemoCarePlanService } from '../../../services/demo/demo-care-plan.service';
import { DemoDailyTipService } from '../../../services/demo/demo-daily-tip.service';
import { DemoVaccinesService } from '../../../services/demo/demo-vaccines.service';
import { PetService } from '../../../services/pet.service';
import { ClienteTreatmentService } from '../../../services/treatment-cliente.service';
import { RatioRingComponent } from '../../shared/ratio-ring/ratio-ring.component';
import { AvatarComponent } from '../../shared/avatar/avatar.component';
import { BlockComponent } from '../../shared/block/block.component';
import { CardComponent } from '../../shared/card/card.component';
import { DemoTagComponent } from '../../shared/demo-tag/demo-tag.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { HeroComponent } from '../../shared/hero/hero.component';
import { InlineErrorComponent } from '../../shared/inline-error/inline-error.component';
import { ListItemComponent } from '../../shared/list-item/list-item.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { ShellSidebarDirective } from '../../shared/shell/shell-sidebar.directive';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../shared/sticker/sticker.component';

// Mascota del dueño con sus tratamientos reales: de ahí salen el plan, los recientes y las citas demo.
interface PetHistory {
  pet: Pet;
  treatments: ClienteTreatment[];
}

// Lo que pinta el héroe: cargando, error o texto listo (con la mascota destacada, si la hay).
type HeroView =
  | { kind: 'loading' }
  | { kind: 'error' }
  | { kind: 'ready'; copy: HeroCopy; pet: Pet | null };

const LOADING = { status: 'loading' } as const;
const CLOCK_TICK_MS = 60_000;
const PETS_LIMIT = 3;
const RECENT_LIMIT = 4;
const VACCINES_LIMIT = 3;
const SIDEBAR_ROWS = [0, 1, 2];
const SKELETON_ROWS = [0, 1, 2, 3];
const OWNER_NOT_FOUND: ViewState<Pet[]> = { status: 'error', message: 'Dueño no encontrado.' };

/**
 * Inicio del cliente (11.6, 11.15): héroe con el cuidado pendiente de su mascota, próxima cita,
 * mis mascotas (solo lectura, S21), plan de cuidado, carné de vacunas, tratamientos recientes y
 * «Consejo del día» (S22). Mascotas, tratamientos, próxima cita y «Tu clínica» son reales; plan,
 * vacunas y consejo son demo (S3). Cada fuente carga y falla por separado.
 */
@Component({
  selector: 'app-cliente-inicio',
  imports: [
    RouterLink,
    ButtonModule,
    TagModule,
    LucideCheck,
    LucideClock,
    LucidePlus,
    AvatarComponent,
    BlockComponent,
    CardComponent,
    DemoTagComponent,
    EmptyStateComponent,
    HeroComponent,
    InlineErrorComponent,
    ListItemComponent,
    PageHeaderComponent,
    RatioRingComponent,
    ShellSidebarDirective,
    SkeletonComponent,
    StickerComponent,
  ],
  templateUrl: './inicio.component.html',
})
export class InicioComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly petService = inject(PetService);
  private readonly treatmentService = inject(ClienteTreatmentService);
  private readonly appointmentService = inject(AppointmentService);
  private readonly clinicService = inject(ClinicService);
  private readonly carePlan = inject(DemoCarePlanService);
  private readonly vaccineService = inject(DemoVaccinesService);
  private readonly tipService = inject(DemoDailyTipService);
  private readonly clock = inject(CLOCK);

  // Reloj de la página: mantiene bien «Hoy» y «Mañana» si cambia el día con la página abierta.
  protected readonly now = signal(this.clock());

  // :ownerId pertenece a la ruta padre; null si el id es inválido.
  private readonly ownerId$ = this.route.parent!.paramMap.pipe(map((params) => parseRouteId(params.get('ownerId'))));
  private readonly ownerId = toSignal(this.ownerId$, { initialValue: null });

  private readonly petsRetry = new BehaviorSubject<void>(undefined);
  private readonly historiesRetry = new BehaviorSubject<void>(undefined);
  private readonly appointmentsRetry = new BehaviorSubject<void>(undefined);
  private readonly clinicRetry = new BehaviorSubject<void>(undefined);
  private readonly vaccinesRetry = new BehaviorSubject<void>(undefined);
  private readonly tipRetry = new BehaviorSubject<void>(undefined);

  // Mascotas del dueño (reales). Se comparte para que los tratamientos no repitan la petición.
  private readonly pets$: Observable<ViewState<Pet[]>> = combineLatest([this.ownerId$, this.petsRetry]).pipe(
    switchMap(([ownerId]) =>
      ownerId === null ? of(OWNER_NOT_FOUND) : this.petService.getByOwnerId(ownerId).pipe(toViewState<Pet[]>()),
    ),
  );
  private readonly sharedPets$ = this.pets$.pipe(shareReplay({ bufferSize: 1, refCount: true }));

  protected readonly pets = toSignal(this.sharedPets$, { initialValue: LOADING as ViewState<Pet[]> });

  // Tratamientos de todas sus mascotas (reales); dependen de las mascotas y las siguen si se recargan.
  protected readonly histories = toSignal(
    combineLatest([this.sharedPets$, this.historiesRetry]).pipe(
      switchMap(([pets]): Observable<ViewState<PetHistory[]>> => {
        if (pets.status === 'loading') {
          return of(LOADING);
        }
        return pets.status === 'error' ? of(pets) : this.loadHistories(pets.data).pipe(toViewState<PetHistory[]>());
      }),
    ),
    { initialValue: LOADING as ViewState<PetHistory[]> },
  );

  // Citas del dueño (reales): de ahí sale la próxima.
  protected readonly appointments = toSignal(
    combineLatest([this.ownerId$, this.appointmentsRetry]).pipe(
      switchMap(([ownerId]): Observable<ViewState<Appointment[]>> =>
        ownerId === null
          ? of({ status: 'error', message: 'Dueño no encontrado.' })
          : this.appointmentService.getByOwnerId(ownerId).pipe(toViewState<Appointment[]>()),
      ),
    ),
    { initialValue: LOADING as ViewState<Appointment[]> },
  );

  protected readonly clinic = toSignal(
    this.clinicRetry.pipe(switchMap(() => this.clinicService.getClinic().pipe(toViewState<Clinic>()))),
    { initialValue: LOADING as ViewState<Clinic> },
  );
  protected readonly clinicHours = computed(() => {
    const state = this.clinic();
    return state.status === 'ready' ? clinicHoursText(state.data.hours) : '';
  });

  protected readonly tip = toSignal(
    this.tipRetry.pipe(switchMap(() => this.tipService.getTip().pipe(toViewState<DailyTip>()))),
    { initialValue: LOADING as ViewState<DailyTip> },
  );

  protected readonly petList = computed(() => {
    const state = this.pets();
    return state.status === 'ready' ? state.data : [];
  });
  protected readonly hasPets = computed(() => this.petList().length > 0);
  // Sin mascotas no hay nada que cuidar, vacunar ni agendar: se omiten esas tarjetas.
  protected readonly showPetCards = computed(() => this.pets().status !== 'ready' || this.hasPets());
  protected readonly visiblePets = computed(() => this.petList().slice(0, PETS_LIMIT));

  // Anchos de rejilla (9.7): sin mascotas no hay tarjetas de cuidado, así que «Mis mascotas» y el consejo se reparten la fila.
  protected readonly petsColClass = computed(() =>
    this.showPetCards()
      ? 'col-span-12 min-w-0 @min-[680px]:col-span-6 @min-[960px]:col-span-4'
      : 'col-span-12 min-w-0 @min-[960px]:col-span-4',
  );
  protected readonly tipColClass = computed(() =>
    this.showPetCards() ? 'col-span-12 min-w-0 @min-[960px]:col-span-5' : 'col-span-12 min-w-0',
  );

  // Plan de cada mascota activa. El plan se arma con los tratamientos reales ya cargados.
  private readonly plans = computed<PetPlan[]>(() => {
    const state = this.histories();
    return state.status === 'ready'
      ? state.data.map(({ pet, treatments }) => ({ pet, plan: this.carePlan.planFrom(pet, treatments) }))
      : [];
  });

  // Mascota destacada. Los cuidados ya marcados se leen sin seguirlos: marcar uno no cambia de mascota.
  protected readonly featured = computed<Pet | null>(() => {
    const pets = this.pets();
    if (pets.status !== 'ready') {
      return null;
    }
    const histories = this.histories();
    if (histories.status === 'ready') {
      return pickFeaturedPet(this.plans(), untracked(() => this.carePlan.doneIds()));
    }
    // Sin tratamientos no hay plan que comparar: sirve la primera mascota activa.
    return histories.status === 'error' ? pickFeaturedPet(pets.data.map((pet) => ({ pet, plan: [] })), new Set()) : null;
  });

  protected readonly plan = computed<CareItem[]>(() => {
    const pet = this.featured();
    return this.plans().find((entry) => entry.pet.id === pet?.id)?.plan ?? [];
  });
  protected readonly progress = computed(() => this.carePlan.progress(this.plan()));
  protected readonly progressPercent = computed(() => {
    const { done, total } = this.progress();
    return total ? Math.round((done / total) * 100) : 0;
  });
  private readonly pending = computed(() => this.progress().total - this.progress().done);

  protected readonly hero = computed<HeroView>(() => {
    const pets = this.pets();
    if (pets.status !== 'ready') {
      return pets.status === 'error' ? { kind: 'error' } : { kind: 'loading' };
    }
    if (!pets.data.length) {
      return { kind: 'ready', copy: HERO_NO_PETS, pet: null };
    }
    const histories = this.histories();
    const pet = this.featured();
    if (histories.status === 'loading' || !pet) {
      return { kind: 'loading' };
    }
    const copy =
      histories.status === 'error'
        ? HERO_FALLBACK
        : heroCopy(pet.name, this.pending(), this.progress().total);
    return { kind: 'ready', copy, pet };
  });
  protected readonly heroSticker = computed(() => {
    const view = this.hero();
    return view.kind === 'ready' && view.pet ? speciesSticker(view.pet.species) : 'dog';
  });
  protected readonly heroKit = computed(() => {
    const view = this.hero();
    return view.kind === 'ready' && view.pet ? coatKitFor(view.pet.id) : 1;
  });

  protected readonly nextAppointment = computed<Appointment | null>(() => {
    const state = this.appointments();
    return state.status === 'ready' ? (splitAppointments(state.data, this.now()).upcoming[0] ?? null) : null;
  });

  protected readonly recent = computed(() => {
    const pet = this.featured();
    const state = this.histories();
    const treatments = state.status === 'ready' ? (state.data.find((entry) => entry.pet.id === pet?.id)?.treatments ?? []) : [];
    return recentTreatments(treatments, RECENT_LIMIT);
  });

  // Carné de la mascota destacada (demo).
  protected readonly vaccines = toSignal(
    combineLatest([toObservable(this.featured), this.vaccinesRetry]).pipe(
      switchMap(([pet]): Observable<ViewState<VaccineCard>> =>
        pet ? this.vaccineService.getCard(pet).pipe(toViewState<VaccineCard>()) : of(LOADING),
      ),
    ),
    { initialValue: LOADING as ViewState<VaccineCard> },
  );
  protected readonly vaccineRows = computed(() => {
    const state = this.vaccines();
    return state.status === 'ready' ? vaccineHighlights(state.data, VACCINES_LIMIT) : [];
  });
  protected readonly vaccineSummary = computed(() => {
    const state = this.vaccines();
    return state.status === 'ready' ? vaccineSummaryText(state.data.summary) : '';
  });

  protected readonly reminders = computed<Reminder[]>(() => {
    const vaccines = this.vaccines();
    return buildReminders({
      petName: this.featured()?.name ?? null,
      pending: this.pending(),
      next: this.nextAppointment(),
      vaccine: vaccines.status === 'ready' ? (vaccineHighlights(vaccines.data, 1)[0] ?? null) : null,
    });
  });

  // «Cargando» de cada tarjeta: una tarjeta que depende de las mascotas también espera a estas.
  protected readonly petsSkeleton = skeletonVisible(computed(() => this.pets().status === 'loading'));
  protected readonly historiesSkeleton = skeletonVisible(
    computed(() => this.pets().status === 'loading' || this.histories().status === 'loading'),
  );
  protected readonly vaccinesSkeleton = skeletonVisible(
    computed(
      () => this.pets().status === 'loading' || this.histories().status === 'loading' || this.vaccines().status === 'loading',
    ),
  );
  protected readonly tipSkeleton = skeletonVisible(computed(() => this.tip().status === 'loading'));
  protected readonly appointmentsSkeleton = skeletonVisible(computed(() => this.appointments().status === 'loading'));

  // Una tarjeta que depende de las mascotas falla si fallan estas o los tratamientos.
  protected readonly historiesFailed = computed(
    () => this.pets().status === 'error' || this.histories().status === 'error',
  );

  protected readonly skeletonRows = SKELETON_ROWS;
  protected readonly sidebarRows = SIDEBAR_ROWS;
  protected readonly dateLabel = computed(() => longDateEs(this.now()));
  protected readonly kicker = computed(() => {
    const name = firstName(this.auth.user()?.name);
    return name ? `Hola, ${name}` : 'Hola';
  });

  protected readonly ageLabel = ageLabel;
  protected readonly petStatus = petStatus;
  protected readonly vaccineSeverity = vaccineSeverity;
  protected readonly whenTag = whenTag;

  // Enlaces del área del dueño; con un id inválido apuntan a /cliente/x/..., como el resto del área.
  protected readonly base = computed(() => ['/cliente', this.ownerId() ?? 'x']);
  protected readonly bookLink = computed(() => [...this.base(), 'citas']);
  protected readonly petsLink = computed(() => [...this.base(), 'mascotas']);

  constructor() {
    const timer = setInterval(() => this.now.set(this.clock()), CLOCK_TICK_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  // «Reprogramar» abre el asistente de citas en la reprogramación de esa cita.
  protected rescheduleQuery(appointment: Appointment): { reprogramar: number } {
    return { reprogramar: appointment.id };
  }

  protected canChange(appointment: Appointment): boolean {
    return canChange(appointment, this.now());
  }

  protected reasonLabel(appointment: Appointment): string {
    return APPOINTMENT_REASON_LABELS[appointment.reason];
  }

  protected retryAppointments(): void {
    this.appointmentsRetry.next();
  }

  protected retryClinic(): void {
    this.clinicRetry.next();
  }

  protected petLink(petId: number): (string | number)[] {
    return [...this.base(), 'mascotas', petId];
  }

  protected when(appointment: Appointment): string {
    return appointmentWhen(appointment.start, this.now());
  }

  protected isDone(item: CareItem): boolean {
    return this.carePlan.isDone(item.id);
  }

  protected toggleCare(item: CareItem): void {
    this.carePlan.toggle(item.id);
  }

  protected retryPets(): void {
    this.petsRetry.next();
  }

  // Si fallaron las mascotas se vuelve a empezar por ellas; si no, solo los tratamientos.
  protected retryHistories(): void {
    if (this.pets().status === 'error') {
      this.petsRetry.next();
    } else {
      this.historiesRetry.next();
    }
  }

  protected retryVaccines(): void {
    this.vaccinesRetry.next();
  }

  protected retryTip(): void {
    this.tipRetry.next();
  }

  private loadHistories(pets: Pet[]): Observable<PetHistory[]> {
    if (!pets.length) {
      return of([]);
    }
    return forkJoin(
      pets.map((pet) =>
        this.treatmentService.getByPetId(pet.id).pipe(map((treatments): PetHistory => ({ pet, treatments }))),
      ),
    );
  }
}
