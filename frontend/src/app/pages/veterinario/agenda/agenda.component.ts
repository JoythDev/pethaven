import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, linkedSignal, signal, untracked } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideChevronLeft, LucideChevronRight, LucideClock, LucidePlus, LucideStethoscope, LucideTriangleAlert } from '@lucide/angular';
import { BehaviorSubject, Observable, combineLatest, forkJoin, map, of, switchMap, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { buildDayTimeline } from '../../../core/day-timeline/day-timeline';
import { addMonths, formatTime, isSameDay, startOfDay } from '../../../core/demo/demo-date';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { identityHue } from '../../../core/identity/identity';
import { petStatus } from '../../../core/status/pet-status';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import {
  AgendaState,
  AgendaView,
  agendaQueryParams,
  agendaSubtitle,
  appointmentStatusTag,
  dayLabelEs,
  lastVisitLabel,
  monthTitleEs,
  parseAgendaParams,
  petSummaryLine,
  resolveSelection,
  timelineRows,
  weekDays,
} from '../../../core/vet-agenda/vet-agenda';
import { WeekSummary, weekSummary } from '../../../core/vet-home/vet-home';
import { Appointment, DayAgenda, DayCount } from '../../../models/demo/appointment.model';
import { Pet, Species } from '../../../models/pet.model';
import { Treatment } from '../../../models/treatment.model';
import { AuthService } from '../../../services/auth.service';
import { DemoAppointmentsService } from '../../../services/demo/demo-appointments.service';
import { DEMO_NOW } from '../../../services/demo/demo-latency';
import { PetService } from '../../../services/pet.service';
import { VeterinarianService } from '../../../services/veterinarian.service';
import { AvatarComponent } from '../../shared/avatar/avatar.component';
import { CardComponent } from '../../shared/card/card.component';
import { DaySummaryComponent } from '../../shared/day-summary/day-summary.component';
import { DefinitionListComponent } from '../../shared/definition-list/definition-list.component';
import { DemoTagComponent } from '../../shared/demo-tag/demo-tag.component';
import { DetailPanelComponent } from '../../shared/detail-panel/detail-panel.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { InlineErrorComponent } from '../../shared/inline-error/inline-error.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { ShellSidebarDirective } from '../../shared/shell/shell-sidebar.directive';
import { ShellSidebarService } from '../../shared/shell/shell-sidebar.service';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../shared/sticker/sticker.component';
import { WeekSummaryComponent } from '../../shared/week-summary/week-summary.component';
import { MonthCalendarComponent } from './month-calendar/month-calendar.component';

const LOADING = { status: 'loading' } as const;
const NOT_A_VET = 'No hay un veterinario autenticado.';
const CLOCK_TICK_MS = 30_000;
const DEMO_SUMMARY = 'Función demo';

const VIEW_OPTIONS: { label: string; value: AgendaView }[] = [
  { label: 'Día', value: 'dia' },
  { label: 'Semana', value: 'semana' },
];

const sameTime = (a: Date, b: Date): boolean => a.getTime() === b.getTime();
const sameOptionalTime = (a: Date | null, b: Date | null): boolean => (a && b ? sameTime(a, b) : a === b);

// Datos de la semana que se muestra: la agenda de cada día de atención.
interface WeekColumn {
  date: Date;
  label: string;
  number: number;
  today: boolean;
  selected: boolean;
  agenda: DayAgenda;
}

// Semana de hoy y citas ya atendidas hoy: con ellas «Esta semana» calcula el avance.
interface WeekSource {
  days: DayCount[];
  completedToday: number;
}

/**
 * Agenda del veterinario (10.17, demo): calendario en la barra lateral, «Resumen del día» y línea
 * de tiempo (Día o Semana) del día elegido, y el detalle de la visita. El día, la vista y la cita
 * viven en la URL (`?dia`, `?vista`, `?cita`). Las citas son demo (S3); la mascota, su estado y la
 * última visita son reales. Cada fuente carga y falla por separado.
 */
@Component({
  selector: 'app-vet-agenda',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
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
    LucideTriangleAlert,
    AvatarComponent,
    CardComponent,
    DaySummaryComponent,
    DefinitionListComponent,
    DemoTagComponent,
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
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly petService = inject(PetService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly appointments = inject(DemoAppointmentsService);
  private readonly messages = inject(MessageService);
  private readonly clock = inject(DEMO_NOW);
  private readonly shell = inject(ShellSidebarService);

  protected readonly viewOptions = VIEW_OPTIONS;
  protected readonly defaultSpecies = Species.DOG;
  protected readonly skeletonRows = [0, 1, 2, 3, 4, 5];

  // Reloj de la página: avanza cada 30 s para mover las franjas pasadas.
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

  private readonly agendaRetry = new BehaviorSubject<void>(undefined);
  private readonly monthRetry = new BehaviorSubject<void>(undefined);
  private readonly weekRetry = new BehaviorSubject<void>(undefined);
  private readonly summaryRetry = new BehaviorSubject<void>(undefined);
  private readonly petsRetry = new BehaviorSubject<void>(undefined);
  private readonly treatmentsRetry = new BehaviorSubject<void>(undefined);

  // Citas demo del día elegido.
  protected readonly agenda = toSignal(
    combineLatest([toObservable(this.day), this.agendaRetry]).pipe(
      switchMap(([day]) => this.loadAgenda(day).pipe(toViewState<DayAgenda>())),
    ),
    { initialValue: LOADING as ViewState<DayAgenda> },
  );

  // Agenda de los seis días de la semana; solo se pide con la vista «Semana».
  private readonly weekRequest = computed(() => (this.view() === 'semana' ? weekDays(this.day())[0] : null), {
    equal: sameOptionalTime,
  });
  protected readonly weekAgendas = toSignal(
    combineLatest([toObservable(this.weekRequest), this.weekRetry]).pipe(
      switchMap(([start]) =>
        start ? this.loadWeekAgendas(start).pipe(toViewState<DayAgenda[]>()) : of(LOADING as ViewState<DayAgenda[]>),
      ),
    ),
    { initialValue: LOADING as ViewState<DayAgenda[]> },
  );

  // Puntos del calendario: citas por día del mes visible.
  protected readonly month = toSignal(
    combineLatest([toObservable(this.visibleMonth), this.monthRetry]).pipe(
      switchMap(([month]) => this.loadMonth(month).pipe(toViewState<DayCount[]>())),
    ),
    { initialValue: LOADING as ViewState<DayCount[]> },
  );

  // «Esta semana»: la semana de hoy, no la del día elegido.
  private readonly weekSource = toSignal(
    combineLatest([toObservable(this.today), this.summaryRetry]).pipe(
      switchMap(([today]) => this.loadWeekSource(today).pipe(toViewState<WeekSource>())),
    ),
    { initialValue: LOADING as ViewState<WeekSource> },
  );

  // Datos reales: mascotas y tratamientos del veterinario fallan por separado.
  protected readonly pets = toSignal(
    this.petsRetry.pipe(switchMap(() => this.loadPets().pipe(toViewState<Pet[]>()))),
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
    return state.status === 'ready'
      ? { status: 'ready', data: weekSummary(state.data.days, this.now(), state.data.completedToday) }
      : state;
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

  // Mascota real de la cita elegida (puede faltar si ya no está en la lista).
  protected readonly pet = computed<Pet | null>(() => {
    const selected = this.selected();
    const state = this.pets();
    return selected && state.status === 'ready' ? (state.data.find((p) => p.id === selected.petId) ?? null) : null;
  });
  protected readonly petInactive = computed(() => this.pet()?.active === false);
  protected readonly detailHue = computed(() => {
    const selected = this.selected();
    return selected ? identityHue(selected.petId) : 'honey';
  });
  protected readonly detailSubtitle = computed(() => {
    const pet = this.pet();
    return pet ? petSummaryLine(pet) : null;
  });
  protected readonly petStatusTag = computed(() => {
    const pet = this.pet();
    return pet ? petStatus(pet) : null;
  });
  protected readonly lastVisit = computed(() => {
    const selected = this.selected();
    const state = this.treatments();
    return selected && state.status === 'ready' ? lastVisitLabel(state.data, selected.petId) : null;
  });
  protected readonly statusTag = computed(() => {
    const selected = this.selected();
    return selected ? appointmentStatusTag(selected.status) : null;
  });

  // Detalle como cajón por debajo de 1080 px: se abre al elegir una cita.
  protected readonly panelOpen = signal(false);
  protected readonly withPanel = computed(() => this.shell.layout() === 'expanded');

  constructor() {
    const timer = setInterval(() => this.now.set(this.clock()), CLOCK_TICK_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
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

  // Un solo aviso a la vez (10.20): el nuevo reemplaza al anterior.
  protected demoAction(detail: string): void {
    this.messages.clear();
    this.messages.add({ severity: 'info', summary: DEMO_SUMMARY, detail, life: 4000 });
  }

  protected demoFreeSlot(time: string): void {
    this.demoAction(`Agendar a las ${time} llegará con la versión final.`);
  }

  protected retryAgenda(): void {
    this.agendaRetry.next();
  }

  protected retryWeekAgendas(): void {
    this.weekRetry.next();
  }

  protected retryMonth(): void {
    this.monthRetry.next();
  }

  protected retrySummary(): void {
    this.summaryRetry.next();
  }

  protected retryPets(): void {
    this.petsRetry.next();
  }

  protected retryTreatments(): void {
    this.treatmentsRetry.next();
  }

  protected time(appointment: Appointment): string {
    return formatTime(appointment.start);
  }

  protected appointmentStatus(appointment: Appointment) {
    return appointmentStatusTag(appointment.status);
  }

  protected hueOf(appointment: Appointment) {
    return identityHue(appointment.petId);
  }

  protected isSelected(appointment: Appointment): boolean {
    return this.selected()?.id === appointment.id;
  }

  private go(patch: Partial<AgendaState>, replaceUrl = false): void {
    const next: AgendaState = { ...this.state(), ...patch };
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: agendaQueryParams(next, this.today()),
      replaceUrl,
    });
  }

  private vetId(): number | null {
    const user = this.auth.user();
    return user?.role === 'VETERINARIAN' ? user.id : null;
  }

  private loadAgenda(day: Date): Observable<DayAgenda> {
    const id = this.vetId();
    return id === null ? throwError(() => new Error(NOT_A_VET)) : this.appointments.getDayAgenda(id, day);
  }

  private loadWeekAgendas(start: Date): Observable<DayAgenda[]> {
    const id = this.vetId();
    return id === null
      ? throwError(() => new Error(NOT_A_VET))
      : forkJoin(weekDays(start).map((date) => this.appointments.getDayAgenda(id, date)));
  }

  private loadMonth(month: Date): Observable<DayCount[]> {
    const id = this.vetId();
    return id === null ? throwError(() => new Error(NOT_A_VET)) : this.appointments.getMonthOverview(id, month);
  }

  private loadWeekSource(today: Date): Observable<WeekSource> {
    const id = this.vetId();
    if (id === null) {
      return throwError(() => new Error(NOT_A_VET));
    }
    return forkJoin({
      days: this.appointments.getWeekOverview(id, today),
      agenda: this.appointments.getDayAgenda(id, today),
    }).pipe(map(({ days, agenda }) => ({ days, completedToday: agenda.counts.completed })));
  }

  private loadPets(): Observable<Pet[]> {
    return this.vetId() === null ? throwError(() => new Error(NOT_A_VET)) : this.petService.getAll();
  }

  private loadTreatments(): Observable<Treatment[]> {
    const id = this.vetId();
    return id === null ? throwError(() => new Error(NOT_A_VET)) : this.veterinarianService.getTreatmentsByVeterinarianId(id);
  }
}
