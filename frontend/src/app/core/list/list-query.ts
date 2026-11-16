export type ListView = 'cards' | 'table';
export type SortDirection = 'asc' | 'desc';

// Tamaños de página del sistema de diseño (10.10); 12 por defecto en tarjetas y en tabla.
export const PAGE_SIZES = [12, 24, 48] as const;
export type PageSize = (typeof PAGE_SIZES)[number];
export const DEFAULT_PAGE_SIZE: PageSize = 12;

export interface ListSort {
  field: string;
  direction: SortDirection;
}

/** Estado de una lista de recursos. La página es 1-based. */
export interface ListQuery {
  search: string;
  /** Solo filtros con valor: un valor vacío se quita. */
  filters: Readonly<Record<string, string>>;
  sort: ListSort | null;
  page: number;
  pageSize: PageSize;
  view: ListView;
}

/** Lo que cada página aporta para leer y escribir la URL. */
export interface ListQueryConfig {
  /** Claves de filtro que reconoce la lista; cualquier otro parámetro se ignora. */
  filterKeys: readonly string[];
  /** Campos ordenables; sin ella se acepta cualquier campo. */
  sortFields?: readonly string[];
  defaultSort?: ListSort | null;
  /** Vista que se usa cuando la URL no dice nada (la preferencia guardada o el valor por defecto del recurso). */
  defaultView: ListView;
}

// Nombres de los parámetros de la URL. Las claves de filtro van tal cual, así que no pueden coincidir con estas.
const PARAM = { search: 'q', sort: 'orden', direction: 'dir', page: 'pagina', pageSize: 'tamano', view: 'vista' } as const;

export function emptyListQuery(config: ListQueryConfig): ListQuery {
  return {
    search: '',
    filters: {},
    sort: config.defaultSort ?? null,
    page: 1,
    pageSize: DEFAULT_PAGE_SIZE,
    view: config.defaultView,
  };
}

function toPage(raw: string | null): number {
  const value = Number(raw);
  return raw !== null && Number.isInteger(value) && value >= 1 ? value : 1;
}

function toPageSize(raw: string | null): PageSize {
  const value = Number(raw);
  return PAGE_SIZES.find((size) => size === value) ?? DEFAULT_PAGE_SIZE;
}

function toView(raw: string | null, fallback: ListView): ListView {
  return raw === 'cards' || raw === 'table' ? raw : fallback;
}

function toSort(params: URLSearchParams, config: ListQueryConfig): ListSort | null {
  const field = params.get(PARAM.sort);
  if (!field || (config.sortFields && !config.sortFields.includes(field))) {
    return config.defaultSort ?? null;
  }
  return { field, direction: params.get(PARAM.direction) === 'desc' ? 'desc' : 'asc' };
}

/** Lee el estado de la lista de los parámetros de la URL; lo inválido o ausente cae en el valor por defecto. */
export function parseListQuery(params: URLSearchParams, config: ListQueryConfig): ListQuery {
  const filters: Record<string, string> = {};
  for (const key of config.filterKeys) {
    const value = params.get(key);
    if (value) {
      filters[key] = value;
    }
  }
  return {
    search: params.get(PARAM.search) ?? '',
    filters,
    sort: toSort(params, config),
    page: toPage(params.get(PARAM.page)),
    pageSize: toPageSize(params.get(PARAM.pageSize)),
    view: toView(params.get(PARAM.view), config.defaultView),
  };
}

function sameSort(a: ListSort | null, b: ListSort | null): boolean {
  return a === b || (!!a && !!b && a.field === b.field && a.direction === b.direction);
}

/** Parámetros de la URL para el estado dado; omite todo lo que sea igual al valor por defecto. */
export function serializeListQuery(query: ListQuery, config: ListQueryConfig): Record<string, string> {
  const params: Record<string, string> = {};
  if (query.search) {
    params[PARAM.search] = query.search;
  }
  for (const key of config.filterKeys) {
    const value = query.filters[key];
    if (value) {
      params[key] = value;
    }
  }
  if (query.sort && !sameSort(query.sort, config.defaultSort ?? null)) {
    params[PARAM.sort] = query.sort.field;
    if (query.sort.direction === 'desc') {
      params[PARAM.direction] = 'desc';
    }
  }
  if (query.page > 1) {
    params[PARAM.page] = String(query.page);
  }
  if (query.pageSize !== DEFAULT_PAGE_SIZE) {
    params[PARAM.pageSize] = String(query.pageSize);
  }
  if (query.view !== config.defaultView) {
    params[PARAM.view] = query.view;
  }
  return params;
}

// Cambiar la búsqueda, los filtros o el orden devuelve siempre a la página 1 (10.10).
export function withSearch(query: ListQuery, search: string): ListQuery {
  return { ...query, search: search.trim(), page: 1 };
}

export function withFilter(query: ListQuery, key: string, value: string): ListQuery {
  const { [key]: _removed, ...rest } = query.filters;
  return { ...query, filters: value ? { ...rest, [key]: value } : rest, page: 1 };
}

export function withSort(query: ListQuery, sort: ListSort | null): ListQuery {
  return { ...query, sort, page: 1 };
}

/** «Quitar filtros»: deja la búsqueda. */
export function clearFilters(query: ListQuery): ListQuery {
  return { ...query, filters: {}, page: 1 };
}

/** Quita búsqueda y filtros; es lo que ofrece el estado «sin resultados». */
export function clearAll(query: ListQuery): ListQuery {
  return { ...query, search: '', filters: {}, page: 1 };
}

export function withPage(query: ListQuery, page: number): ListQuery {
  return { ...query, page: Number.isFinite(page) ? Math.max(1, Math.trunc(page)) : 1 };
}

export function withPageSize(query: ListQuery, pageSize: PageSize): ListQuery {
  return { ...query, pageSize, page: 1 };
}

/** Cambiar de vista conserva filtros, orden y página: las dos vistas comparten todo (10.11). */
export function withView(query: ListQuery, view: ListView): ListQuery {
  return { ...query, view };
}

/** Orden de un encabezado: un campo nuevo empieza ascendente; el mismo campo invierte la dirección. */
export function toggleSort(current: ListSort | null, field: string): ListSort {
  if (current?.field === field) {
    return { field, direction: current.direction === 'asc' ? 'desc' : 'asc' };
  }
  return { field, direction: 'asc' };
}
