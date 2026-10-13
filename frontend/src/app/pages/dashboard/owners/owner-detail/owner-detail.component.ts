import { Component, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { EMPTY, Subject, catchError, exhaustMap, forkJoin, map, of, startWith, switchMap, tap } from 'rxjs';
import { Owner } from '../../../../models/owner.model';
import { Pet } from '../../../../models/pet.model';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { getErrorMessage } from '../../../../utils/http-error.util';

// Estado que pinta la plantilla: cargando, dueño con sus mascotas o mensaje de error.
interface OwnerDetailState {
  loading: boolean;
  owner: Owner | null;
  pets: Pet[];
  error: string | null;
}

const LOADING_STATE: OwnerDetailState = { loading: true, owner: null, pets: [], error: null };

@Component({
  selector: 'app-owners-detail',
  imports: [RouterLink, ButtonModule, TagModule],
  templateUrl: './owner-detail.component.html',
})
export class OwnersDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ownerService = inject(OwnerService);
  private readonly petService = inject(PetService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  // Cada emisión es el dueño cuya eliminación el usuario ya confirmó.
  private readonly confirmedDelete$ = new Subject<Owner>();

  // Lee el :id de la URL y lanza en paralelo, con forkJoin, la consulta del dueño y la de
  // sus mascotas; emite cuando ambas responden. switchMap descarta la consulta anterior si
  // el id cambia. startWith emite el estado "cargando" y catchError convierte una falla
  // (404, red caída) en un estado con mensaje en vez de romper el flujo.
  readonly state = toSignal(
    this.route.paramMap.pipe(
      map((params) => Number(params.get('id'))),
      switchMap((id) =>
        forkJoin({
          owner: this.ownerService.getById(id),
          pets: this.petService.getByOwnerId(id),
        }).pipe(
          map(({ owner, pets }): OwnerDetailState => ({ loading: false, owner, pets, error: null })),
          catchError((error: unknown) =>
            of<OwnerDetailState>({ loading: false, owner: null, pets: [], error: getErrorMessage(error) }),
          ),
          startWith(LOADING_STATE),
        ),
      ),
    ),
    { initialValue: LOADING_STATE },
  );

  constructor() {
    // exhaustMap ignora confirmaciones repetidas mientras una eliminación sigue en curso.
    // Si el API la rechaza (por ejemplo 409 por tratamientos registrados) muestra su mensaje
    // y deja al usuario en la ficha; si funciona, avisa y regresa al listado.
    this.confirmedDelete$
      .pipe(
        exhaustMap((owner) =>
          this.ownerService.delete(owner.id).pipe(
            tap(() =>
              this.messageService.add({
                severity: 'success',
                summary: 'Dueño eliminado',
                detail: `${owner.name} se eliminó correctamente.`,
              }),
            ),
            catchError((error: unknown) => {
              this.messageService.add({
                severity: 'error',
                summary: 'No se pudo eliminar al dueño',
                detail: getErrorMessage(error),
              });
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.router.navigate(['/dashboard/owners']));
  }

  editar(id: number): void {
    this.router.navigate(['/dashboard/owners/update', id]);
  }

  volver(): void {
    this.router.navigate(['/dashboard/owners']);
  }

  verPet(id: number): void {
    this.router.navigate(['/dashboard/pets', id]);
  }

  // Abre el diálogo de confirmación y, si el usuario acepta, envía el dueño a confirmedDelete$.
  eliminar(owner: Owner, petsCount: number): void {
    const petsDetail =
      petsCount > 0
        ? `También se eliminarán ${petsCount === 1 ? 'su mascota asociada' : `sus ${petsCount} mascotas asociadas`}.`
        : 'No tiene mascotas asociadas.';

    this.confirmationService.confirm({
      header: 'Eliminar dueño',
      message:
        `¿Seguro que quieres eliminar a ${owner.name}? ${petsDetail} ` +
        'Si alguna de sus mascotas tiene tratamientos registrados, no se podrá eliminar. ' +
        'Esta acción no se puede deshacer.',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.confirmedDelete$.next(owner),
    });
  }

  onPhotoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (!img.src.endsWith('paw-print-icon.svg')) {
      img.src = '/images/icons/paw-print-icon.svg';
    }
  }
}
