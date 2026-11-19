import { NgTemplateOutlet } from '@angular/common';
import { Component, DestroyRef, TemplateRef, computed, contentChild, effect, inject, input, output, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  LucideChevronLeft,
  LucideChevronRight,
  LucideCircleAlert,
  LucideLayoutGrid,
  LucideSearch,
  LucideTable,
  LucideX,
} from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';

import { DEFAULT_PAGE_SIZE, ListView, PAGE_SIZES, PageSize } from '../../../core/list/list-query';
import { pageCount, rangeText } from '../../../core/list/pagination';
import { StickerName } from '../../../core/stickers/stickers';
import { skeletonVisible } from '../../../core/ui/skeleton-timing';
import { EmptyStateComponent } from '../empty-state/empty-state.component';
import { ShellSidebarService } from '../shell/shell-sidebar.service';
import { SkeletonComponent } from '../skeleton/skeleton.component';
import { StickerComponent } from '../sticker/sticker.component';

export type ListItemId = number | string;

/** La búsqueda de una lista se emite 250 ms después de la última tecla (11.1). */
export const SEARCH_DEBOUNCE_MS = 250;

/** Contexto de la plantilla `#card`: una por elemento de la página actual. */
export interface ResourceCardContext<T> {
  $implicit: T;
  selected: boolean;
  /** Selecciona este elemento: hacerlo emite `itemSelect` con su id. */
  select: () => void;
}

/** Contexto de la plantilla `#table`: el `p-table` completo con los elementos de la página actual. */
export interface ResourceTableContext<T> {
  $implicit: readonly T[];
  selectedId: ListItemId | null;
  select: (id: ListItemId) => void;
}

const defaultItemId = (item: unknown): ListItemId => (item as { id: ListItemId }).id;

const VIEW_OPTIONS: { value: ListView; label: string; icon: 'grid' | 'table' }[] = [
  { value: 'cards', label: 'Vista de tarjetas', icon: 'grid' },
  { value: 'table', label: 'Vista de tabla', icon: 'table' },
];

// Columnas de la rejilla según el ancho de la lista (contenedor): 2 desde 440 px, 3 desde 700 y 4 desde 940.
// En compacto siempre una (span 12, 10.12).
const GRID_CLASS = 'grid gap-3.5 grid-cols-1 @[440px]:grid-cols-2 @[700px]:grid-cols-3 @[940px]:grid-cols-4';
const GRID_CLASS_COMPACT = 'grid gap-3.5 grid-cols-1';

const PAGE_SIZE_OPTIONS = PAGE_SIZES.map((size) => ({ value: size, label: `${size} por página` }));

/**
 * Marco de una lista de recursos (11.1): barra de herramientas con búsqueda, filtros y cambio de
 * vista; cuerpo en tarjetas o tabla; paginación; y los estados de carga, vacío, sin resultados y
 * error. Es presentacional: la página filtra, ordena y pagina, y aquí solo recibe la página actual.
 *
 * - Ranuras: `[filters]` (chips de filtro) y `[emptyAction]` (acción del estado «aún no hay datos»).
 * - Plantillas: `<ng-template #card let-item let-selected="selected" let-select="select">` y
 *   `<ng-template #table let-items let-selectedId="selectedId" let-select="select">` con el `p-table`.
 * - Va en una rejilla de 12 columnas: ocupa 8 con el panel de detalle a su lado (`withPanel`) y 12 si no.
 * - Por debajo de 680 px la tabla no se ofrece y la lista siempre va en tarjetas (9.9).
 */
@Component({
  selector: 'app-resource-list',
  imports: [
    NgTemplateOutlet,
    FormsModule,
    ButtonModule,
    InputTextModule,
    MessageModule,
    SelectModule,
    SelectButtonModule,
    TooltipModule,
    EmptyStateComponent,
    SkeletonComponent,
    StickerComponent,
    LucideChevronLeft,
    LucideChevronRight,
    LucideCircleAlert,
    LucideLayoutGrid,
    LucideSearch,
    LucideTable,
    LucideX,
  ],
  templateUrl: './resource-list.component.html',
  host: { '[class]': 'hostClass()' },
})
export class ResourceListComponent<T> {
  /** Elementos de la página actual. */
  readonly items = input.required<readonly T[]>();
  /** Total de elementos que cumplen búsqueda y filtros, no solo los de la página. */
  readonly total = input.required<number>();
  readonly page = input(1);
  readonly pageSize = input<PageSize>(DEFAULT_PAGE_SIZE);
  readonly view = input<ListView>('cards');
  readonly searchText = input('');
  /** `id` del buscador; lo usan las pruebas E2E (Selenium) para localizarlo. */
  readonly searchId = input<string | null>(null);
  readonly searchLabel = input('Buscar');
  readonly searchPlaceholder = input('Buscar');
  readonly loading = input(false);
  /** Mensaje del error de carga; null si no hay. */
  readonly error = input<string | null>(null);
  /** Hay algún filtro activo: muestra «Quitar filtros». */
  readonly filtersActive = input(false);
  readonly selectedId = input<ListItemId | null>(null);
  readonly itemId = input<(item: T) => ListItemId>(defaultItemId);
  /** Con panel de detalle al lado la lista ocupa 8 de 12 columnas. */
  readonly withPanel = input(true);
  readonly emptyHeading = input('Aún no hay registros');
  readonly emptyDescription = input('Cuando haya elementos aparecerán aquí.');
  readonly emptySticker = input<StickerName>('paw');

  readonly searchChange = output<string>();
  readonly viewChange = output<ListView>();
  readonly pageChange = output<number>();
  readonly pageSizeChange = output<PageSize>();
  readonly itemSelect = output<ListItemId>();
  /** «Quitar filtros»: la página decide si también limpia la búsqueda (lo hace en «sin resultados»). */
  readonly clearFilters = output<void>();
  readonly retry = output<void>();

  protected readonly cardTemplate = contentChild<TemplateRef<ResourceCardContext<T>>>('card');
  protected readonly tableTemplate = contentChild<TemplateRef<ResourceTableContext<T>>>('table');

  private readonly shell = inject(ShellSidebarService);

  protected readonly viewOptions = VIEW_OPTIONS;
  protected readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  protected readonly skeletonRows = Array.from({ length: 8 });

  protected readonly compact = computed(() => this.shell.layout() === 'compact');
  protected readonly effectiveView = computed<ListView>(() => (this.compact() ? 'cards' : this.view()));
  protected readonly gridClass = computed(() => (this.compact() ? GRID_CLASS_COMPACT : GRID_CLASS));
  protected readonly pageTotal = computed(() => pageCount(this.total(), this.pageSize()));
  protected readonly range = computed(() => rangeText(this.total(), this.page(), this.pageSize()));
  protected readonly hasQuery = computed(() => this.filtersActive() || !!this.searchText().trim());
  protected readonly showSkeleton = skeletonVisible(this.loading);

  protected readonly noResultsText = computed(() => {
    const term = this.searchText().trim();
    return term
      ? `Nada coincide con «${term}». Prueba con otra palabra o quita los filtros.`
      : 'Ningún elemento cumple los filtros que elegiste.';
  });

  protected readonly hostClass = computed(
    () => 'block min-w-0 ' + (this.withPanel() && this.shell.layout() === 'expanded' ? 'col-span-8' : 'col-span-12'),
  );

  // Texto del campo: lo que escribe la persona, antes del antirrebote.
  protected readonly text = signal('');
  private timer: ReturnType<typeof setTimeout> | undefined;
  // Último texto emitido: si la página lo devuelve tarde, no pisa lo que la persona siguió escribiendo.
  private lastEmitted = '';

  constructor() {
    // Solo un cambio externo de la búsqueda (p. ej. «Quitar filtros») reescribe el campo.
    effect(() => {
      const incoming = this.searchText();
      untracked(() => {
        if (incoming !== this.lastEmitted) {
          clearTimeout(this.timer);
          this.lastEmitted = incoming;
          this.text.set(incoming);
        }
      });
    });
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timer));
  }

  protected onSearchInput(value: string): void {
    this.text.set(value);
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.emitSearch(value), SEARCH_DEBOUNCE_MS);
  }

  /** Enter no espera al antirrebote. */
  protected flushSearch(): void {
    clearTimeout(this.timer);
    this.emitSearch(this.text());
  }

  protected clearSearch(): void {
    clearTimeout(this.timer);
    this.text.set('');
    this.emitSearch('');
  }

  private emitSearch(value: string): void {
    this.lastEmitted = value.trim();
    this.searchChange.emit(this.lastEmitted);
  }

  protected cardContext(item: T): ResourceCardContext<T> {
    const id = this.itemId()(item);
    return { $implicit: item, selected: id === this.selectedId(), select: () => this.itemSelect.emit(id) };
  }

  protected tableContext(): ResourceTableContext<T> {
    return {
      $implicit: this.items(),
      selectedId: this.selectedId(),
      select: (id) => this.itemSelect.emit(id),
    };
  }

  protected onViewChange(value: ListView | null): void {
    if (value) {
      this.viewChange.emit(value);
    }
  }
}
