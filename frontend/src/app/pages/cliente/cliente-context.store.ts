import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Owner } from '../../models/owner.model';
import { OwnerService } from '../../services/owner.service';

/**
 * Dueño del área del cliente, compartido entre el layout (topbar) y las
 * páginas (perfil). Vive a nivel raíz mientras dure la navegación del área.
 */
@Injectable({ providedIn: 'root' })
export class ClienteContextStore {
  private readonly ownerService = inject(OwnerService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly ownerState = signal<Owner | null>(null);

  /** Dueño cargado; null mientras carga o si el id no existe. */
  readonly owner = this.ownerState.asReadonly();

  // Carga el dueño una sola vez por id: repetir la misma carga no vuelve a llamar al API.
  load(ownerId: number): void {
    if (this.ownerState()?.id === ownerId) {
      return;
    }

    this.ownerService
      .getById(ownerId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (owner) => this.ownerState.set(owner),
        // Un fallo aquí no rompe el área: la página que necesite el detalle del
        // dueño muestra su propio estado de error.
        error: () => this.ownerState.set(null),
      });
  }
}
