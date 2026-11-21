/** Iconos de las secciones; el shell los resuelve, así los layouts no importan iconos. */
export type ShellIconName =
  | 'house'
  | 'calendar'
  | 'paw-print'
  | 'users'
  | 'package'
  | 'bell'
  | 'user'
  | 'chart-column'
  | 'stethoscope';

/** Sección de primer nivel de un rol: una pestaña arriba y un ítem en la barra inferior. */
export interface ShellTab {
  label: string;
  icon: ShellIconName;
  link: string;
  /** La pestaña de inicio solo está activa en su ruta exacta. */
  exact?: boolean;
}

/** Configuración del shell para un rol. */
export interface ShellConfig {
  tabs: ShellTab[];
  roleLabel: string;
  userName: string;
  userId: number | string | null;
  userPhotoUrl: string | null;
  profileLink: string;
}

const MAX_BAR_ITEMS = 5;

function pathSegments(url: string): string[] {
  return url.split(/[?#]/)[0].split('/').filter(Boolean);
}

/** Índice de la pestaña activa según la URL (la sección de primer nivel, también en rutas anidadas); -1 si ninguna. */
export function activeTabIndex(tabs: readonly ShellTab[], url: string): number {
  const current = pathSegments(url);
  return tabs.findIndex((tab) => {
    const target = pathSegments(tab.link);
    if (tab.exact) {
      return target.length === current.length && target.every((s, i) => s === current[i]);
    }
    return target.length <= current.length && target.every((s, i) => s === current[i]);
  });
}

/** Barra inferior: hasta cinco ítems; con más secciones, las cuatro primeras y «Más» con el resto. */
export function splitBottomTabs<T>(tabs: readonly T[]): { visible: T[]; overflow: T[] } {
  if (tabs.length <= MAX_BAR_ITEMS) {
    return { visible: [...tabs], overflow: [] };
  }
  return { visible: tabs.slice(0, MAX_BAR_ITEMS - 1), overflow: tabs.slice(MAX_BAR_ITEMS - 1) };
}

export type ShellLayout = 'compact' | 'medium' | 'expanded';

/** Disposición según el ancho del contenedor de la aplicación (9.1): <680, 680–1079, ≥1080. */
export function layoutForWidth(width: number): ShellLayout {
  if (width < 680) {
    return 'compact';
  }
  return width < 1080 ? 'medium' : 'expanded';
}
