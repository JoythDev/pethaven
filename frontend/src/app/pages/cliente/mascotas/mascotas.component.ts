import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideCalendar } from '@lucide/angular';
import { BehaviorSubject, Observable, combineLatest, forkJoin, map, of, switchMap } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import { appointmentWhen } from '../../../core/client-home/client-home';
import { bookingLink, lastDiagnosis, nextAppointmentFor, petCountText } from '../../../core/client-pets/client-pets';
import { ownerIdParam } from '../../../core/client-pets/owner-route';
import { vetsFromTreatments } from '../../../core/demo/appointments';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { identityHue } from '../../../core/identity/identity';
import { ageLabel, formatKg } from '../../../core/pet-detail/pet-detail';
import { petStatus } from '../../../core/status/pet-status';
import { coatKitFor, speciesSticker } from '../../../core/stickers/stickers';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import { Pet, Species } from '../../../models/pet.model';
import { ClienteTreatment } from '../../../models/treatment-cliente.model';
import { DemoAppointmentsService } from '../../../services/demo/demo-appointments.service';
import { DEMO_NOW } from '../../../services/demo/demo-latency';
import { PetService } from '../../../services/pet.service';
import { ClienteTreatmentService } from '../../../services/treatment-cliente.service';
import { BlockComponent } from '../../shared/block/block.component';
import { DefinitionListComponent } from '../../shared/definition-list/definition-list.component';
import { DemoTagComponent } from '../../shared/demo-tag/demo-tag.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { InlineErrorComponent } from '../../shared/inline-error/inline-error.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { SkeletonComponent } from '../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../shared/sticker/sticker.component';

// Lo que muestra cada tarjeta: la mascota (real), el diagnóstico de su último tratamiento (real)
// y su próxima cita (demo, ya como texto).
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
 * último tratamiento, próxima cita (demo) y los accesos «Ver ficha» y «Agendar». Solo lectura: no hay
 * botón para agregar, porque el veterinario registra las mascotas nuevas. Mascotas y tratamientos son
 * reales; la próxima cita sale del servicio demo de citas (S3).
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
    DemoTagComponent,
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
  private readonly appointments = inject(DemoAppointmentsService);
  private readonly clock = inject(DEMO_NOW);

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

  // Las mascotas del dueño y, de cada una, sus tratamientos. Las citas demo se calculan con las mascotas
  // y los veterinarios que ya las atendieron.
  private load(ownerId: number): Observable<ViewState<PetCard[]>> {
    return this.petService.getByOwnerId(ownerId).pipe(
      switchMap((pets) =>
        (pets.length ? forkJoin(pets.map((pet) => this.treatmentService.getByPetId(pet.id))) : of([])).pipe(
          map((lists) => this.toCards(pets, lists)),
        ),
      ),
      toViewState<PetCard[]>(),
    );
  }

  private toCards(pets: Pet[], treatments: ClienteTreatment[][]): PetCard[] {
    const upcoming = this.appointments.ownerAppointmentsFrom(pets, vetsFromTreatments(treatments.flat())).upcoming;
    const today = this.clock();
    return pets.map((pet, i) => {
      const next = nextAppointmentFor(pet.id, upcoming);
      return {
        pet,
        lastDiagnosis: lastDiagnosis(treatments[i]),
        nextAppointment: next ? appointmentWhen(next.start, today) : null,
      };
    });
  }
}
