import { ListView } from './list-query';

type ViewStorage = Pick<Storage, 'getItem' | 'setItem'>;

export function viewStorageKey(resource: string): string {
  return `pethaven.view.${resource}`;
}

// Acceder a localStorage puede lanzar (ventana privada, datos bloqueados); en ese caso no hay preferencia.
function browserStorage(): ViewStorage | null {
  try {
    return localStorage;
  } catch {
    return null;
  }
}

/** Vista guardada para el recurso o, si no hay (o el almacenamiento falla), la que le toca por defecto. */
export function readViewPreference(
  resource: string,
  fallback: ListView,
  storage: ViewStorage | null = browserStorage(),
): ListView {
  try {
    const stored = storage?.getItem(viewStorageKey(resource));
    return stored === 'cards' || stored === 'table' ? stored : fallback;
  } catch {
    return fallback;
  }
}

export function writeViewPreference(
  resource: string,
  view: ListView,
  storage: ViewStorage | null = browserStorage(),
): void {
  try {
    storage?.setItem(viewStorageKey(resource), view);
  } catch {
    // Sin almacenamiento la elección solo vale para esta visita.
  }
}
