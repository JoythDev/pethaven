import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { LucideClock, LucidePlus } from '@lucide/angular';
import { BehaviorSubject, Observable, forkJoin, map, switchMap, throwError } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';

import { buildDayTimeline } from '../../../core/day-timeline/day-timeline';
import { formatTime, startOfDay } from '../../../core/demo/demo-date';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { petStatus } from '../../../core/status/pet-status';
import { greetingFor } from '../../../core/ui/greeting';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import {
  HERO_FALLBACK,
  RecentPatient,
  firstName,
  heroCopy,
  longDateEs,
  recentPatients,
  remainingAppointments,
  weekSummary,
} from '../../../core/vet-home/vet-home';
import { Appointment, DayAgenda, DayCount } from '../../../models/demo/appointment.model';
import { StockAlerts } from '../../../models/demo/stock-minimum.model';
import { Pet } from '../../../models/pet.model';
import { AuthService } from '../../../services/auth.service';
import { DEMO_NOW } from '../../../services/demo/demo-latency';
import { DemoAppointmentsService } from '../../../services/demo/demo-appointments.service';
import { DemoStockMinimumService } from '../../../services/demo/demo-stock-minimum.service';
import { PetService } from '../../../services/pet.service';
import { VeterinarianService } from '../../../services/veterinarian.service';
import { AvatarComponent } from '../../shared/avatar/avatar.component';
import { BlockComponent } from '../../shared/block/block.component';
import { CardComponent } from '../../shared/card/card.component';
import { DemoTagComponent } from '../../shared/demo-tag/demo-tag.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { HeroComponent } from '../../shared/hero/hero.component';
import { ListItemComponent } from '../../shared/list-item/list-item.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { ShellSidebarDirective } from '../../shared/shell/shell-sidebar.directive';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../shared/sticker/sticker.component';
import { StockMeterComponent } from '../../shared/stock-meter/stock-meter.component';
import { DaySummaryComponent } from './day-summary/day-summary.component';
import { InlineErrorComponent } from './inline-error.component';

// Datos reales del veterinario: pacientes recientes y las mascotas que puede atender.
interface RealData {
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
 * Inicio del veterinario (11.6): héroe con el día, «Resumen del día», citas de hoy, pacientes
 * recientes y existencias bajas. Las citas y las existencias son demo (S3); los pacientes son
 * reales. Cada fuente carga y falla por separado.
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
    BlockComponent,
    CardComponent,
    DaySummaryComponent,
    DemoTagComponent,
    EmptyStateComponent,
    HeroComponent,
    InlineErrorComponent,
    ListItemComponent,
    PageHeaderComponent,
    ShellSidebarDirective,
    SkeletonComponent,
    StickerComponent,
    StockMeterComponent,
  ],
  templateUrl: './home.component.html',
})
export class DashboardHomeComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly appointments = inject(DemoAppointmentsService);
  private readonly stockService = inject(DemoStockMinimumService);
  private readonly clock = inject(DEMO_NOW);

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
  protected readonly weekLoading = computed(() => this.weekView().status === 'loading');
  protected readonly weekSkeleton = skeletonVisible(this.weekLoading);
  protected readonly stockSkeleton = skeletonVisible(computed(() => this.stock().status === 'loading'));

  protected readonly skeletonRows = [0, 1, 2, 3];
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

  // «Esta semana» necesita la semana y las citas ya atendidas de hoy: falla si falla cualquiera.
  protected readonly weekView = computed(() => {
    const week = this.week();
    const agenda = this.agenda();
    if (week.status === 'error') {
      return week;
    }
    if (agenda.status === 'error') {
      return agenda;
    }
    if (week.status === 'ready' && agenda.status === 'ready') {
      return {
        status: 'ready',
        data: weekSummary(week.data, this.now(), agenda.data.counts.completed),
      } as const;
    }
    return LOADING;
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
    this.agendaRetry.next();
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
        recent: recentPatients(treatments, pets, RECENT_LIMIT),
        // Solo las activas: el formulario de tratamiento rechaza las inactivas.
        pets: pets.filter((pet) => pet.active).sort((a, b) => a.name.localeCompare(b.name, 'es')),
      })),
    );
  }

  private loadAgenda(): Observable<DayAgenda> {
    const id = this.vetId();
    return id === null
      ? throwError(() => new Error(NOT_A_VET))
      : this.appointments.getDayAgenda(id, startOfDay(this.clock()));
  }

  private loadWeek(): Observable<DayCount[]> {
    const id = this.vetId();
    return id === null
      ? throwError(() => new Error(NOT_A_VET))
      : this.appointments.getWeekOverview(id, startOfDay(this.clock()));
  }
}
