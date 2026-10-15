import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AccordionModule } from 'primeng/accordion';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { Observable, Subject, combineLatest, forkJoin, map, of, switchMap } from 'rxjs';
import { parseRouteId } from '../../../core/http/route-id';
import { ViewState, toViewState } from '../../../core/http/view-state';
import { onPhotoError as handlePhotoError } from '../../../core/ui/photo-fallback';
import { Pet } from '../../../models/pet.model';
import { ClienteTreatment } from '../../../models/treatment-cliente.model';
import { PetService } from '../../../services/pet.service';
import { ClienteTreatmentService } from '../../../services/treatment-cliente.service';
import { ageLabel, drugCountLabel, speciesLabel, weightLabel } from '../shared/cliente-labels';
import { ClientePetStatusTagComponent } from '../shared/pet-status-tag/cliente-pet-status-tag.component';

// Mascota con su historial de tratamientos, como lo pinta el detalle del cliente.
interface PetDetail {
  pet: Pet;
  treatments: ClienteTreatment[];
}

@Component({
  selector: 'app-mascota-detalle',
  imports: [RouterLink, AccordionModule, SkeletonModule, TagModule, ClientePetStatusTagComponent],
  templateUrl: './mascota-detalle.component.html',
})
export class MascotaDetalleComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly petService = inject(PetService);
  private readonly treatmentService = inject(ClienteTreatmentService);
  private readonly destroyRef = inject(DestroyRef);

  // :ownerId de la ruta padre y :petId de esta ruta; null mientras llegan o si un id es inválido.
  private readonly ownerId = signal<number | null>(null);
  private readonly petId = signal<number | null>(null);

  // Cada emisión vuelve a pedir la mascota y sus tratamientos (carga inicial y recargas).
  // El primer HTTP sale de ngOnInit, no del constructor.
  private readonly refresh$ = new Subject<void>();

  // Estado del detalle. Un id inválido, o una mascota de otro dueño, terminan en error sin
  // mostrar datos ajenos; con ids válidos toViewState emite "cargando" y luego "listo" o "error".
  readonly state = toSignal(this.refresh$.pipe(switchMap(() => this.load())), {
    initialValue: { status: 'loading' } as ViewState<PetDetail>,
  });

  // Id del tratamiento más reciente: abre su panel del acordeón sin depender del índice,
  // así el acordeón no se rompe cuando cambia el estado.
  readonly latestTreatmentId = computed<number>(() => {
    const state = this.state();
    if (state.status !== 'ready') {
      return 0;
    }
    return state.data.treatments[0]?.id ?? 0;
  });

  // Enlace de regreso al listado; con el dueño inválido apunta al marcador /cliente/x/...
  readonly backLink = computed<(string | number)[]>(() => ['/cliente', this.ownerId() ?? 'x', 'mascotas']);

  // Etiquetas en español del cliente, expuestas con estos nombres para la plantilla.
  readonly speciesLabel = speciesLabel;
  readonly ageLabel = ageLabel;
  readonly weightLabel = weightLabel;
  readonly drugCountLabel = drugCountLabel;

  // Ícono de respaldo si la foto no carga. Se expone con este nombre para el binding de la plantilla.
  readonly onPhotoError = handlePhotoError;

  ngOnInit(): void {
    // Los dos ids viven en rutas distintas: el :ownerId en la ruta padre (esta ruta hija no lo
    // hereda con la estrategia de params por defecto) y el :petId en esta ruta. Los paramMap
    // emiten su valor actual al suscribirse, así que la carga inicial sale de una sola emisión
    // y un cambio en cualquiera de los dos vuelve a cargar el detalle.
    combineLatest([
      this.route.parent!.paramMap.pipe(map((params) => parseRouteId(params.get('ownerId')))),
      this.route.paramMap.pipe(map((params) => parseRouteId(params.get('petId')))),
    ])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([ownerId, petId]) => {
        this.ownerId.set(ownerId);
        this.petId.set(petId);
        this.refresh$.next();
      });
  }

  // Fecha del tratamiento en dd/MM/yyyy. La cadena ISO no trae zona horaria, así que se
  // leen los componentes locales de la fecha para no desplazar el día.
  formatDate(value: string): string {
    const date = new Date(value);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    return `${day}/${month}/${date.getFullYear()}`;
  }

  // Pide la mascota y sus tratamientos en paralelo, valida el dueño de la ruta y ordena el
  // historial del más reciente al más antiguo sin mutar el arreglo original.
  private load(): Observable<ViewState<PetDetail>> {
    const ownerId = this.ownerId();
    const petId = this.petId();

    if (ownerId === null || petId === null) {
      return of<ViewState<PetDetail>>({ status: 'error', message: 'Mascota no encontrada.' });
    }

    return forkJoin({
      pet: this.petService.getById(petId),
      treatments: this.treatmentService.getByPetId(petId),
    }).pipe(
      toViewState<PetDetail>(),
      map((state): ViewState<PetDetail> => {
        if (state.status !== 'ready') {
          return state;
        }
        // El área del cliente solo muestra mascotas de su dueño: si el id no coincide se
        // trata como si no existiera. Es una comprobación de interfaz, no de seguridad.
        if (state.data.pet.ownerId !== ownerId) {
          return { status: 'error', message: 'Mascota no encontrada.' };
        }
        return {
          status: 'ready',
          data: {
            pet: state.data.pet,
            treatments: [...state.data.treatments].sort(
              (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime(),
            ),
          },
        };
      }),
    );
  }
}
