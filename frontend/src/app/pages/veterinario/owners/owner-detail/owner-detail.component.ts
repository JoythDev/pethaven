import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideChevronLeft, LucidePencil, LucidePlus, LucideTrash2 } from '@lucide/angular';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { BehaviorSubject, combineLatest, finalize, forkJoin, map, of, switchMap } from 'rxjs';

import { parseRouteId } from '../../../../core/http/route-id';
import { identityHue, initials } from '../../../../core/identity/identity';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { OWNER_NOT_FOUND, OwnerLoad, toOwnerLoad } from '../../../../core/owners/owner-detail';
import { newPetUrl, ownerPetQuery } from '../../../../core/owners/owner-form';
import { petsCountLabel } from '../../../../core/owners/owners-list';
import { ageLabel, speciesLabel } from '../../../../core/pets-list/pets-list';
import { petStatus } from '../../../../core/status/pet-status';
import { skeletonVisible } from '../../../../core/ui/skeleton-timing';
import { Owner } from '../../../../models/owner.model';
import { Pet } from '../../../../models/pet.model';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { AvatarComponent } from '../../../shared/avatar/avatar.component';
import { BlockComponent } from '../../../shared/block/block.component';
import { CardComponent } from '../../../shared/card/card.component';
import { DefinitionListComponent } from '../../../shared/definition-list/definition-list.component';
import { EmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { InlineErrorComponent } from '../../../shared/inline-error/inline-error.component';
import { ListItemComponent } from '../../../shared/list-item/list-item.component';
import { SkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { StickerComponent } from '../../../shared/sticker/sticker.component';
import { confirmOwnerDeletion } from '../owner-delete-confirmation';
import { OwnerFormDialogComponent } from '../owner-form-dialog/owner-form-dialog.component';

const LOADING = { status: 'loading' } as const;

/**
 * Ficha de un dueño (11.2): cabecera en su color de identidad con sus datos y acciones, y tarjetas
 * «Datos de contacto» y «Mascotas». Editar abre el diálogo de edición sin salir de la ficha.
 */
@Component({
  selector: 'app-owners-detail',
  imports: [
    RouterLink,
    ButtonModule,
    TagModule,
    LucideChevronLeft,
    LucidePencil,
    LucidePlus,
    LucideTrash2,
    AvatarComponent,
    BlockComponent,
    CardComponent,
    DefinitionListComponent,
    EmptyStateComponent,
    InlineErrorComponent,
    ListItemComponent,
    OwnerFormDialogComponent,
    SkeletonComponent,
    StickerComponent,
  ],
  templateUrl: './owner-detail.component.html',
})
export class OwnersDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ownerService = inject(OwnerService);
  private readonly petService = inject(PetService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly state = signal<OwnerLoad>(LOADING);
  protected readonly skeleton = skeletonVisible(computed(() => this.state().status === 'loading'));
  protected readonly editing = signal(false);
  private readonly deleting = signal(false);

  private readonly reload$ = new BehaviorSubject<void>(undefined);
  // Al recargar tras guardar se conservan los datos que ya se ven en vez de volver al esqueleto.
  private silentReload = false;

  protected readonly hue = computed(() => {
    const state = this.state();
    return identityHue(state.status === 'ready' ? state.data.owner.id : 0);
  });

  protected readonly newPetUrl = newPetUrl();
  protected readonly ownerPetQuery = ownerPetQuery;
  protected readonly initials = initials;
  protected readonly petsCountLabel = petsCountLabel;
  protected readonly speciesLabel = speciesLabel;
  protected readonly ageLabel = ageLabel;

  constructor() {
    // Lee el :id de la URL y pide en paralelo el dueño y sus mascotas. Un id inválido es «no encontrado»
    // sin llamar al API; switchMap descarta la consulta anterior si el id o la recarga cambian.
    combineLatest([this.route.paramMap.pipe(map((params) => parseRouteId(params.get('id')))), this.reload$])
      .pipe(
        switchMap(([id]) =>
          id === null
            ? of<OwnerLoad>(OWNER_NOT_FOUND)
            : forkJoin({ owner: this.ownerService.getById(id), pets: this.petService.getByOwnerId(id) }).pipe(
                toOwnerLoad(),
              ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((load) => {
        if (load.status === 'loading' && this.silentReload) {
          return;
        }
        if (load.status !== 'loading') {
          this.silentReload = false;
        }
        this.state.set(load);
      });
  }

  protected retry(): void {
    this.state.set(LOADING);
    this.reload$.next();
  }

  protected status(pet: Pet) {
    return petStatus(pet);
  }

  /** El diálogo guardó: la ficha se recarga con los datos nuevos. */
  protected onSaved(): void {
    this.silentReload = true;
    this.reload$.next();
  }

  // Abre la confirmación con el total de mascotas que la ficha ya conoce.
  protected remove(owner: Owner, petsCount: number): void {
    confirmOwnerDeletion(this.confirmationService, owner, petsCount, () => this.deleteOwner(owner));
  }

  // Si el API rechaza la eliminación (p. ej. 409 por tratamientos registrados) muestra su mensaje y
  // deja al usuario en la ficha; si funciona, avisa y regresa a la lista.
  private deleteOwner(owner: Owner): void {
    if (this.deleting()) {
      return;
    }
    this.deleting.set(true);
    this.ownerService
      .delete(owner.id)
      .pipe(
        finalize(() => this.deleting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Dueño eliminado', `${owner.name} se eliminó correctamente.`);
          void this.router.navigate(['/veterinario/duenos']);
        },
        error: (error: unknown) => this.notifications.error('No se pudo eliminar al dueño', error),
      });
  }
}
