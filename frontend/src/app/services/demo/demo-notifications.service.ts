import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, forkJoin, map, of, tap } from 'rxjs';
import { generateDashboardPeriods } from '../../core/demo/dashboard-periods';
import {
  administratorNotifications,
  clientNotifications,
  veterinarianNotifications,
} from '../../core/demo/notifications';
import { DemoNotification } from '../../models/demo/demo-notification.model';
import { AdministratorDashboardService } from '../administrator-dashboard.service';
import { AuthService } from '../auth.service';
import { DrugService } from '../drug.service';
import { PetService } from '../pet.service';
import { VeterinarianService } from '../veterinarian.service';
import { DemoAppointmentsService } from './demo-appointments.service';
import { DemoCarePlanService } from './demo-care-plan.service';
import { DEMO_NOW, demoLatency } from './demo-latency';

// Demo (S14): el API no tiene notificaciones. Se generan por rol con datos reales (mascotas,
// medicamentos, veterinarios, totales) y con el resto de los servicios demo; cuáles se leyeron
// vive solo en memoria. Hay que llamar a `refresh()` para (re)generar la lista.
@Injectable({ providedIn: 'root' })
export class DemoNotificationsService {
  private readonly auth = inject(AuthService);
  private readonly petService = inject(PetService);
  private readonly drugService = inject(DrugService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly dashboardService = inject(AdministratorDashboardService);
  private readonly appointments = inject(DemoAppointmentsService);
  private readonly carePlan = inject(DemoCarePlanService);
  private readonly now = inject(DEMO_NOW);

  private readonly base = signal<DemoNotification[]>([]);
  private readonly readIds = signal<ReadonlySet<string>>(new Set());

  // De la más reciente a la más antigua, con la marca de leída ya aplicada.
  readonly notifications = computed(() =>
    this.base().map((n) => (this.readIds().has(n.id) ? { ...n, unread: false } : n)),
  );
  readonly unreadCount = computed(() => this.notifications().filter((n) => n.unread).length);
  // Las tres más recientes: menú de la campana y bloque de la barra lateral.
  readonly recent = computed(() => this.notifications().slice(0, 3));

  // Genera la lista del usuario con sesión. Si algún dato real no se puede pedir, queda vacía:
  // un aviso demo nunca debe romper la interfaz.
  refresh(): Observable<DemoNotification[]> {
    return this.generate().pipe(
      catchError(() => of([] as DemoNotification[])),
      demoLatency(),
      tap((list) => this.base.set(list)),
    );
  }

  markAsRead(id: string): void {
    this.readIds.update((ids) => new Set(ids).add(id));
  }

  markAllAsRead(): void {
    this.readIds.set(new Set(this.base().map((n) => n.id)));
  }

  private generate(): Observable<DemoNotification[]> {
    const user = this.auth.user();
    if (!user) {
      return of([]);
    }
    const today = this.now();
    switch (user.role) {
      case 'OWNER':
        return this.appointments.loadOwnerContext(user.id).pipe(
          map((context) =>
            clientNotifications({
              today,
              ownerId: user.id,
              pets: context.pets,
              upcoming: this.appointments.ownerAppointmentsFrom(context.pets, context.vets).upcoming,
              care: context.petTreatments.map(({ pet, treatments }) => ({
                pet,
                items: this.carePlan.planFrom(pet, treatments),
              })),
              doneCareIds: this.carePlan.doneIds(),
            }),
          ),
        );
      case 'VETERINARIAN':
        return forkJoin({
          vet: this.veterinarianService.getById(user.id),
          pets: this.petService.getAll(),
          drugs: this.drugService.getAll(),
        }).pipe(
          map(({ vet, pets, drugs }) =>
            veterinarianNotifications({
              today,
              todayAppointments: this.appointments.agendaFor(vet, pets, today).appointments,
              pets,
              drugs,
            }),
          ),
        );
      case 'ADMIN':
        return forkJoin({
          drugs: this.drugService.getAll(),
          veterinarians: this.veterinarianService.getAll(),
          summary: this.dashboardService.getSummary(),
        }).pipe(
          map(({ drugs, veterinarians, summary }) =>
            administratorNotifications({
              today,
              drugs,
              veterinarians,
              periods: generateDashboardPeriods(summary, today),
            }),
          ),
        );
    }
  }
}
