import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideArrowUpRight, LucidePencil, LucidePlus, LucideStethoscope } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToggleButtonModule } from 'primeng/togglebutton';
import { Subject, map, of, switchMap } from 'rxjs';

import { ViewState, toViewState } from '../../../../core/http/view-state';
import { identityHue } from '../../../../core/identity/identity';
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
import { resolvePetArea } from '../../../../core/pets-list/pet-area';
import {
  PET_FILTER_KEYS,
  PET_SORT_ACCESSORS,
  PET_STATUS_FILTERS,
  ageLabel,
  lastTreatment,
  patientsCountLabel,
  petMatches,
  registeredPetsLabel,
  replacePet,
  speciesLabel,
} from '../../../../core/pets-list/pets-list';
import { petStatus } from '../../../../core/status/pet-status';
import { coatKitFor, speciesSticker } from '../../../../core/stickers/stickers';
import { skeletonVisible } from '../../../../core/ui/skeleton-timing';
import { Pet, Species } from '../../../../models/pet.model';
import { Treatment } from '../../../../models/treatment.model';
import { AuthService } from '../../../../services/auth.service';
import { PetService } from '../../../../services/pet.service';
import { TreatmentService } from '../../../../services/treatment.service';
import { VeterinarianService } from '../../../../services/veterinarian.service';
import { AvatarComponent } from '../../../shared/avatar/avatar.component';
import { DefinitionListComponent } from '../../../shared/definition-list/definition-list.component';
import { DetailPanelComponent } from '../../../shared/detail-panel/detail-panel.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { PetActiveSwitchComponent } from '../../../shared/pet-active-switch/pet-active-switch.component';
import { ResourceCardComponent } from '../../../shared/resource-card/resource-card.component';
import { ResourceListComponent } from '../../../shared/resource-list/resource-list.component';
import { SelectableRowDirective } from '../../../shared/resource-list/selectable-row.directive';
import { SortHeaderComponent } from '../../../shared/resource-list/sort-header.component';
import { ShellSidebarService } from '../../../shared/shell/shell-sidebar.service';
import { StickerComponent } from '../../../shared/sticker/sticker.component';

const VIEW_RESOURCE = 'mascotas';
const LOAD_ERROR = 'No pudimos cargar las mascotas. Revisa tu conexión e inténtalo de nuevo.';
const LOADING = { status: 'loading' } as const;

const CONFIG: ListQueryConfig = {
  filterKeys: PET_FILTER_KEYS,
  sortFields: Object.keys(PET_SORT_ACCESSORS),
  defaultSort: { field: 'name', direction: 'asc' },
  defaultView: readViewPreference(VIEW_RESOURCE, 'cards'),
};

/**
 * Mascotas (11.1): lista en tarjetas o tabla con búsqueda, chips de filtro, orden y paginación en la
 * URL, y panel de detalle. La monta el veterinario, con «Nueva mascota», «Mis pacientes», el
 * interruptor «Mascota activa» (11.11), «Nuevo tratamiento» y «Editar», y el administrador, que solo
 * consulta (11.7): el área llega por el `data` de la ruta. Sin barra lateral contextual (9.5).
 * Cambiar el estado actualiza la mascota en su sitio, sin recargar.
 */
@Component({
  selector: 'app-pets-list',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    RouterLink,
    ButtonModule,
    SkeletonModule,
    TableModule,
    TagModule,
    ToggleButtonModule,
    LucideArrowUpRight,
    LucidePencil,
    LucidePlus,
    LucideStethoscope,
    AvatarComponent,
    DefinitionListComponent,
    DetailPanelComponent,
    PageHeaderComponent,
    PetActiveSwitchComponent,
    ResourceCardComponent,
    ResourceListComponent,
    SelectableRowDirective,
    SortHeaderComponent,
    StickerComponent,
  ],
  templateUrl: './pets-list.component.html',
})
export class PetsListComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly petService = inject(PetService);
  private readonly treatmentService = inject(TreatmentService);
  private readonly veterinarianService = inject(VeterinarianService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly shell = inject(ShellSidebarService);

  protected readonly area = resolvePetArea(this.route.snapshot.data);
  protected readonly dog = Species.DOG;
  protected readonly cat = Species.CAT;
  protected readonly activeFilter = PET_STATUS_FILTERS.active;
  protected readonly inactiveFilter = PET_STATUS_FILTERS.inactive;
  protected readonly panelOpen = signal(false);
  protected readonly selectedId = signal<number | null>(null);

  protected readonly state = signal<ViewState<Pet[]>>(LOADING);
  private readonly reload$ = new Subject<void>();

  // Pacientes del veterinario (para «Mis pacientes»); se piden una vez, la primera vez que se usa el chip.
  private readonly patientIds = signal<ReadonlySet<number> | null>(null);
  private readonly patientsLoading = signal(false);

  protected readonly query = toSignal(
    this.route.queryParams.pipe(
      map((params) =>
        parseListQuery(new URLSearchParams(Object.entries(params).map(([key, value]) => [key, String(value)])), CONFIG),
      ),
    ),
    { initialValue: parseListQuery(new URLSearchParams(), CONFIG) },
  );

  protected readonly pets = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? state.data : [];
  });
  private readonly mineActive = computed(() => this.area.canManage && !!this.query().filters['mios']);

  private readonly filtered = computed(() => {
    const { search, filters } = this.query();
    const patientIds = this.mineActive() ? (this.patientIds() ?? new Set<number>()) : null;
    const criteria = { search, species: filters['especie'] ?? '', status: filters['estado'] ?? '', patientIds };
    return this.pets().filter((pet) => petMatches(pet, criteria));
  });
  protected readonly total = computed(() => this.filtered().length);
  protected readonly page = computed(() => clampPage(this.query().page, this.total(), this.query().pageSize));
  protected readonly pageItems = computed(() =>
    paginate(sortItems(this.filtered(), this.query().sort, PET_SORT_ACCESSORS), this.page(), this.query().pageSize),
  );

  protected readonly loading = computed(() => this.state().status === 'loading' || this.patientsLoading());
  protected readonly error = computed(() => (this.state().status === 'error' ? LOAD_ERROR : null));
  protected readonly filtersActive = computed(() => Object.keys(this.query().filters).length > 0);
  protected readonly subtitle = computed(() => {
    if (this.state().status !== 'ready') {
      return null;
    }
    const total = this.pets().length;
    return this.area.canManage ? patientsCountLabel(total) : registeredPetsLabel(total);
  });

  protected readonly current = computed(() => this.pets().find((p) => p.id === this.selectedId()) ?? null);
  protected readonly currentHue = computed(() => identityHue(this.current()?.id ?? 0));

  // Último tratamiento real de la mascota elegida; cada selección pide el suyo y cancela el anterior.
  private readonly treatments = toSignal(
    toObservable(this.selectedId).pipe(
      switchMap((id) =>
        id === null
          ? of<ViewState<Treatment[]>>(LOADING)
          : this.treatmentService.getByPetId(id).pipe(toViewState<Treatment[]>()),
      ),
    ),
    { initialValue: LOADING as ViewState<Treatment[]> },
  );
  protected readonly treatmentsFailed = computed(() => this.treatments().status === 'error');
  protected readonly lastTreatment = computed(() => {
    const state = this.treatments();
    return state.status === 'ready' ? lastTreatment(state.data) : null;
  });
  protected readonly treatmentsReady = computed(() => this.treatments().status === 'ready');
  protected readonly treatmentsSkeleton = skeletonVisible(computed(() => this.treatments().status === 'loading'));

  protected readonly speciesLabel = speciesLabel;
  protected readonly ageLabel = ageLabel;
  protected readonly withSearch = withSearch;
  protected readonly withPage = withPage;
  protected readonly withPageSize = withPageSize;
  protected readonly clearAll = clearAll;

  constructor() {
    this.reload$
      .pipe(
        switchMap(() => this.petService.getAll().pipe(toViewState<Pet[]>())),
        takeUntilDestroyed(),
      )
      .subscribe((state) => this.state.set(state));
    this.reload$.next();

    // La página se corrige en la URL cuando la lista encoge.
    effect(() => {
      const query = this.query();
      if (this.state().status === 'ready' && query.page !== this.page()) {
        this.update(withPage(query, this.page()));
      }
    });

    // En la columna fija se selecciona la primera mascota al cargar (10.13); en cajón no, para no abrirlo solo.
    effect(() => {
      if (this.shell.layout() === 'expanded' && this.selectedId() === null && this.pageItems().length) {
        this.selectedId.set(this.pageItems()[0].id);
      }
    });

    // «Mis pacientes» necesita los tratamientos del veterinario: se piden la primera vez que se activa.
    effect(() => {
      if (this.mineActive()) {
        untracked(() => this.loadPatients());
      }
    });
  }

  protected retry(): void {
    this.state.set(LOADING);
    this.reload$.next();
  }

  protected status(pet: Pet) {
    return petStatus(pet);
  }

  protected sticker(pet: Pet) {
    return speciesSticker(pet.species);
  }

  protected kit(pet: Pet) {
    return coatKitFor(pet.id);
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

  // Un chip activo se quita al volver a pulsarlo; en especie y estado, elegir otro valor reemplaza el anterior.
  protected toggleFilter(key: string, value: string): void {
    const active = this.query().filters[key] === value;
    this.update(withFilter(this.query(), key, active ? '' : value));
  }

  /** El interruptor guardó el estado: la mascota se reemplaza en la lista y el panel la sigue por id. */
  protected onPetChanged(updated: Pet): void {
    this.state.update((s) => (s.status === 'ready' ? { status: 'ready', data: replacePet(s.data, updated) } : s));
  }

  private loadPatients(): void {
    const user = this.auth.user();
    if (this.patientIds() !== null || this.patientsLoading() || !user || user.role !== 'VETERINARIAN') {
      return;
    }
    this.patientsLoading.set(true);
    this.veterinarianService.getTreatmentsByVeterinarianId(user.id).subscribe({
      next: (treatments) => {
        this.patientIds.set(new Set(treatments.map((t) => t.petId)));
        this.patientsLoading.set(false);
      },
      error: (error: unknown) => {
        this.patientsLoading.set(false);
        this.notifications.error('No se pudieron cargar tus pacientes', error);
        this.update(withFilter(this.query(), 'mios', ''));
      },
    });
  }
}
