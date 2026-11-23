import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, defer, forkJoin, map, of, switchMap, tap } from 'rxjs';
import { splitAppointments } from '../../core/client-appointments/client-appointments';
import { generateDashboardPeriods } from '../../core/demo/dashboard-periods';
import {
  administratorNotifications,
  clientNotifications,
  veterinarianNotifications,
} from '../../core/demo/notifications';
import { DemoNotification } from '../../models/demo/demo-notification.model';
import { ClienteTreatment } from '../../models/treatment-cliente.model';
import { agendaAppointments } from '../../core/vet-agenda/vet-agenda';
import { AppointmentService } from '../appointment.service';
import { AdministratorDashboardService } from '../administrator-dashboard.service';
import { AuthService } from '../auth.service';
import { DrugService } from '../drug.service';
import { PetService } from '../pet.service';
import { VeterinarianService } from '../veterinarian.service';
import { ClienteTreatmentService } from '../treatment-cliente.service';
import { DemoCarePlanService } from './demo-care-plan.service';
import { dayKey } from '../../core/demo/demo-date';
import { DEMO_NOW, demoLatency } from './demo-latency';

// Demo (S14): el API no tiene notificaciones. Se generan por rol con datos reales (mascotas, citas,
// medicamentos, veterinarios, totales) y con el resto de los servicios demo; cuáles se leyeron
// vive solo en memoria. Hay que llamar a `refresh()` para (re)generar la lista.
@Injectable({ providedIn: 'root' })
export class DemoNotificationsService {
  private readonly auth = inject(AuthService);
  private readonly petService = inject(PetService);
  private readonly drugService = inject(DrugService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly dashboardService = inject(AdministratorDashboardService);
  private readonly appointmentService = inject(AppointmentService);
  private readonly treatmentService = inject(ClienteTreatmentService);
  private readonly carePlan = inject(DemoCarePlanService);
  private readonly now = inject(DEMO_NOW);

  private readonly base = signal<DemoNotification[]>([]);
  private readonly readIds = signal<ReadonlySet<string>>(new Set());
  // Usuario y día a los que pertenecen las marcas de leída; si cambian, se reinician.
  private readKey = '';

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
    return defer(() => {
      const user = this.auth.user();
      const key = user ? `${user.role}-${user.id}-${dayKey(this.now())}` : '';
      if (key !== this.readKey) {
        this.readKey = key;
        this.readIds.set(new Set());
      }
      return this.generate();
    }).pipe(
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
        return forkJoin({
          pets: this.petService.getByOwnerId(user.id),
          appointments: this.appointmentService.getByOwnerId(user.id),
        }).pipe(
          switchMap(({ pets, appointments }) =>
            // Un fallo en los tratamientos de una mascota no tumba la lista: esa mascota queda sin cuidados.
            (pets.length
              ? forkJoin(
                  pets.map((pet) =>
                    this.treatmentService.getByPetId(pet.id).pipe(catchError(() => of<ClienteTreatment[]>([]))),
                  ),
                )
              : of<ClienteTreatment[][]>([])
            ).pipe(
              map((lists) =>
                clientNotifications({
                  today,
                  ownerId: user.id,
                  pets,
                  upcoming: splitAppointments(appointments, today).upcoming,
                  care: pets.map((pet, i) => ({ pet, items: this.carePlan.planFrom(pet, lists[i]) })),
                  doneCareIds: this.carePlan.doneIds(),
                }),
              ),
            ),
          ),
        );
      case 'VETERINARIAN':
        return forkJoin({
          appointments: this.appointmentService.getVeterinarianAgenda(user.id, dayKey(today), dayKey(today)),
          pets: this.petService.getAll(),
          drugs: this.drugService.getAll(),
        }).pipe(
          map(({ appointments, pets, drugs }) =>
            veterinarianNotifications({
              today,
              todayAppointments: agendaAppointments(appointments),
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
      default:
        return of([]);
    }
  }
}
