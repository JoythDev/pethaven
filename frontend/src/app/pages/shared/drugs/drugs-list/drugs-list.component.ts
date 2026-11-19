import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router, RouterLink } from '@angular/router';
import { LucideFileSpreadsheet, LucidePencil, LucidePill, LucidePlus, LucideTrash2 } from '@lucide/angular';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToggleButtonModule } from 'primeng/togglebutton';
import { Subject, finalize, map, switchMap } from 'rxjs';

import {
  DRUG_FILTER_KEYS,
  DRUG_SORT_ACCESSORS,
  DRUG_STATUS_PARAM,
  DrugRow,
  buildDrugRows,
  drugMatches,
  drugStatus,
  drugStatusFromParam,
  drugsCountLabel,
  priceRangeOf,
  sanitizePriceInput,
} from '../../../../core/drugs/drugs-list';
import { parseRouteId } from '../../../../core/http/route-id';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { identityHue } from '../../../../core/identity/identity';
import {
  ListQuery,
  ListQueryConfig,
  ListSort,
  ListView,
  clearAll,
  parseListQuery,
  parseSelectedId,
  serializeListQuery,
  withFilter,
  withPage,
  withPageSize,
  withSearch,
  withSort,
  withView,
} from '../../../../core/list/list-query';
import { sortItems } from '../../../../core/list/list-sort';
import { clampPage, paginate } from '../../../../core/list/pagination';
import { readViewPreference, writeViewPreference } from '../../../../core/list/view-preference';
import { NotificationService } from '../../../../core/notifications/notification.service';
import { resourceDialogRoute } from '../../../../core/owners/owners-route';
import { Drug } from '../../../../models/drug.model';
import { DrugService } from '../../../../services/drug.service';
import { DrugFormDialogComponent } from '../drug-form-dialog/drug-form-dialog.component';
import { DefinitionListComponent } from '../../definition-list/definition-list.component';
import { DetailPanelComponent } from '../../detail-panel/detail-panel.component';
import { PageHeaderComponent } from '../../page-header/page-header.component';
import { ResourceCardComponent } from '../../resource-card/resource-card.component';
import { ResourceListComponent } from '../../resource-list/resource-list.component';
import { SelectableRowDirective } from '../../resource-list/selectable-row.directive';
import { SortHeaderComponent } from '../../resource-list/sort-header.component';
import { ShellSidebarService } from '../../shell/shell-sidebar.service';
import { StickerComponent } from '../../sticker/sticker.component';
import { StockMeterComponent } from '../../stock-meter/stock-meter.component';
import { readDrugArea } from '../drug-area';
import { confirmDrugDeletion } from '../drug-delete-confirmation';
import { formatCurrency, formatPercent, formatUnits, getMarginPercent } from '../drug-stock';

const VIEW_RESOURCE = 'medicamentos';
const LOAD_ERROR = 'No pudimos cargar los medicamentos. Revisa tu conexión e inténtalo de nuevo.';
const LOADING = { status: 'loading' } as const;
const PRICE_DEBOUNCE_MS = 250;
const VET_NOTE = 'Consulta de inventario. Solo administración puede modificarlo.';

const toSearchParams = (params: Params): URLSearchParams =>
  new URLSearchParams(Object.entries(params).map(([key, value]) => [key, String(value)]));

const CONFIG: ListQueryConfig = {
  filterKeys: DRUG_FILTER_KEYS,
  sortFields: Object.keys(DRUG_SORT_ACCESSORS),
  defaultSort: { field: 'name', direction: 'asc' },
  defaultView: readViewPreference(VIEW_RESOURCE, 'table'),
};

/**
 * Medicamentos (11.1): lista en tabla (por defecto) o tarjetas con búsqueda, chips de existencias, rango
 * de precio de venta (11.16), orden y paginación en la URL, y panel de detalle. La monta el administrador
 * (`canManage`: crear, importar, editar y eliminar) y el veterinario (solo consulta, sin acciones). En el
 * área del administrador la lista, `add` y `update/:id` son una misma ruta: el diálogo de alta o edición se
 * abre según la URL y cerrarlo vuelve a la lista sin recrearla.
 */
@Component({
  selector: 'app-drugs-list',
  imports: [
    FormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    TableModule,
    TagModule,
    ToggleButtonModule,
    LucideFileSpreadsheet,
    LucidePencil,
    LucidePill,
    LucidePlus,
    LucideTrash2,
    DefinitionListComponent,
    DetailPanelComponent,
    DrugFormDialogComponent,
    PageHeaderComponent,
    ResourceCardComponent,
    ResourceListComponent,
    SelectableRowDirective,
    SortHeaderComponent,
    StickerComponent,
    StockMeterComponent,
  ],
  templateUrl: './drugs-list.component.html',
})
export class DrugsListComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly drugService = inject(DrugService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly shell = inject(ShellSidebarService);

  protected readonly area = readDrugArea(this.route);

  protected readonly panelOpen = signal(false);
  protected readonly selectedId = signal<number | null>(null);

  protected readonly state = signal<ViewState<Drug[]>>(LOADING);
  private readonly reload$ = new Subject<void>();
  private readonly deleting = signal(false);
  // Medicamento recién guardado: se selecciona cuando llega la lista recargada que ya lo incluye.
  private pendingSelectId: number | null = null;

  protected readonly query = toSignal(
    this.route.queryParams.pipe(map((params) => parseListQuery(toSearchParams(params), CONFIG))),
    { initialValue: parseListQuery(new URLSearchParams(), CONFIG) },
  );

  // `?seleccion=<id>` elige ese medicamento y abre su panel (también como cajón). Los cambios de la lista
  // reescriben la URL sin ese parámetro, así que no vuelve a imponerse sobre lo que la persona elija después.
  private readonly urlSelectedId = toSignal(
    this.route.queryParams.pipe(map((params) => parseSelectedId(toSearchParams(params)))),
    { initialValue: parseSelectedId(toSearchParams(this.route.snapshot.queryParams)) },
  );

  // Diálogo según la URL: `add` abre el alta y `update/:id` la edición de ese medicamento (solo administración).
  private readonly dialogRoute = toSignal(
    this.route.url.pipe(map((segments) => resourceDialogRoute(segments.map((segment) => segment.path)))),
    { initialValue: resourceDialogRoute(this.route.snapshot.url.map((segment) => segment.path)) },
  );
  private readonly dialogEditId = computed(() => {
    const route = this.dialogRoute();
    return route?.mode === 'edit' ? parseRouteId(route.rawId) : null;
  });
  protected readonly dialogOpen = computed(() => {
    const route = this.dialogRoute();
    return (
      this.area.canManage &&
      (route?.mode === 'create' || (route?.mode === 'edit' && this.dialogEditId() !== null))
    );
  });
  protected readonly dialogDrugId = this.dialogEditId;

  protected readonly rows = computed<DrugRow[]>(() => {
    const state = this.state();
    return state.status === 'ready' ? buildDrugRows(state.data) : [];
  });

  protected readonly status = computed(() => drugStatusFromParam(this.query().filters['estado']));
  protected readonly range = computed(() => priceRangeOf(this.query().filters));

  private readonly filtered = computed(() => {
    const { min, max } = this.range();
    const criteria = { search: this.query().search, status: this.status(), min, max };
    return this.rows().filter((row) => drugMatches(row, criteria));
  });
  protected readonly total = computed(() => this.filtered().length);
  protected readonly page = computed(() => clampPage(this.query().page, this.total(), this.query().pageSize));
  protected readonly pageItems = computed(() =>
    paginate(sortItems(this.filtered(), this.query().sort, DRUG_SORT_ACCESSORS), this.page(), this.query().pageSize),
  );

  protected readonly loading = computed(() => this.state().status === 'loading');
  protected readonly error = computed(() => (this.state().status === 'error' ? LOAD_ERROR : null));
  protected readonly filtersActive = computed(() => Object.keys(this.query().filters).length > 0);
  protected readonly subtitle = computed(() => {
    if (this.state().status !== 'ready') {
      return null;
    }
    const count = drugsCountLabel(this.rows().length);
    return this.area.canManage ? count : `${count} · ${VET_NOTE}`;
  });

  protected readonly current = computed(() => this.rows().find((row) => row.id === this.selectedId()) ?? null);
  protected readonly currentHue = computed(() => identityHue(this.current()?.id ?? 0));

  // Texto de los campos de precio: lo que escribe la persona, antes del antirrebote.
  protected readonly minText = signal('');
  protected readonly maxText = signal('');
  private priceTimer: ReturnType<typeof setTimeout> | undefined;

  protected readonly statusFilters = [
    { label: 'Disponible', value: DRUG_STATUS_PARAM.ok },
    { label: 'Stock bajo', value: DRUG_STATUS_PARAM.low },
    { label: 'Agotado', value: DRUG_STATUS_PARAM.out },
  ];
  protected readonly hueOf = identityHue;
  protected readonly formatCurrency = formatCurrency;
  protected readonly formatUnits = formatUnits;
  protected readonly formatPercent = formatPercent;
  protected readonly getMarginPercent = getMarginPercent;
  protected readonly drugStatus = drugStatus;
  protected readonly withSearch = withSearch;
  protected readonly withPage = withPage;
  protected readonly withPageSize = withPageSize;
  protected readonly clearAll = clearAll;

  constructor() {
    this.reload$
      .pipe(
        switchMap(() => this.drugService.getAll().pipe(toViewState<Drug[]>())),
        takeUntilDestroyed(),
      )
      // Al recargar tras eliminar se conservan los datos que ya se ven.
      .subscribe((state) => {
        if (state.status !== 'loading' || this.state().status !== 'ready') {
          this.state.set(state);
        }
        if (state.status === 'ready' && this.pendingSelectId !== null) {
          this.selectedId.set(this.pendingSelectId);
          this.pendingSelectId = null;
        }
      });
    this.reload$.next();

    // `update/:id` con un id inválido: mismo aviso de siempre y regreso a la lista.
    effect(() => {
      const route = this.dialogRoute();
      if (this.area.canManage && route?.mode === 'edit' && this.dialogEditId() === null) {
        this.notifications.error('Medicamento no encontrado', null);
        this.closeDialog();
      }
    });

    // Un cambio externo de la URL («Quitar filtros», atrás del navegador) reescribe los campos de precio.
    effect(() => {
      const { filters } = this.query();
      this.minText.set(filters['min'] ?? '');
      this.maxText.set(filters['max'] ?? '');
    });

    effect(() => {
      const id = this.urlSelectedId();
      if (id !== null) {
        this.selectedId.set(id);
        this.panelOpen.set(true);
      }
    });

    // La página se corrige en la URL cuando la lista encoge.
    effect(() => {
      const query = this.query();
      if (this.state().status === 'ready' && query.page !== this.page()) {
        this.update(withPage(query, this.page()));
      }
    });

    // En la columna fija se selecciona el primero al cargar y cuando el elegido ya no existe (10.13);
    // en cajón no, para no abrirlo solo.
    effect(() => {
      const stale = this.selectedId() === null || (this.state().status === 'ready' && this.current() === null);
      if (this.shell.layout() === 'expanded' && stale && this.pageItems().length) {
        this.selectedId.set(this.pageItems()[0].id);
      }
    });

    this.destroyRef.onDestroy(() => clearTimeout(this.priceTimer));
  }

  protected retry(): void {
    this.state.set(LOADING);
    this.reload$.next();
  }

  protected select(id: number | string): void {
    this.selectedId.set(id as number);
    this.panelOpen.set(true);
  }

  protected update(query: ListQuery): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: serializeListQuery(query, CONFIG),
      replaceUrl: true,
    });
  }

  protected onSort(sort: ListSort): void {
    this.update(withSort(this.query(), sort));
  }

  protected onView(view: ListView): void {
    writeViewPreference(VIEW_RESOURCE, view);
    this.update(withView(this.query(), view));
  }

  protected toggleStatus(value: string): void {
    const active = this.query().filters['estado'] === value;
    this.update(withFilter(this.query(), 'estado', active ? '' : value));
  }

  /** Escribe en un campo de precio: solo dígitos y, tras 250 ms sin teclear, pasa a la URL. */
  protected onPriceInput(key: 'min' | 'max', input: HTMLInputElement): void {
    const digits = sanitizePriceInput(input.value);
    input.value = digits;
    (key === 'min' ? this.minText : this.maxText).set(digits);
    clearTimeout(this.priceTimer);
    this.priceTimer = setTimeout(() => this.update(withFilter(this.query(), key, digits)), PRICE_DEBOUNCE_MS);
  }

  /** Enter no espera al antirrebote. */
  protected flushPrice(key: 'min' | 'max'): void {
    clearTimeout(this.priceTimer);
    this.update(withFilter(this.query(), key, (key === 'min' ? this.minText : this.maxText)()));
  }

  /** El diálogo guardó: la lista se recarga y el panel sigue al medicamento guardado. */
  protected onSaved(drug: Drug): void {
    this.pendingSelectId = drug.id;
    this.reload$.next();
  }

  protected closeDialog(): void {
    void this.router.navigate([this.area.basePath], { queryParamsHandling: 'preserve' });
  }

  protected remove(drug: DrugRow): void {
    confirmDrugDeletion(this.confirmationService, drug, () => this.deleteDrug(drug));
  }

  // Si el API rechaza la eliminación (409 por tratamientos asociados) muestra su mensaje y deja la lista
  // como estaba; si funciona, avisa y recarga.
  private deleteDrug(drug: DrugRow): void {
    if (this.deleting()) {
      return;
    }
    this.deleting.set(true);
    this.drugService
      .delete(drug.id)
      .pipe(
        finalize(() => this.deleting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Medicamento eliminado', `${drug.name} se eliminó correctamente.`);
          if (this.selectedId() === drug.id) {
            this.selectedId.set(null);
          }
          this.reload$.next();
        },
        error: (error: unknown) => this.notifications.error('No se pudo eliminar el medicamento', error),
      });
  }
}
