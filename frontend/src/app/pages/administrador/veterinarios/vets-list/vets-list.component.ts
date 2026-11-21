import { Component, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucidePencil, LucidePlus } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToggleButtonModule } from 'primeng/togglebutton';
import { Subject, map, of, switchMap } from 'rxjs';

import { parseRouteId } from '../../../../core/http/route-id';
import { ViewState, toViewState } from '../../../../core/http/view-state';
import { identityHue, initials } from '../../../../core/identity/identity';
import {
  ListQuery,
  ListQueryConfig,
  ListSort,
  ListView,
  clearAll,
  parseListQuery,
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
import { VetActivity, deriveVetActivity } from '../../../../core/vets/vet-activity';
import {
  VET_FILTER_KEYS,
  VET_SORT_ACCESSORS,
  VET_STATUS_FILTERS,
  replaceVet,
  vetStatusLabel,
  vetsCountLabel,
  vetMatches,
} from '../../../../core/vets/vets-list';
import { Veterinarian } from '../../../../models/veterinarian.model';
import { VeterinarianService } from '../../../../services/veterinarian.service';
import { AvatarComponent } from '../../../shared/avatar/avatar.component';
import { DefinitionListComponent } from '../../../shared/definition-list/definition-list.component';
import { DetailPanelComponent } from '../../../shared/detail-panel/detail-panel.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ResourceCardComponent } from '../../../shared/resource-card/resource-card.component';
import { ResourceListComponent } from '../../../shared/resource-list/resource-list.component';
import { SelectableRowDirective } from '../../../shared/resource-list/selectable-row.directive';
import { SortHeaderComponent } from '../../../shared/resource-list/sort-header.component';
import { ShellSidebarService } from '../../../shared/shell/shell-sidebar.service';
import { VetActiveSwitchComponent } from '../vet-active-switch/vet-active-switch.component';
import { VetFormDialogComponent } from '../vet-form-dialog/vet-form-dialog.component';

const VIEW_RESOURCE = 'veterinarios';
const LIST_URL = '/administrador/veterinarios';
const LOAD_ERROR = 'No pudimos cargar los veterinarios. Revisa tu conexión e inténtalo de nuevo.';
const LOADING = { status: 'loading' } as const;

const CONFIG: ListQueryConfig = {
  filterKeys: VET_FILTER_KEYS,
  sortFields: Object.keys(VET_SORT_ACCESSORS),
  defaultSort: { field: 'name', direction: 'asc' },
  defaultView: readViewPreference(VIEW_RESOURCE, 'cards'),
};

/**
 * Veterinarios del administrador (11.1): lista en tarjetas (por defecto) o tabla con búsqueda, chips
 * «Activos» e «Inactivos», orden y paginación en la URL, y panel de detalle con el interruptor
 * «Cuenta activa». Sin barra lateral contextual. La lista, `veterinarios/add` y
 * `veterinarios/update/:id` son una misma ruta: el diálogo de alta o edición se abre según la URL y
 * cerrarlo vuelve a `veterinarios` sin recrear la lista.
 */
@Component({
  selector: 'app-vets-list',
  imports: [
    FormsModule,
    RouterLink,
    ButtonModule,
    TableModule,
    TagModule,
    ToggleButtonModule,
    LucidePencil,
    LucidePlus,
    AvatarComponent,
    DefinitionListComponent,
    DetailPanelComponent,
    PageHeaderComponent,
    ResourceCardComponent,
    ResourceListComponent,
    SelectableRowDirective,
    SortHeaderComponent,
    VetActiveSwitchComponent,
    VetFormDialogComponent,
  ],
  templateUrl: './vets-list.component.html',
})
export class VetsListComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly vetService = inject(VeterinarianService);
  private readonly notifications = inject(NotificationService);
  private readonly shell = inject(ShellSidebarService);

  protected readonly panelOpen = signal(false);
  protected readonly selectedId = signal<number | null>(null);

  protected readonly state = signal<ViewState<Veterinarian[]>>(LOADING);
  private readonly reload$ = new Subject<void>();
  // Veterinario recién guardado: se selecciona cuando llega la lista recargada que ya lo incluye.
  private pendingSelectId: number | null = null;

  protected readonly query = toSignal(
    this.route.queryParams.pipe(
      map((params) =>
        parseListQuery(new URLSearchParams(Object.entries(params).map(([key, value]) => [key, String(value)])), CONFIG),
      ),
    ),
    { initialValue: parseListQuery(new URLSearchParams(), CONFIG) },
  );

  // Diálogo según la URL: `add` abre el alta y `update/:id` la edición de ese veterinario.
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
    return route?.mode === 'create' || (route?.mode === 'edit' && this.dialogEditId() !== null);
  });
  protected readonly dialogVetId = this.dialogEditId;

  protected readonly vets = computed<Veterinarian[]>(() => {
    const state = this.state();
    return state.status === 'ready' ? state.data : [];
  });

  private readonly filtered = computed(() => {
    const { search, filters } = this.query();
    const criteria = { search, status: filters['estado'] ?? '' };
    return this.vets().filter((vet) => vetMatches(vet, criteria));
  });
  protected readonly total = computed(() => this.filtered().length);
  protected readonly page = computed(() => clampPage(this.query().page, this.total(), this.query().pageSize));
  protected readonly pageItems = computed(() =>
    paginate(sortItems(this.filtered(), this.query().sort, VET_SORT_ACCESSORS), this.page(), this.query().pageSize),
  );

  protected readonly loading = computed(() => this.state().status === 'loading');
  protected readonly error = computed(() => (this.state().status === 'error' ? LOAD_ERROR : null));
  protected readonly filtersActive = computed(() => Object.keys(this.query().filters).length > 0);
  protected readonly subtitle = computed(() => (this.state().status === 'ready' ? vetsCountLabel(this.vets()) : null));

  protected readonly current = computed(() => this.vets().find((vet) => vet.id === this.selectedId()) ?? null);
  protected readonly currentHue = computed(() => identityHue(this.current()?.id ?? 0));

  // Los pacientes y los tratamientos del mes salen de los tratamientos del veterinario elegido.
  protected readonly activity = toSignal(
    toObservable(this.selectedId).pipe(
      switchMap((id) =>
        id === null
          ? of<ViewState<VetActivity>>(LOADING)
          : this.vetService.getTreatmentsByVeterinarianId(id).pipe(
              map((treatments) => deriveVetActivity(treatments, new Date())),
              toViewState<VetActivity>(),
            ),
      ),
    ),
    { initialValue: LOADING as ViewState<VetActivity> },
  );
  protected readonly activityData = computed(() => {
    const activity = this.activity();
    return activity.status === 'ready' ? activity.data : null;
  });

  protected readonly listUrl = LIST_URL;
  protected readonly activeFilter = VET_STATUS_FILTERS.active;
  protected readonly inactiveFilter = VET_STATUS_FILTERS.inactive;
  protected readonly initials = initials;
  protected readonly statusLabel = vetStatusLabel;
  protected readonly withSearch = withSearch;
  protected readonly withPage = withPage;
  protected readonly withPageSize = withPageSize;
  protected readonly clearAll = clearAll;

  constructor() {
    this.reload$
      .pipe(
        switchMap(() => this.vetService.getAll().pipe(toViewState<Veterinarian[]>())),
        takeUntilDestroyed(),
      )
      // Al recargar tras guardar se conservan los datos que ya se ven.
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
      if (route?.mode === 'edit' && this.dialogEditId() === null) {
        this.notifications.error('Veterinario no encontrado', null);
        this.closeDialog();
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

  protected toggleFilter(key: string, value: string): void {
    const active = this.query().filters[key] === value;
    this.update(withFilter(this.query(), key, active ? '' : value));
  }

  /** El diálogo guardó: la lista se recarga y el panel sigue al veterinario guardado. */
  protected onSaved(vet: Veterinarian): void {
    this.pendingSelectId = vet.id;
    this.reload$.next();
  }

  /** El interruptor guardó: se sustituye el veterinario sin recargar la lista. */
  protected onActiveChanged(updated: Veterinarian): void {
    this.state.update((state) =>
      state.status === 'ready' ? { status: 'ready', data: replaceVet(state.data, updated) } : state,
    );
  }

  protected closeDialog(): void {
    void this.router.navigate([LIST_URL], { queryParamsHandling: 'preserve' });
  }
}
