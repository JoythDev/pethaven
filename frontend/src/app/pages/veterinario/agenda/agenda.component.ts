import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, linkedSignal, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideChevronLeft, LucideChevronRight, LucideClock, LucidePlus, LucideStethoscope } from '@lucide/angular';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import {
  BehaviorSubject,
  MonoTypeOperatorFunction,
  Observable,
  catchError,
  Subject,
  startWith,
  filter,
  finalize,
  forkJoin,
  map,
  of,
  switchMap,
  throwError,
} from 'rxjs';

import { buildDayTimeline } from '../../../core/day-timeline/day-timeline';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { identityHue } from '../../../core/identity/identity';
import { NotificationService } from '../../../core/notifications/notification.service';
import { appointmentStatus } from '../../../core/status/appointment-status';
import { petStatus } from '../../../core/status/pet-status';
import { addDays, addMonths, dayKey, formatTime, isSameDay, startOfDay } from '../../../core/time/calendar';
import { CLOCK } from '../../../core/time/clock';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import {
  canMarkNoShow,
  canStart,
  completionKind,
  needsPetRegistration,
  provisionalPetLine,
  reasonLabel,
  startHint,
  OUTCOME_LABELS,
} from '../../../core/vet-appointments/vet-appointments';
import {
  AgendaState,
  AgendaView,
  agendaQueryParams,
  agendaSubtitle,
  buildDayAgenda,
  countsByDay,
  dayLabelEs,
  lastVisitLabel,
  mondayOf,
  monthRange,
  monthTitleEs,
  parseAgendaParams,
  petSummaryLine,
  resolveSelection,
  timelineRows,
  weekDays,
} from '../../../core/vet-agenda/vet-agenda';
import { WeekSummary, weekSummary } from '../../../core/vet-home/vet-home';
import { Appointment, DayAgenda, DayCount } from '../../../models/appointment.model';
import { Clinic } from '../../../models/clinic.model';
import { Pet, Species } from '../../../models/pet.model';
import { Treatment } from '../../../models/treatment.model';
import { AppointmentService } from '../../../services/appointment.service';
import { AuthService } from '../../../services/auth.service';
import { ClinicService } from '../../../services/clinic.service';
import { PetService } from '../../../services/pet.service';
import { VeterinarianService } from '../../../services/veterinarian.service';
import { AvatarComponent } from '../../shared/avatar/avatar.component';
import { CardComponent } from '../../shared/card/card.component';
import { DaySummaryComponent } from '../../shared/day-summary/day-summary.component';
import { DefinitionListComponent } from '../../shared/definition-list/definition-list.component';
import { DetailPanelComponent } from '../../shared/detail-panel/detail-panel.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { InlineErrorComponent } from '../../shared/inline-error/inline-error.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { ShellSidebarDirective } from '../../shared/shell/shell-sidebar.directive';
import { ShellSidebarService } from '../../shared/shell/shell-sidebar.service';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../shared/sticker/sticker.component';
import { WeekSummaryComponent } from '../../shared/week-summary/week-summary.component';
import { AppointmentDialogComponent } from './appointment-dialog/appointment-dialog.component';
import { CompleteDialogComponent } from './complete-dialog/complete-dialog.component';
import { MonthCalendarComponent } from './month-calendar/month-calendar.component';

const LOADING = { status: 'loading' } as const;
const NOT_A_VET = 'No hay un veterinario autenticado.';
const CLOCK_TICK_MS = 30_000;

const VIEW_OPTIONS: { label: string; value: AgendaView }[] = [
  { label: 'Día', value: 'dia' },
  { label: 'Semana', value: 'semana' },
];

const sameTime = (a: Date, b: Date): boolean => a.getTime() === b.getTime();
const sameOptionalTime = (a: Date | null, b: Date | null): boolean => (a && b ? sameTime(a, b) : a === b);

// Recarga tras un cambio: conserva los datos en pantalla hasta que llegan los nuevos (sin esqueleto).
// Un «Reintentar» tras un error sí vuelve a mostrar la carga.
// Cada valor de la fuente arranca con una carga visible; las recargas posteriores dicen si van en silencio.
function withReload<T>(source: Observable<T>, reload: Observable<boolean>): Observable<[T, boolean]> {
  return source.pipe(switchMap((value) => reload.pipe(startWith(false), map((quiet): [T, boolean] => [value, quiet]))));
}

function quietWhen<T>(quiet: boolean): MonoTypeOperatorFunction<ViewState<T>> {
  return (source) => (quiet ? source.pipe(filter((state) => state.status !== 'loading')) : source);
}

// Datos de la semana que se muestra: la agenda de cada día de atención.
interface WeekColumn {
  date: Date;
  label: string;
  number: number;
  today: boolean;
  selected: boolean;
  agenda: DayAgenda;
}

// Diálogo abierto: nueva cita (con la franja libre elegida, si la hay), reprogramar o completar.
type AgendaDialog =
  | { kind: 'new'; day: string | null; time: string | null }
  | { kind: 'reschedule'; appointment: Appointment }
  | { kind: 'complete'; appointment: Appointment };

/**
 * Agenda del veterinario (10.17): calendario en la barra lateral, «Resumen del día» y línea de tiempo
 * (Día o Semana) del día elegido, y el detalle de la visita con sus acciones (S16.7–S16.9). Las citas
 * vienen del API y las franjas, del horario de la clínica; las canceladas dejan su franja libre. El día,
 * la vista y la cita viven en la URL (`?dia`, `?vista`, `?cita`). Cada fuente carga y falla por separado;
 * tras cada cambio se recargan las citas.
 */
@Component({
  selector: 'app-vet-agenda',
  imports: [
    DatePipe,
    FormsModule,
    ButtonModule,
    SelectButtonModule,
    SkeletonModule,
    TagModule,
    TooltipModule,
    LucideChevronLeft,
    LucideChevronRight,
    LucideClock,
    LucidePlus,
    LucideStethoscope,
    AppointmentDialogComponent,
    AvatarComponent,
    CardComponent,
    CompleteDialogComponent,
    DaySummaryComponent,
    DefinitionListComponent,
    DetailPanelComponent,
    EmptyStateComponent,
    InlineErrorComponent,
    MonthCalendarComponent,
    PageHeaderComponent,
    ShellSidebarDirective,
    SkeletonComponent,
    StickerComponent,
    WeekSummaryComponent,
  ],
  templateUrl: './agenda.component.html',
  styleUrl: './agenda.component.css',
})
export class AgendaComponent {
  private readonly auth = inject(AuthService);
  // Id del veterinario de la sesión. Va antes de las cargas: toSignal se suscribe al construir el componente.
  protected readonly vetIdValue = computed(() => {
    const user = this.auth.user();
    return user?.role === 'VETERINARIAN' ? user.id : null;
  });
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly petService = inject(PetService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly appointmentService = inject(AppointmentService);
  private readonly clinicService = inject(ClinicService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly clock = inject(CLOCK);
  private readonly shell = inject(ShellSidebarService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly viewOptions = VIEW_OPTIONS;
  protected readonly defaultSpecies = Species.DOG;
  protected readonly skeletonRows = [0, 1, 2, 3, 4, 5];
  protected readonly outcomeLabels = OUTCOME_LABELS;

  // Reloj de la página: avanza cada 30 s para mover las franjas pasadas y habilitar acciones.
  protected readonly now = signal(this.clock());
  protected readonly today = computed(() => startOfDay(this.now()), { equal: sameTime });

  // Estado de la URL: la fuente de verdad del día, la vista y la cita elegida.
  private readonly queryParams = toSignal(this.route.queryParamMap, { initialValue: this.route.snapshot.queryParamMap });
  protected readonly state = computed(() => parseAgendaParams(this.queryParams(), this.today()), {
    equal: (a, b) => sameTime(a.day, b.day) && a.view === b.view && a.appointmentId === b.appointmentId,
  });
  protected readonly day = computed(() => this.state().day, { equal: sameTime });
  protected readonly view = computed(() => this.state().view);

  // Mes que muestra el calendario: sigue al día elegido y las flechas lo mueven sin elegir otro día.
  protected readonly visibleMonth = linkedSignal<Date, Date>({
    source: this.day,
    computation: (day) => new Date(day.getFullYear(), day.getMonth(), 1),
    equal: sameTime,
  });

  // Cada recarga dice si es silenciosa (tras un cambio) o muestra la carga (primera vez o «Reintentar»).
  private readonly agendaReload = new Subject<boolean>();
  private readonly monthReload = new Subject<boolean>();
  private readonly weekReload = new Subject<boolean>();
  private readonly summaryReload = new Subject<boolean>();
  private readonly petsReload = new Subject<boolean>();
  private readonly treatmentsRetry = new BehaviorSubject<void>(undefined);
  private readonly clinicRetry = new BehaviorSubject<void>(undefined);

  // Horario de la clínica: dibuja las franjas de cada día.
  private readonly clinic$ = this.clinicService.getClinic();
  protected readonly clinic = toSignal(
    this.clinicRetry.pipe(switchMap(() => this.clinic$.pipe(toViewState<Clinic>()))),
    { initialValue: LOADING as ViewState<Clinic> },
  );
  protected readonly clinicData = computed(() => {
    const state = this.clinic();
    return state.status === 'ready' ? state.data : null;
  });

  // Citas del día elegido.
  protected readonly agenda = toSignal(
    withReload(toObservable(this.day), this.agendaReload).pipe(
      switchMap(([day, quiet]) => this.loadDays(day, 1).pipe(map(([agenda]) => agenda), toViewState<DayAgenda>(), quietWhen(quiet))),
    ),
    { initialValue: LOADING as ViewState<DayAgenda> },
  );

  // Agenda de los seis días de la semana; solo se pide con la vista «Semana».
  private readonly weekRequest = computed(() => (this.view() === 'semana' ? weekDays(this.day())[0] : null), {
    equal: sameOptionalTime,
  });
  protected readonly weekAgendas = toSignal(
    withReload(toObservable(this.weekRequest), this.weekReload).pipe(
      switchMap(([start, quiet]) =>
        start
          ? this.loadDays(start, weekDays(start).length).pipe(toViewState<DayAgenda[]>(), quietWhen(quiet))
          : of(LOADING as ViewState<DayAgenda[]>),
      ),
    ),
    { initialValue: LOADING as ViewState<DayAgenda[]> },
  );

  // Puntos del calendario: citas por día del mes visible.
  protected readonly month = toSignal(
    withReload(toObservable(this.visibleMonth), this.monthReload).pipe(
      switchMap(([month, quiet]) => {
        const { from, days } = monthRange(month);
        return this.loadCounts(from, days).pipe(toViewState<DayCount[]>(), quietWhen(quiet));
      }),
    ),
    { initialValue: LOADING as ViewState<DayCount[]> },
  );

  // «Esta semana»: la semana de hoy (lunes a domingo), no la del día elegido.
  private readonly weekSource = toSignal(
    withReload(toObservable(this.today), this.summaryReload).pipe(
      switchMap(([today, quiet]) => this.loadCounts(mondayOf(today), 7).pipe(toViewState<DayCount[]>(), quietWhen(quiet))),
    ),
    { initialValue: LOADING as ViewState<DayCount[]> },
  );

  // Datos reales: mascotas y tratamientos del veterinario fallan por separado.
  protected readonly pets = toSignal(
    this.petsReload.pipe(startWith(false), switchMap((quiet) => this.loadPets().pipe(toViewState<Pet[]>(), quietWhen(quiet)))),
    { initialValue: LOADING as ViewState<Pet[]> },
  );
  protected readonly treatments = toSignal(
    this.treatmentsRetry.pipe(switchMap(() => this.loadTreatments().pipe(toViewState<Treatment[]>()))),
    { initialValue: LOADING as ViewState<Treatment[]> },
  );

  protected readonly agendaSkeleton = skeletonVisible(computed(() => this.agenda().status === 'loading'));
  protected readonly weekSkeleton = skeletonVisible(computed(() => this.weekAgendas().status === 'loading'));
  protected readonly petsSkeleton = skeletonVisible(computed(() => this.pets().status === 'loading'));
  protected readonly treatmentsSkeleton = skeletonVisible(computed(() => this.treatments().status === 'loading'));

  protected readonly subtitle = computed(() => {
    const state = this.agenda();
    return agendaSubtitle(this.day(), state.status === 'ready' ? state.data : null);
  });
  protected readonly monthTitle = computed(() => monthTitleEs(this.visibleMonth()));
  protected readonly monthCounts = computed(() => {
    const state = this.month();
    return state.status === 'ready' ? state.data : null;
  });

  protected readonly weekView = computed<ViewState<WeekSummary>>(() => {
    const state = this.weekSource();
    return state.status === 'ready' ? { status: 'ready', data: weekSummary(state.data) } : state;
  });

  protected readonly timeline = computed(() => {
    const state = this.agenda();
    return state.status === 'ready' ? buildDayTimeline(state.data, this.now()) : null;
  });
  protected readonly rows = computed(() => timelineRows(this.timeline()?.segments ?? []));
  protected readonly slotTimes = computed(() => {
    const state = this.agenda();
    return state.status === 'ready' ? state.data.slots.map((slot) => slot.time) : [];
  });

  protected readonly weekColumns = computed<WeekColumn[] | null>(() => {
    const state = this.weekAgendas();
    if (state.status !== 'ready') {
      return null;
    }
    const selected = this.day();
    const today = this.today();
    return state.data.map((agenda) => ({
      date: agenda.date,
      label: dayLabelEs(agenda.date),
      number: agenda.date.getDate(),
      today: isSameDay(agenda.date, today),
      selected: isSameDay(agenda.date, selected),
      agenda,
    }));
  });

  // Cita elegida: la de la URL; sin ella (o si ya no existe) la inicial. La hora no se sigue: la
  // elegida solo cambia cuando cambia la agenda, no cada 30 s.
  protected readonly selected = computed<Appointment | null>(() => {
    const state = this.agenda();
    const id = this.state().appointmentId;
    return state.status === 'ready' ? resolveSelection(state.data, id, untracked(this.now)) : null;
  });

  // Mascota real de la cita elegida; null si es provisional o ya no está en la lista.
  protected readonly pet = computed<Pet | null>(() => {
    const selected = this.selected();
    const state = this.pets();
    return selected && selected.petId !== null && state.status === 'ready'
      ? (state.data.find((p) => p.id === selected.petId) ?? null)
      : null;
  });
  protected readonly detailHue = computed(() => {
    const selected = this.selected();
    return selected ? this.hueOf(selected) : 'honey';
  });
  protected readonly detailSubtitle = computed(() => {
    const selected = this.selected();
    if (selected?.petId === null) {
      return provisionalPetLine(selected);
    }
    const pet = this.pet();
    return pet ? petSummaryLine(pet) : null;
  });
  protected readonly fullLink = computed(() => {
    const selected = this.selected();
    return selected?.petId != null ? ['/veterinario/mascotas', selected.petId] : null;
  });
  protected readonly petStatusTag = computed(() => {
    const pet = this.pet();
    return pet ? petStatus(pet) : null;
  });
  protected readonly lastVisit = computed(() => {
    const selected = this.selected();
    const state = this.treatments();
    return selected?.petId != null && state.status === 'ready' ? lastVisitLabel(state.data, selected.petId) : null;
  });
  protected readonly statusTag = computed(() => {
    const selected = this.selected();
    return selected ? appointmentStatus(selected) : null;
  });

  // Acciones de la cita elegida según su estado y la hora (S16.7).
  protected readonly actions = computed(() => {
    const visit = this.selected();
    const now = this.now();
    if (!visit) {
      return null;
    }
    return {
      scheduled: visit.status === 'SCHEDULED',
      inProgress: visit.status === 'IN_PROGRESS',
      canStart: canStart(visit, now),
      startHint: startHint(visit, now),
      canNoShow: canMarkNoShow(visit, now),
      registerPet: needsPetRegistration(visit),
      completeLabel: completionKind(visit) === 'consultation' ? 'Completar consulta' : 'Completar cita',
    };
  });
  // Acción en curso sobre una cita (iniciar o «No asistió»): evita el doble clic.
  protected readonly busy = signal(false);

  protected readonly dialog = signal<AgendaDialog | null>(null);

  // Detalle como cajón por debajo de 1080 px: se abre al elegir una cita.
  protected readonly panelOpen = signal(false);
  protected readonly withPanel = computed(() => this.shell.layout() === 'expanded');

  constructor() {
    const timer = setInterval(() => this.now.set(this.clock()), CLOCK_TICK_MS);
    this.destroyRef.onDestroy(() => clearInterval(timer));
  }

  protected selectDay(date: Date): void {
    this.go({ day: date, appointmentId: null });
  }

  protected selectAppointment(appointment: Appointment): void {
    this.go({ appointmentId: appointment.id }, true);
    this.panelOpen.set(true);
  }

  // Desde la vista de semana: elige el día y la cita y vuelve a «Día».
  protected openFromWeek(date: Date, appointment: Appointment | null): void {
    this.go({ day: date, view: 'dia', appointmentId: appointment?.id ?? null });
    if (appointment) {
      this.panelOpen.set(true);
    }
  }

  protected setView(view: AgendaView): void {
    this.go({ view });
  }

  protected shiftMonth(months: number): void {
    this.visibleMonth.set(addMonths(this.visibleMonth(), months));
  }

  protected showMonth(date: Date): void {
    this.visibleMonth.set(new Date(date.getFullYear(), date.getMonth(), 1));
  }

  // ---------- Acciones ----------

  /** «Nueva cita»: desde el encabezado sin franja, o desde una franja libre con su día y hora. */
  protected newAppointment(time: string | null = null): void {
    this.dialog.set({ kind: 'new', day: time ? dayKey(this.day()) : null, time });
  }

  protected reschedule(appointment: Appointment): void {
    this.dialog.set({ kind: 'reschedule', appointment });
  }

  protected complete(appointment: Appointment): void {
    this.dialog.set({ kind: 'complete', appointment });
  }

  protected closeDialog(): void {
    this.dialog.set(null);
  }

  // Tras guardar en un diálogo: la cita queda elegida en su día.
  protected onSaved(appointment: Appointment): void {
    const sameDay = isSameDay(appointment.start, this.day());
    this.go({ day: startOfDay(appointment.start), appointmentId: appointment.id }, sameDay);
    this.refreshAll();
  }

  protected start(appointment: Appointment): void {
    this.runAction(this.appointmentService.start(appointment.id), 'No se pudo iniciar la consulta', (result) =>
      this.notifications.success('Consulta iniciada', `La consulta de ${result.petName} está en curso.`),
    );
  }

  protected markNoShow(appointment: Appointment): void {
    this.confirmation.confirm({
      header: `¿Marcar que ${appointment.petName} no asistió?`,
      message: `La cita de las ${formatTime(appointment.start)} quedará como «No asistió» y no se podrá iniciar.`,
      acceptLabel: 'No asistió',
      rejectLabel: 'Volver',
      rejectButtonProps: { outlined: true, severity: 'secondary' },
      acceptButtonProps: { severity: 'danger' },
      accept: () =>
        this.runAction(this.appointmentService.noShow(appointment.id), 'No se pudo marcar la cita', (result) =>
          this.notifications.success('Cita marcada', `La cita de ${result.petName} quedó como «No asistió».`),
        ),
    });
  }

  protected cancel(appointment: Appointment): void {
    this.confirmation.confirm({
      header: `¿Cancelar la cita de ${appointment.petName}?`,
      message: `La franja de las ${formatTime(appointment.start)} quedará libre y avisaremos a ${appointment.ownerName} por correo.`,
      acceptLabel: 'Cancelar cita',
      rejectLabel: 'Mantener cita',
      rejectButtonProps: { outlined: true, severity: 'secondary' },
      acceptButtonProps: { severity: 'danger' },
      accept: () =>
        this.runAction(this.appointmentService.cancel(appointment.id), 'No se pudo cancelar la cita', (result) =>
          this.notifications.success('Cita cancelada', `La cita de ${result.petName} quedó cancelada.`),
        ),
    });
  }

  // «Registrar mascota»: el formulario de mascota con los datos provisionales de la cita.
  protected registerPet(appointment: Appointment): void {
    void this.router.navigate(['/veterinario/mascotas/add'], { queryParams: { cita: appointment.id } });
  }

  // ---------- Reintentos ----------

  protected retryAgenda(): void {
    this.clinicRetry.next();
    this.agendaReload.next(false);
  }

  protected retryWeekAgendas(): void {
    this.weekReload.next(false);
  }

  protected retryMonth(): void {
    this.monthReload.next(false);
  }

  protected retrySummary(): void {
    this.summaryReload.next(false);
  }

  protected retryPets(): void {
    this.petsReload.next(false);
  }

  protected retryTreatments(): void {
    this.treatmentsRetry.next();
  }

  protected time(appointment: Appointment): string {
    return formatTime(appointment.start);
  }

  protected reason(appointment: Appointment): string {
    return reasonLabel(appointment);
  }

  protected minutes(appointment: Appointment): number {
    return Math.round((appointment.end.getTime() - appointment.start.getTime()) / 60_000);
  }

  protected appointmentStatus(appointment: Appointment) {
    return appointmentStatus(appointment);
  }

  // Tono de la mascota; una provisional aún no tiene id, así que toma el de la cita.
  protected hueOf(appointment: Appointment) {
    return identityHue(appointment.petId ?? appointment.id);
  }

  protected isSelected(appointment: Appointment): boolean {
    return this.selected()?.id === appointment.id;
  }

  private runAction(request$: Observable<Appointment>, failure: string, done: (result: Appointment) => void): void {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    request$
      .pipe(
        finalize(() => this.busy.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (result) => {
          done(result);
          this.refreshAll();
        },
        error: (error: unknown) => {
          this.notifications.error(failure, error);
          // La cita pudo cambiar en otra pestaña: se recarga para mostrar su estado real.
          this.refreshAll();
        },
      });
  }

  // Recarga en silencio todo lo que depende de las citas (y las mascotas, que cambian con el alta o el ingreso).
  private refreshAll(): void {
    this.now.set(this.clock());
    this.agendaReload.next(true);
    this.weekReload.next(true);
    this.monthReload.next(true);
    this.summaryReload.next(true);
    this.petsReload.next(true);
  }

  private go(patch: Partial<AgendaState>, replaceUrl = false): void {
    const next: AgendaState = { ...this.state(), ...patch };
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: agendaQueryParams(next, this.today()),
      replaceUrl,
    });
  }

  // Agenda de `count` días seguidos desde `from`, con una sola petición al API.
  private loadDays(from: Date, count: number): Observable<DayAgenda[]> {
    const id = this.vetIdValue();
    if (id === null) {
      return throwError(() => new Error(NOT_A_VET));
    }
    const to = addDays(from, count - 1);
    return forkJoin({
      clinic: this.clinic$,
      appointments: this.appointmentService.getVeterinarianAgenda(id, dayKey(from), dayKey(to)),
    }).pipe(
      map(({ clinic, appointments }) =>
        Array.from({ length: count }, (_, i) => buildDayAgenda(addDays(from, i), appointments, clinic)),
      ),
    );
  }

  // Citas por día de un rango; si el horario no carga, los domingos cuentan como cerrados.
  private loadCounts(from: Date, days: number): Observable<DayCount[]> {
    const id = this.vetIdValue();
    if (id === null) {
      return throwError(() => new Error(NOT_A_VET));
    }
    return forkJoin({
      clinic: this.clinic$.pipe(catchError(() => of(null))),
      appointments: this.appointmentService.getVeterinarianAgenda(id, dayKey(from), dayKey(addDays(from, days - 1))),
    }).pipe(map(({ clinic, appointments }) => countsByDay(appointments, from, days, clinic)));
  }

  private loadPets(): Observable<Pet[]> {
    return this.vetIdValue() === null ? throwError(() => new Error(NOT_A_VET)) : this.petService.getAll();
  }

  private loadTreatments(): Observable<Treatment[]> {
    const id = this.vetIdValue();
    return id === null ? throwError(() => new Error(NOT_A_VET)) : this.veterinarianService.getTreatmentsByVeterinarianId(id);
  }
}
