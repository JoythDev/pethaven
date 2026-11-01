import { Component, DestroyRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { Table, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import {
  EMPTY,
  Subject,
  catchError,
  concatMap,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  share,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { Owner } from '../../../../models/owner.model';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { normalizeText } from '../../../../core/text/normalize-text';
import { confirmOwnerDeletion } from '../owner-delete-confirmation';

// Dueño junto con la cantidad de mascotas que tiene (null si no se pudo consultar).
interface OwnerDeletion {
  owner: Owner;
  petsCount: number | null;
}

@Component({
  selector: 'app-owners-list',
  imports: [
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    TooltipModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
  ],
  templateUrl: './owners-list.component.html',
})
export class OwnersListComponent implements OnInit {
  private readonly ownerService = inject(OwnerService);
  private readonly petService = inject(PetService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly table = viewChild<Table>('dt');

  readonly searchControl = new FormControl('', { nonNullable: true });

  // Cada emisión pide al API la lista de dueños otra vez (carga inicial y recargas).
  private readonly refresh$ = new Subject<void>();

  // Cada emisión es un dueño cuya eliminación el usuario acaba de solicitar.
  private readonly deleteRequest$ = new Subject<Owner>();

  // Cada emisión es un dueño cuya eliminación el usuario ya confirmó.
  private readonly confirmedDelete$ = new Subject<Owner>();

  // Últimos dueños cargados. Se conservan durante una recarga para que la tabla no se vacíe.
  private readonly lastOwners = signal<Owner[]>([]);

  // Estado de la lista. Cada refresh$ pide los dueños otra vez y switchMap cancela la petición
  // anterior si llega otra. toViewState va dentro de switchMap para que cada petición empiece en
  // 'loading' y termine en 'ready' o 'error'. tap guarda los datos listos y vacía la lista si falla.
  // Aquí no se llama al API: eso ocurre al emitir refresh$.
  readonly owners = toSignal(
    this.refresh$.pipe(
      switchMap(() =>
        this.ownerService.getAll().pipe(
          toViewState<Owner[]>(),
          tap((state) => {
            if (state.status === 'ready') {
              this.lastOwners.set(state.data);
            } else if (state.status === 'error') {
              this.lastOwners.set([]);
            }
          }),
        ),
      ),
    ),
    { initialValue: { status: 'loading' } as ViewState<Owner[]> },
  );

  // Espera 250 ms sin teclear, normaliza el texto y descarta repeticiones antes de filtrar.
  // share() evita que el reset de página y el filtro creen cada uno su propia cadena de debounce.
  private readonly searchTerm$ = this.searchControl.valueChanges.pipe(
    debounceTime(250),
    map((term) => normalizeText(term)),
    distinctUntilChanged(),
    share(),
  );

  // Último texto buscado (normalizado) como signal, para recalcular la lista filtrada.
  readonly searchTerm = toSignal(this.searchTerm$.pipe(startWith('')), { initialValue: '' });

  // Lista visible: últimos dueños cargados filtrados por búsqueda. No depende del estado de
  // carga, así la tabla no se vacía mientras recarga.
  readonly filteredOwners = computed<Owner[]>(() => {
    const term = this.searchTerm();
    return this.lastOwners().filter((owner) => this.matches(owner, term));
  });

  ngOnInit(): void {
    // Llama a goToFirstPage cuando cambia la búsqueda para volver a la página 1.
    this.searchTerm$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.goToFirstPage());

    // Al solicitar una eliminación consulta cuántas mascotas tiene el dueño para mostrarlo
    // en la confirmación. switchMap descarta la consulta anterior si se pide otro dueño;
    // si la consulta falla, la confirmación se abre igual pero sin el conteo.
    this.deleteRequest$
      .pipe(
        switchMap((owner) =>
          this.petService.getByOwnerId(owner.id).pipe(
            map((pets): number | null => pets.length),
            catchError(() => of(null)),
            map((petsCount): OwnerDeletion => ({ owner, petsCount })),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ owner, petsCount }) =>
        confirmOwnerDeletion(this.confirmationService, owner, petsCount, () =>
          this.confirmedDelete$.next(owner),
        ),
      );

    // concatMap procesa las eliminaciones confirmadas de una en una. Si el API rechaza la
    // eliminación (por ejemplo 409 por tratamientos registrados) muestra su mensaje y el
    // flujo sigue vivo; si funciona, recarga la lista.
    this.confirmedDelete$
      .pipe(
        concatMap((owner) =>
          this.ownerService.delete(owner.id).pipe(
            tap(() => this.notifications.success('Dueño eliminado', `${owner.name} se eliminó correctamente.`)),
            catchError((error: unknown) => {
              this.notifications.error('No se pudo eliminar al dueño', error);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.refresh$.next());

    // Los pipelines ya están conectados; ahora sí se dispara la carga inicial.
    this.refresh$.next();
  }

  nuevo(): void {
    this.router.navigate(['/veterinario/owners/add']);
  }

  ver(owner: Owner): void {
    this.router.navigate(['/veterinario/owners', owner.id]);
  }

  editar(owner: Owner): void {
    this.router.navigate(['/veterinario/owners/update', owner.id]);
  }

  // Envía el dueño a deleteRequest$; la confirmación se abre cuando termina la consulta de mascotas.
  eliminar(owner: Owner): void {
    this.deleteRequest$.next(owner);
  }

  // Indica si el nombre o el documento del dueño contienen el texto buscado.
  private matches(owner: Owner, term: string): boolean {
    if (!term) {
      return true;
    }
    return normalizeText(owner.name).includes(term) || normalizeText(owner.document).includes(term);
  }

  // Lleva la tabla de PrimeNG a la primera página.
  private goToFirstPage(): void {
    const table = this.table();
    if (table) {
      table.first = 0;
    }
  }
}
