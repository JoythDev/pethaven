import { Component, DestroyRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { Table, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import {
  EMPTY,
  Subject,
  catchError,
  concatMap,
  debounceTime,
  distinctUntilChanged,
  map,
  merge,
  share,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { Drug } from '../../../../models/drug.model';
import { DrugService } from '../../../../services/drug.service';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { normalizeText } from '../../../../core/text/normalize-text';
import { readDrugArea } from '../drug-area';
import { confirmDrugDeletion } from '../drug-delete-confirmation';
import {
  DrugStockStatus,
  STOCK_FILTER_OPTIONS,
  formatCurrency,
  formatUnits,
  getStockInfo,
  getStockStatus,
} from '../drug-stock';

// Listado de medicamentos compartido por el administrador (gestión completa) y el veterinario
// (solo consulta). Qué acciones se muestran lo decide la configuración de la ruta.
@Component({
  selector: 'app-drugs-list',
  imports: [
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    TooltipModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    TagModule,
  ],
  templateUrl: './drugs-list.component.html',
})
export class DrugsListComponent implements OnInit {
  private readonly drugService = inject(DrugService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly table = viewChild<Table>('dt');

  readonly area = readDrugArea(inject(ActivatedRoute));

  readonly stockOptions = STOCK_FILTER_OPTIONS;

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly stockControl = new FormControl<DrugStockStatus | null>(null);
  readonly minPriceControl = new FormControl<number | null>(null);
  readonly maxPriceControl = new FormControl<number | null>(null);

  // Cada emisión pide al API la lista de medicamentos otra vez (carga inicial y recargas).
  private readonly refresh$ = new Subject<void>();

  // Cada emisión es un medicamento cuya eliminación el usuario ya confirmó.
  private readonly confirmedDelete$ = new Subject<Drug>();

  // Últimos medicamentos cargados. Se conservan durante una recarga para que la tabla no se vacíe.
  private readonly lastDrugs = signal<Drug[]>([]);

  // Estado de la lista. switchMap cancela la petición anterior si llega otro refresh$ y
  // toViewState hace que cada petición empiece en 'loading' y termine en 'ready' o 'error'.
  readonly drugs = toSignal(
    this.refresh$.pipe(
      switchMap(() =>
        this.drugService.getAll().pipe(
          toViewState<Drug[]>(),
          tap((state) => {
            if (state.status === 'ready') {
              this.lastDrugs.set(state.data);
            } else if (state.status === 'error') {
              this.lastDrugs.set([]);
            }
          }),
        ),
      ),
    ),
    { initialValue: { status: 'loading' } as ViewState<Drug[]> },
  );

  // Espera 250 ms sin teclear, normaliza el texto y descarta repeticiones antes de filtrar.
  private readonly searchTerm$ = this.searchControl.valueChanges.pipe(
    debounceTime(250),
    map((term) => normalizeText(term)),
    distinctUntilChanged(),
    share(),
  );

  readonly searchTerm = toSignal(this.searchTerm$.pipe(startWith('')), { initialValue: '' });
  readonly stockFilter = toSignal(this.stockControl.valueChanges, { initialValue: null });
  readonly minPrice = toSignal(this.minPriceControl.valueChanges, { initialValue: null });
  readonly maxPrice = toSignal(this.maxPriceControl.valueChanges, { initialValue: null });

  // El rango es inválido cuando el mínimo supera al máximo; en ese caso no se filtra por precio.
  readonly invalidPriceRange = computed(() => {
    const min = this.minPrice();
    const max = this.maxPrice();
    return min !== null && max !== null && min > max;
  });

  readonly hasFilters = computed(
    () =>
      this.searchTerm() !== '' ||
      this.stockFilter() !== null ||
      this.minPrice() !== null ||
      this.maxPrice() !== null,
  );

  // Lista visible: últimos medicamentos cargados filtrados por nombre, stock y precio de venta.
  readonly filteredDrugs = computed<Drug[]>(() => {
    const term = this.searchTerm();
    const stock = this.stockFilter();
    const useRange = !this.invalidPriceRange();
    const min = useRange ? this.minPrice() : null;
    const max = useRange ? this.maxPrice() : null;

    return this.lastDrugs().filter(
      (drug) =>
        (!term || normalizeText(drug.name).includes(term)) &&
        (stock === null || getStockStatus(drug.unitsAvailable) === stock) &&
        (min === null || drug.salePrice >= min) &&
        (max === null || drug.salePrice <= max),
    );
  });

  readonly formatCurrency = formatCurrency;
  readonly formatUnits = formatUnits;
  readonly getStockInfo = getStockInfo;

  ngOnInit(): void {
    // Cualquier cambio de filtro vuelve a la página 1 de la tabla.
    merge(
      this.searchTerm$,
      this.stockControl.valueChanges,
      this.minPriceControl.valueChanges,
      this.maxPriceControl.valueChanges,
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.goToFirstPage());

    // concatMap procesa las eliminaciones confirmadas de una en una. Si el API la rechaza
    // (409 porque el medicamento está en tratamientos) muestra su mensaje y el flujo sigue vivo.
    this.confirmedDelete$
      .pipe(
        concatMap((drug) =>
          this.drugService.delete(drug.id).pipe(
            tap(() => this.notifications.success('Medicamento eliminado', `${drug.name} se eliminó correctamente.`)),
            catchError((error: unknown) => {
              this.notifications.error('No se pudo eliminar el medicamento', error);
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
    this.router.navigate([this.area.basePath, 'add']);
  }

  importar(): void {
    this.router.navigate([this.area.basePath, 'import']);
  }

  ver(drug: Drug): void {
    this.router.navigate([this.area.basePath, drug.id]);
  }

  editar(drug: Drug): void {
    this.router.navigate([this.area.basePath, 'update', drug.id]);
  }

  eliminar(drug: Drug): void {
    confirmDrugDeletion(this.confirmationService, drug, () => this.confirmedDelete$.next(drug));
  }

  limpiarFiltros(): void {
    this.searchControl.setValue('');
    this.stockControl.setValue(null);
    this.minPriceControl.setValue(null);
    this.maxPriceControl.setValue(null);
  }

  // Lleva la tabla de PrimeNG a la primera página.
  private goToFirstPage(): void {
    const table = this.table();
    if (table) {
      table.first = 0;
    }
  }
}
