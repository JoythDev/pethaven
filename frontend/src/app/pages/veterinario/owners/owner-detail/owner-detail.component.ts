import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { EMPTY, Subject, catchError, exhaustMap, forkJoin, map, of, switchMap, tap } from 'rxjs';
import { Owner } from '../../../../models/owner.model';
import { Pet } from '../../../../models/pet.model';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { parseRouteId } from '../../../../core/http/route-id';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { onPhotoError as handlePhotoError } from '../../../../core/ui/photo-fallback';
import { confirmOwnerDeletion } from '../owner-delete-confirmation';

// Datos que pinta la ficha: el dueño y sus mascotas, cargados en paralelo.
interface OwnerDetail {
  owner: Owner;
  pets: Pet[];
}

@Component({
  selector: 'app-owners-detail',
  imports: [RouterLink, ButtonModule, TagModule],
  templateUrl: './owner-detail.component.html',
})
export class OwnersDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ownerService = inject(OwnerService);
  private readonly petService = inject(PetService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  // Cada emisión es el dueño cuya eliminación el usuario ya confirmó.
  private readonly confirmedDelete$ = new Subject<Owner>();

  readonly state = signal<ViewState<OwnerDetail>>({ status: 'loading' });

  // Lee el :id de la URL y lanza en paralelo, con forkJoin, la consulta del dueño y la de
  // sus mascotas. Un id inválido termina en error sin llamar al API. switchMap descarta la
  // consulta anterior si el id cambia, y toViewState emite "cargando" y luego "listo" o "error".
  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        map((params) => parseRouteId(params.get('id'))),
        switchMap((id) =>
          id === null
            ? of<ViewState<OwnerDetail>>({ status: 'error', message: 'El dueño solicitado no existe.' })
            : forkJoin({
                owner: this.ownerService.getById(id),
                pets: this.petService.getByOwnerId(id),
              }).pipe(toViewState<OwnerDetail>()),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((state) => this.state.set(state));

    // exhaustMap ignora confirmaciones repetidas mientras una eliminación sigue en curso.
    // Si el API la rechaza (por ejemplo 409 por tratamientos registrados) muestra su mensaje
    // y deja al usuario en la ficha; si funciona, avisa y regresa al listado.
    this.confirmedDelete$
      .pipe(
        exhaustMap((owner) =>
          this.ownerService.delete(owner.id).pipe(
            tap(() =>
              this.notifications.success('Dueño eliminado', `${owner.name} se eliminó correctamente.`),
            ),
            catchError((error: unknown) => {
              this.notifications.error('No se pudo eliminar al dueño', error);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.router.navigate(['/veterinario/owners']));
  }

  editar(id: number): void {
    this.router.navigate(['/veterinario/owners/update', id]);
  }

  volver(): void {
    this.router.navigate(['/veterinario/owners']);
  }

  verPet(id: number): void {
    this.router.navigate(['/veterinario/pets', id]);
  }

  // Abre el diálogo de confirmación y, si el usuario acepta, envía el dueño a confirmedDelete$.
  eliminar(owner: Owner, petsCount: number): void {
    confirmOwnerDeletion(this.confirmationService, owner, petsCount, () => this.confirmedDelete$.next(owner));
  }

  // Ícono de respaldo si la foto no carga. Se expone con este nombre para el binding de la plantilla.
  readonly onPhotoError = handlePhotoError;
}
