import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideCalendar } from '@lucide/angular';
import { BehaviorSubject, Observable, catchError, combineLatest, forkJoin, map, of, switchMap } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import { appointmentWhen } from '../../../core/client-home/client-home';
import { bookingLink, lastDiagnosis, nextAppointmentFor, petCountText } from '../../../core/client-pets/client-pets';
import { splitAppointments } from '../../../core/client-appointments/client-appointments';
import { ownerIdParam } from '../../../core/client-pets/owner-route';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { identityHue } from '../../../core/identity/identity';
import { ageLabel, formatKg } from '../../../core/pet-detail/pet-detail';
import { petStatus } from '../../../core/status/pet-status';
import { coatKitFor, speciesSticker } from '../../../core/stickers/stickers';
import { CLOCK } from '../../../core/time/clock';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import { Appointment } from '../../../models/appointment.model';
import { Pet, Species } from '../../../models/pet.model';
import { ClienteTreatment } from '../../../models/treatment-cliente.model';
import { AppointmentService } from '../../../services/appointment.service';
import { PetService } from '../../../services/pet.service';
import { ClienteTreatmentService } from '../../../services/treatment-cliente.service';
import { BlockComponent } from '../../shared/block/block.component';
import { DefinitionListComponent } from '../../shared/definition-list/definition-list.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { InlineErrorComponent } from '../../shared/inline-error/inline-error.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../shared/sticker/sticker.component';

// Lo que muestra cada tarjeta: la mascota, el diagnóstico de su último tratamiento y su próxima cita (ya como texto).
interface PetCard {
  pet: Pet;
  lastDiagnosis: string | null;
  nextAppointment: string | null;
}

const LOADING = { status: 'loading' } as const;
const OWNER_NOT_FOUND: ViewState<PetCard[]> = { status: 'error', message: 'Dueño no encontrado.' };
const SKELETON_CARDS = [0, 1, 2];

/**
 * «Mis mascotas» del cliente (11.7, S21): una tarjeta por mascota con su color de identidad, estado,
 * último tratamiento, próxima cita y los accesos «Ver ficha» y «Agendar» (solo para las mascotas en casa:
 * las hospitalizadas las agenda el veterinario). Solo lectura: no hay botón para agregar, porque el
 * veterinario registra las mascotas nuevas.
 */
@Component({
  selector: 'app-mascotas',
  imports: [
    RouterLink,
    ButtonModule,
    TagModule,
    LucideCalendar,
    BlockComponent,
    DefinitionListComponent,
    EmptyStateComponent,
    InlineErrorComponent,
    PageHeaderComponent,
    SkeletonComponent,
    StickerComponent,
  ],
  templateUrl: './mascotas.component.html',
})
export class MascotasComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly petService = inject(PetService);
  private readonly treatmentService = inject(ClienteTreatmentService);
  private readonly appointmentService = inject(AppointmentService);
  private readonly clock = inject(CLOCK);

  // :ownerId vive en una ruta ancestra; null si es inválido.
  private readonly ownerId$ = ownerIdParam(this.route);
  private readonly ownerId = toSignal(this.ownerId$, { initialValue: null });
  private readonly retry$ = new BehaviorSubject<void>(undefined);

  protected readonly state = toSignal(
    combineLatest([this.ownerId$, this.retry$]).pipe(
      switchMap(([ownerId]) => (ownerId === null ? of(OWNER_NOT_FOUND) : this.load(ownerId))),
    ),
    { initialValue: LOADING as ViewState<PetCard[]> },
  );

  protected readonly skeleton = skeletonVisible(computed(() => this.state().status === 'loading'));
  protected readonly skeletonCards = SKELETON_CARDS;

  protected readonly cards = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? state.data : [];
  });
  protected readonly subtitle = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? petCountText(state.data.length) : null;
  });
  protected readonly bookLink = computed(() => bookingLink(this.ownerId()));

  // Fotos que no cargaron: esa mascota vuelve a su sticker.
  private readonly failedPhotos = signal<ReadonlySet<number>>(new Set());

  protected readonly ageLabel = ageLabel;
  protected readonly formatKg = formatKg;
  protected readonly petStatus = petStatus;
  protected readonly hue = identityHue;

  protected speciesLabel(species: Species): string {
    return species === Species.CAT ? 'Gato' : 'Perro';
  }

  protected sticker(pet: Pet) {
    return speciesSticker(pet.species);
  }

  protected kit(pet: Pet) {
    return coatKitFor(pet.id);
  }

  protected petLink(petId: number): (string | number)[] {
    return ['/cliente', this.ownerId() ?? 'x', 'mascotas', petId];
  }

  protected showPhoto(pet: Pet): boolean {
    return !!pet.photoUrl && !this.failedPhotos().has(pet.id);
  }

  protected onPhotoError(petId: number): void {
    this.failedPhotos.update((failed) => new Set(failed).add(petId));
  }

  protected retry(): void {
    this.retry$.next();
  }

  // Las mascotas y citas del dueño y, de cada mascota, sus tratamientos. Un fallo en los tratamientos de
  // una mascota no tumba la lista: esa tarjeta queda sin último tratamiento.
  private load(ownerId: number): Observable<ViewState<PetCard[]>> {
    return forkJoin({
      pets: this.petService.getByOwnerId(ownerId),
      // Sin citas la página sigue siendo útil: las tarjetas muestran que la próxima cita no está disponible.
      appointments: this.appointmentService.getByOwnerId(ownerId).pipe(catchError(() => of(null))),
    }).pipe(
      switchMap(({ pets, appointments }) =>
        (pets.length
          ? forkJoin(pets.map((pet) => this.treatmentService.getByPetId(pet.id).pipe(catchError(() => of<ClienteTreatment[]>([])))))
          : of<ClienteTreatment[][]>([])
        ).pipe(map((treatments) => this.toCards(pets, treatments, appointments))),
      ),
      toViewState<PetCard[]>(),
    );
  }

  private toCards(pets: Pet[], treatments: ClienteTreatment[][], appointments: Appointment[] | null): PetCard[] {
    const today = this.clock();
    const upcoming = appointments ? splitAppointments(appointments, today).upcoming : [];
    return pets.map((pet, i) => {
      const next = nextAppointmentFor(pet.id, upcoming);
      return {
        pet,
        lastDiagnosis: lastDiagnosis(treatments[i]),
        nextAppointment: next ? appointmentWhen(next.start, today) : appointments === null ? 'No disponible' : null,
      };
    });
  }
}
