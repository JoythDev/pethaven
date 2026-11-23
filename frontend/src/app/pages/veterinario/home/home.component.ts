import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { LucideClock, LucidePlus } from '@lucide/angular';
import { BehaviorSubject, Observable, catchError, forkJoin, map, of, switchMap, throwError } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';

import { buildDayTimeline } from '../../../core/day-timeline/day-timeline';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { petStatus } from '../../../core/status/pet-status';
import { greetingFor } from '../../../core/ui/greeting';
import { addDays, dayKey, formatTime, startOfDay } from '../../../core/time/calendar';
import { CLOCK } from '../../../core/time/clock';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import { buildDayAgenda, countsByDay, mondayOf } from '../../../core/vet-agenda/vet-agenda';
import {
  HERO_FALLBACK,
  RecentPatient,
  firstName,
  heroCopy,
  longDateEs,
  VetStats,
  WeekSummary,
  recentPatients,
  remainingAppointments,
  vetStats,
  weekSummary,
} from '../../../core/vet-home/vet-home';
import { APPOINTMENT_REASON_LABELS, Appointment, DayAgenda, DayCount } from '../../../models/appointment.model';
import { StockAlerts } from '../../../models/demo/stock-minimum.model';
import { Pet } from '../../../models/pet.model';
import { AuthService } from '../../../services/auth.service';
import { AppointmentService } from '../../../services/appointment.service';
import { ClinicService } from '../../../services/clinic.service';
import { DemoStockMinimumService } from '../../../services/demo/demo-stock-minimum.service';
import { PetService } from '../../../services/pet.service';
import { VeterinarianService } from '../../../services/veterinarian.service';
import { formatUnits } from '../../shared/drugs/drug-stock';
import { AvatarComponent } from '../../shared/avatar/avatar.component';
import { CardComponent } from '../../shared/card/card.component';
import { DaySummaryComponent } from '../../shared/day-summary/day-summary.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { HeroComponent } from '../../shared/hero/hero.component';
import { InlineErrorComponent } from '../../shared/inline-error/inline-error.component';
import { KpiComponent } from '../../shared/kpi/kpi.component';
import { ListItemComponent } from '../../shared/list-item/list-item.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { ShellSidebarDirective } from '../../shared/shell/shell-sidebar.directive';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../shared/sticker/sticker.component';
import { StockMeterComponent } from '../../shared/stock-meter/stock-meter.component';
import { WeekSummaryComponent } from '../../shared/week-summary/week-summary.component';

// Datos reales del veterinario: indicadores, pacientes recientes y las mascotas que puede atender.
interface RealData {
  stats: VetStats;
  recent: RecentPatient[];
  pets: Pet[];
}

const LOADING = { status: 'loading' } as const;
const NOT_A_VET = 'No hay un veterinario autenticado.';
const CLOCK_TICK_MS = 30_000;
const RECENT_LIMIT = 5;
const TODAY_LIMIT = 5;
const STOCK_LIMIT = 5;

/**
 * Inicio del veterinario (11.6): héroe con el día, «Resumen del día», cuatro indicadores reales
 * (pacientes, tratamientos, pacientes activos y última atención), citas de hoy, pacientes recientes y
 * existencias bajas. Las citas vienen del API (S16); el mínimo de existencias sigue en demo. Cada fuente
 * carga y falla por separado.
 */
@Component({
  selector: 'app-dashboard-home',
  imports: [
    RouterLink,
    ButtonModule,
    SelectModule,
    TagModule,
    LucideClock,
    LucidePlus,
    AvatarComponent,
    CardComponent,
    DaySummaryComponent,
    EmptyStateComponent,
    HeroComponent,
    InlineErrorComponent,
    KpiComponent,
    ListItemComponent,
    PageHeaderComponent,
    ShellSidebarDirective,
    SkeletonComponent,
    StickerComponent,
    StockMeterComponent,
    WeekSummaryComponent,
  ],
  templateUrl: './home.component.html',
})
export class DashboardHomeComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly appointmentService = inject(AppointmentService);
  private readonly clinicService = inject(ClinicService);
  private readonly stockService = inject(DemoStockMinimumService);
  private readonly clock = inject(CLOCK);

  // Reloj de la página: avanza cada 30 s para mover «ahora» y «Sigue».
  protected readonly now = signal(this.clock());

  private readonly realRetry = new BehaviorSubject<void>(undefined);
  private readonly agendaRetry = new BehaviorSubject<void>(undefined);
  private readonly weekRetry = new BehaviorSubject<void>(undefined);
  private readonly stockRetry = new BehaviorSubject<void>(undefined);

  protected readonly real = toSignal(
    this.realRetry.pipe(switchMap(() => this.loadReal().pipe(toViewState<RealData>()))),
    { initialValue: LOADING as ViewState<RealData> },
  );
  protected readonly agenda = toSignal(
    this.agendaRetry.pipe(switchMap(() => this.loadAgenda().pipe(toViewState<DayAgenda>()))),
    { initialValue: LOADING as ViewState<DayAgenda> },
  );
  private readonly week = toSignal(
    this.weekRetry.pipe(switchMap(() => this.loadWeek().pipe(toViewState<DayCount[]>()))),
    { initialValue: LOADING as ViewState<DayCount[]> },
  );
  protected readonly stock = toSignal(
    this.stockRetry.pipe(switchMap(() => this.stockService.getAlerts().pipe(toViewState<StockAlerts>()))),
    { initialValue: LOADING as ViewState<StockAlerts> },
  );

  protected readonly realSkeleton = skeletonVisible(computed(() => this.real().status === 'loading'));
  protected readonly agendaSkeleton = skeletonVisible(computed(() => this.agenda().status === 'loading'));
  protected readonly stockSkeleton = skeletonVisible(computed(() => this.stock().status === 'loading'));

  protected readonly skeletonRows = [0, 1, 2, 3];
  protected readonly formatUnits = formatUnits;
  protected readonly todayDate = computed(() => startOfDay(this.now()));
  protected readonly dateLabel = computed(() => longDateEs(this.now()));
  protected readonly kicker = computed(() => greetingFor(firstName(this.auth.user()?.name), this.now()));

  protected readonly timeline = computed(() => {
    const state = this.agenda();
    return state.status === 'ready' ? buildDayTimeline(state.data, this.now()) : null;
  });
  protected readonly today = computed(() => {
    const state = this.agenda();
    return state.status === 'ready' ? state.data : null;
  });
  protected readonly hero = computed(() => {
    const data = this.today();
    return data ? heroCopy(data) : HERO_FALLBACK;
  });
  protected readonly remaining = computed(() => {
    const data = this.today();
    return data ? remainingAppointments(data, this.now(), TODAY_LIMIT) : [];
  });

  // Texto del estado vacío de «Citas de hoy»: día cerrado, sin citas o ya terminadas.
  protected readonly todayEmpty = computed(() => {
    const day = this.today();
    if (day?.closed) {
      return { heading: 'La clínica no atiende hoy', description: 'Disfruta del descanso; las citas vuelven el lunes.' };
    }
    return day && day.counts.total > 0
      ? { heading: 'Terminaste las citas de hoy', description: 'No quedan más citas en tu agenda de hoy.' }
      : { heading: 'Hoy no tienes citas', description: 'Cuando agenden una, aparecerá aquí.' };
  });

  // «Esta semana»: citas de la semana de hoy (lunes a domingo) y las ya completadas.
  protected readonly weekView = computed<ViewState<WeekSummary>>(() => {
    const week = this.week();
    return week.status === 'ready' ? { status: 'ready', data: weekSummary(week.data) } : week;
  });

  protected readonly stockRows = computed(() => {
    const state = this.stock();
    return state.status === 'ready' ? [...state.data.out, ...state.data.low].slice(0, STOCK_LIMIT) : [];
  });

  constructor() {
    const timer = setInterval(() => this.now.set(this.clock()), CLOCK_TICK_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected appointmentTime(appointment: Appointment): string {
    return formatTime(appointment.start);
  }

  protected appointmentReason(appointment: Appointment): string {
    return APPOINTMENT_REASON_LABELS[appointment.reason];
  }

  // Una mascota provisional aún no tiene ficha: su cita se atiende desde la Agenda.
  protected appointmentLink(appointment: Appointment): unknown[] {
    return appointment.petId === null ? ['/veterinario/agenda'] : ['/veterinario/mascotas', appointment.petId];
  }

  protected status(pet: Pet) {
    return petStatus(pet);
  }

  protected retryReal(): void {
    this.realRetry.next();
  }

  protected retryAgenda(): void {
    this.agendaRetry.next();
  }

  protected retryWeek(): void {
    this.weekRetry.next();
  }

  protected retryStock(): void {
    this.stockRetry.next();
  }

  // «Registrar consulta»: va al formulario de tratamiento de la mascota elegida.
  protected registerTreatment(pet: Pet | null): void {
    if (pet) {
      void this.router.navigate(['/veterinario/mascotas', pet.id, 'treatments', 'add']);
    }
  }

  private vetId(): number | null {
    const user = this.auth.user();
    return user?.role === 'VETERINARIAN' ? user.id : null;
  }

  private loadReal(): Observable<RealData> {
    const id = this.vetId();
    if (id === null) {
      return throwError(() => new Error(NOT_A_VET));
    }
    return forkJoin({
      treatments: this.veterinarianService.getTreatmentsByVeterinarianId(id),
      pets: this.petService.getAll(),
    }).pipe(
      map(({ treatments, pets }) => ({
        stats: vetStats(treatments, pets),
        recent: recentPatients(treatments, pets, RECENT_LIMIT),
        // Solo las activas: el formulario de tratamiento rechaza las inactivas.
        pets: pets.filter((pet) => pet.active).sort((a, b) => a.name.localeCompare(b.name, 'es')),
      })),
    );
  }

  private loadAgenda(): Observable<DayAgenda> {
    const id = this.vetId();
    if (id === null) {
      return throwError(() => new Error(NOT_A_VET));
    }
    const today = startOfDay(this.clock());
    return forkJoin({
      clinic: this.clinicService.getClinic(),
      appointments: this.appointmentService.getVeterinarianAgenda(id, dayKey(today), dayKey(today)),
    }).pipe(map(({ clinic, appointments }) => buildDayAgenda(today, appointments, clinic)));
  }

  // Semana de hoy, de lunes a domingo; si el horario no carga, los domingos cuentan como cerrados.
  private loadWeek(): Observable<DayCount[]> {
    const id = this.vetId();
    if (id === null) {
      return throwError(() => new Error(NOT_A_VET));
    }
    const monday = mondayOf(this.clock());
    return forkJoin({
      clinic: this.clinicService.getClinic().pipe(catchError(() => of(null))),
      appointments: this.appointmentService.getVeterinarianAgenda(id, dayKey(monday), dayKey(addDays(monday, 6))),
    }).pipe(map(({ clinic, appointments }) => countsByDay(appointments, monday, 7, clinic)));
  }
}
