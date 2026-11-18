import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucidePencil, LucidePlus, LucideTrash2 } from '@lucide/angular';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToggleButtonModule } from 'primeng/togglebutton';
import { Subject, catchError, finalize, forkJoin, map, of, switchMap } from 'rxjs';

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
import { newPetUrl, ownerPetQuery } from '../../../../core/owners/owner-form';
import { ownerDialogRoute } from '../../../../core/owners/owners-route';
import {
  OWNER_FILTER_KEYS,
  OWNER_SORT_ACCESSORS,
  OwnerRow,
  buildOwnerRows,
  ownerMatches,
  ownersCountLabel,
  petsCountLabel,
  petsOfOwner,
} from '../../../../core/owners/owners-list';
import { ageLabel, speciesLabel } from '../../../../core/pets-list/pets-list';
import { petStatus } from '../../../../core/status/pet-status';
import { Owner } from '../../../../models/owner.model';
import { Pet } from '../../../../models/pet.model';
import { OwnerService } from '../../../../services/owner.service';
import { PetService } from '../../../../services/pet.service';
import { AvatarComponent } from '../../../shared/avatar/avatar.component';
import { DefinitionListComponent } from '../../../shared/definition-list/definition-list.component';
import { DetailPanelComponent } from '../../../shared/detail-panel/detail-panel.component';
import { ListItemComponent } from '../../../shared/list-item/list-item.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { ResourceCardComponent } from '../../../shared/resource-card/resource-card.component';
import { ResourceListComponent } from '../../../shared/resource-list/resource-list.component';
import { SelectableRowDirective } from '../../../shared/resource-list/selectable-row.directive';
import { SortHeaderComponent } from '../../../shared/resource-list/sort-header.component';
import { ShellSidebarService } from '../../../shared/shell/shell-sidebar.service';
import { confirmOwnerDeletion } from '../owner-delete-confirmation';
import { OwnerFormDialogComponent } from '../owner-form-dialog/owner-form-dialog.component';

const VIEW_RESOURCE = 'duenos';
const LIST_URL = '/veterinario/duenos';
const LOAD_ERROR = 'No pudimos cargar los dueños. Revisa tu conexión e inténtalo de nuevo.';
const PANEL_PETS_MAX = 4;

// Las mascotas se piden aparte de los dueños: si fallan, la lista sigue y el total queda sin dato.
interface OwnersData {
  owners: Owner[];
  pets: Pet[] | null;
}

const LOADING = { status: 'loading' } as const;

const CONFIG: ListQueryConfig = {
  filterKeys: OWNER_FILTER_KEYS,
  sortFields: Object.keys(OWNER_SORT_ACCESSORS),
  defaultSort: { field: 'name', direction: 'asc' },
  defaultView: readViewPreference(VIEW_RESOURCE, 'table'),
};

/**
 * Dueños del veterinario (11.1): lista en tabla (por defecto) o tarjetas con búsqueda, chip de filtro,
 * orden y paginación en la URL, y panel de detalle con sus mascotas. Sin barra lateral contextual.
 * La lista, `duenos/add` y `duenos/update/:id` son una misma ruta: el diálogo de alta o edición se
 * abre según la URL y cerrarlo vuelve a `duenos` sin recrear la lista.
 */
@Component({
  selector: 'app-owners-list',
  imports: [
    FormsModule,
    RouterLink,
    ButtonModule,
    TableModule,
    TagModule,
    ToggleButtonModule,
    LucidePencil,
    LucidePlus,
    LucideTrash2,
    AvatarComponent,
    DefinitionListComponent,
    DetailPanelComponent,
    ListItemComponent,
    OwnerFormDialogComponent,
    PageHeaderComponent,
    ResourceCardComponent,
    ResourceListComponent,
    SelectableRowDirective,
    SortHeaderComponent,
  ],
  templateUrl: './owners-list.component.html',
})
export class OwnersListComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ownerService = inject(OwnerService);
  private readonly petService = inject(PetService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly shell = inject(ShellSidebarService);

  protected readonly panelOpen = signal(false);
  protected readonly selectedId = signal<number | null>(null);

  protected readonly state = signal<ViewState<OwnersData>>(LOADING);
  private readonly reload$ = new Subject<void>();
  private readonly deleting = signal(false);
  // Dueño recién guardado: se selecciona cuando llega la lista recargada que ya lo incluye.
  private pendingSelectId: number | null = null;

  protected readonly query = toSignal(
    this.route.queryParams.pipe(
      map((params) =>
        parseListQuery(new URLSearchParams(Object.entries(params).map(([key, value]) => [key, String(value)])), CONFIG),
      ),
    ),
    { initialValue: parseListQuery(new URLSearchParams(), CONFIG) },
  );

  // Diálogo según la URL: `add` abre el alta y `update/:id` la edición de ese dueño.
  private readonly dialogRoute = toSignal(
    this.route.url.pipe(map((segments) => ownerDialogRoute(segments.map((segment) => segment.path)))),
    { initialValue: ownerDialogRoute(this.route.snapshot.url.map((segment) => segment.path)) },
  );
  private readonly dialogEditId = computed(() => {
    const route = this.dialogRoute();
    return route?.mode === 'edit' ? parseRouteId(route.rawId) : null;
  });
  protected readonly dialogOpen = computed(() => {
    const route = this.dialogRoute();
    return route?.mode === 'create' || (route?.mode === 'edit' && this.dialogEditId() !== null);
  });
  protected readonly dialogOwnerId = this.dialogEditId;

  protected readonly rows = computed<OwnerRow[]>(() => {
    const state = this.state();
    return state.status === 'ready' ? buildOwnerRows(state.data.owners, state.data.pets) : [];
  });
  protected readonly pets = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? state.data.pets : null;
  });

  private readonly filtered = computed(() => {
    const { search, filters } = this.query();
    const criteria = { search, multiplePets: !!filters['varias'] };
    return this.rows().filter((row) => ownerMatches(row, criteria));
  });
  protected readonly total = computed(() => this.filtered().length);
  protected readonly page = computed(() => clampPage(this.query().page, this.total(), this.query().pageSize));
  protected readonly pageItems = computed(() =>
    paginate(sortItems(this.filtered(), this.query().sort, OWNER_SORT_ACCESSORS), this.page(), this.query().pageSize),
  );

  protected readonly loading = computed(() => this.state().status === 'loading');
  protected readonly error = computed(() => (this.state().status === 'error' ? LOAD_ERROR : null));
  protected readonly filtersActive = computed(() => Object.keys(this.query().filters).length > 0);
  protected readonly subtitle = computed(() =>
    this.state().status === 'ready' ? ownersCountLabel(this.rows().length) : null,
  );

  protected readonly current = computed(() => this.rows().find((row) => row.id === this.selectedId()) ?? null);
  protected readonly currentHue = computed(() => identityHue(this.current()?.id ?? 0));
  protected readonly currentPets = computed(() => {
    const row = this.current();
    return row ? petsOfOwner(this.pets(), row.id) : [];
  });
  protected readonly panelPets = computed(() => this.currentPets().slice(0, PANEL_PETS_MAX));
  protected readonly hiddenPets = computed(() => this.currentPets().length - this.panelPets().length);

  protected readonly newPetUrl = newPetUrl();
  protected readonly ownerPetQuery = ownerPetQuery;
  protected readonly initials = initials;
  protected readonly petsCountLabel = petsCountLabel;
  protected readonly speciesLabel = speciesLabel;
  protected readonly ageLabel = ageLabel;
  protected readonly withSearch = withSearch;
  protected readonly withPage = withPage;
  protected readonly withPageSize = withPageSize;
  protected readonly clearAll = clearAll;

  constructor() {
    this.reload$
      .pipe(
        switchMap(() =>
          forkJoin({
            owners: this.ownerService.getAll(),
            // Sin las mascotas la lista sigue siendo útil: el total de cada dueño queda como «—».
            pets: this.petService.getAll().pipe(catchError(() => of(null))),
          }).pipe(toViewState<OwnersData>()),
        ),
        takeUntilDestroyed(),
      )
      // Al recargar tras guardar o eliminar se conservan los datos que ya se ven.
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
        this.notifications.error('Dueño no encontrado', null);
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

  protected status(pet: Pet) {
    return petStatus(pet);
  }

  /** El diálogo guardó: la lista se recarga y el panel sigue al dueño guardado. */
  protected onSaved(owner: Owner): void {
    this.pendingSelectId = owner.id;
    this.reload$.next();
  }

  protected closeDialog(): void {
    void this.router.navigate([LIST_URL], { queryParamsHandling: 'preserve' });
  }

  // La confirmación usa el total de mascotas que la lista ya conoce, sin otra petición.
  protected remove(owner: OwnerRow): void {
    confirmOwnerDeletion(this.confirmationService, owner, owner.petsCount, () => this.deleteOwner(owner));
  }

  // Si el API rechaza la eliminación (p. ej. 409 por tratamientos registrados) muestra su mensaje y
  // deja la lista como estaba; si funciona, avisa y recarga.
  private deleteOwner(owner: OwnerRow): void {
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
          if (this.selectedId() === owner.id) {
            this.selectedId.set(null);
          }
          this.reload$.next();
        },
        error: (error: unknown) => this.notifications.error('No se pudo eliminar al dueño', error),
      });
  }
}
