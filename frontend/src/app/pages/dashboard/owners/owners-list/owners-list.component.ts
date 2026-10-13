import { Component, OnInit, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
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
  combineLatest,
  concatMap,
  debounceTime,
  distinctUntilChanged,
  finalize,
  map,
  of,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { Owner } from '../../../../models/owner.model';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { getErrorMessage } from '../../../../utils/http-error.util';

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
  private readonly messageService = inject(MessageService);

  private readonly table = viewChild<Table>('dt');

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly loading = signal(false);

  // Cada emisión pide al API la lista de dueños otra vez (carga inicial y recargas).
  private readonly reload$ = new Subject<void>();

  // Cada emisión es un dueño cuya eliminación el usuario acaba de solicitar.
  private readonly deleteRequest$ = new Subject<Owner>();

  // Cada emisión es un dueño cuya eliminación el usuario ya confirmó.
  private readonly confirmedDelete$ = new Subject<Owner>();

  // Llama a OwnerService.getAll cada vez que reload$ emite. Si llega otra recarga mientras
  // la anterior sigue en curso, switchMap cancela la petición vieja. Ante un error avisa
  // al usuario y emite una lista vacía para que el flujo siga vivo.
  private readonly owners$ = this.reload$.pipe(
    switchMap(() => {
      this.loading.set(true);
      return this.ownerService.getAll().pipe(
        catchError((error: unknown) => {
          this.notifyError('No se pudieron cargar los dueños', error);
          return of<Owner[]>([]);
        }),
        finalize(() => this.loading.set(false)),
      );
    }),
  );

  // Espera 250 ms sin teclear, normaliza el texto y descarta repeticiones antes de filtrar.
  private readonly searchTerm$ = this.searchControl.valueChanges.pipe(
    debounceTime(250),
    map((term) => this.normalize(term)),
    distinctUntilChanged(),
  );

  // Combina la última lista del API con el último texto buscado y expone el resultado
  // filtrado por nombre o documento como signal para la tabla.
  readonly filteredOwners = toSignal(
    combineLatest([this.owners$, this.searchTerm$.pipe(startWith(''))]).pipe(
      map(([owners, term]) => owners.filter((owner) => this.matches(owner, term))),
    ),
    { initialValue: [] as Owner[] },
  );

  constructor() {
    // Llama a goToFirstPage cuando cambia la búsqueda para volver a la página 1.
    this.searchTerm$.pipe(takeUntilDestroyed()).subscribe(() => this.goToFirstPage());

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
        takeUntilDestroyed(),
      )
      .subscribe(({ owner, petsCount }) => this.openConfirmation(owner, petsCount));

    // concatMap procesa las eliminaciones confirmadas de una en una. Si el API rechaza la
    // eliminación (por ejemplo 409 por tratamientos registrados) muestra su mensaje y el
    // flujo sigue vivo; si funciona, recarga la lista.
    this.confirmedDelete$
      .pipe(
        concatMap((owner) =>
          this.ownerService.delete(owner.id).pipe(
            tap(() =>
              this.messageService.add({
                severity: 'success',
                summary: 'Dueño eliminado',
                detail: `${owner.name} se eliminó correctamente.`,
              }),
            ),
            catchError((error: unknown) => {
              this.notifyError('No se pudo eliminar al dueño', error);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.reload$.next());
  }

  ngOnInit(): void {
    // Dispara la carga inicial: reload$ emite y owners$ llama al API.
    this.reload$.next();
  }

  nuevo(): void {
    this.router.navigate(['/dashboard/owners/add']);
  }

  ver(owner: Owner): void {
    this.router.navigate(['/dashboard/owners', owner.id]);
  }

  editar(owner: Owner): void {
    this.router.navigate(['/dashboard/owners/update', owner.id]);
  }

  // Envía el dueño a deleteRequest$; la confirmación se abre cuando termina la consulta de mascotas.
  eliminar(owner: Owner): void {
    this.deleteRequest$.next(owner);
  }

  // Abre el diálogo de confirmación y, si el usuario acepta, envía el dueño a confirmedDelete$.
  private openConfirmation(owner: Owner, petsCount: number | null): void {
    this.confirmationService.confirm({
      header: 'Eliminar dueño',
      message:
        `¿Seguro que quieres eliminar a ${owner.name}? ${this.describePets(petsCount)} ` +
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

  // Describe qué pasará con las mascotas del dueño al eliminarlo.
  private describePets(petsCount: number | null): string {
    if (petsCount === null) {
      return 'También se eliminarán sus mascotas asociadas.';
    }
    if (petsCount === 0) {
      return 'No tiene mascotas asociadas.';
    }
    return `También se eliminarán ${petsCount === 1 ? 'su mascota asociada' : `sus ${petsCount} mascotas asociadas`}.`;
  }

  // Indica si el nombre o el documento del dueño contienen el texto buscado.
  private matches(owner: Owner, term: string): boolean {
    if (!term) {
      return true;
    }
    return this.normalize(owner.name).includes(term) || this.normalize(owner.document).includes(term);
  }

  // Lleva la tabla de PrimeNG a la primera página.
  private goToFirstPage(): void {
    const table = this.table();
    if (table) {
      table.first = 0;
    }
  }

  private notifyError(summary: string, error: unknown): void {
    this.messageService.add({ severity: 'error', summary, detail: getErrorMessage(error) });
  }

  // Pasa a minúsculas y quita tildes para que la búsqueda no distinga acentos.
  private normalize(value: string): string {
    return value
      .toLowerCase()
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '');
  }
}
