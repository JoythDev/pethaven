import { UrlMatchResult, UrlSegment } from '@angular/router';

/** Qué muestra una ruta con diálogo de alta y edición: la lista sola o con el diálogo abierto. */
export type ResourceDialogRoute = { mode: 'list' } | { mode: 'create' } | { mode: 'edit'; rawId: string };

/** Lee los segmentos de la URL tras el recurso: «», «add» o «update/:id». Cualquier otra cosa no coincide. */
export function resourceDialogRoute(segments: readonly string[]): ResourceDialogRoute | null {
  if (segments.length === 0) return { mode: 'list' };
  if (segments.length === 1 && segments[0] === 'add') return { mode: 'create' };
  if (segments.length === 2 && segments[0] === 'update') return { mode: 'edit', rawId: segments[1] };
  return null;
}

/** Una sola ruta para la lista y sus dos diálogos: así la lista no se vuelve a crear al abrir o cerrar el diálogo. */
export function resourceListMatcher(segments: UrlSegment[]): UrlMatchResult | null {
  const route = resourceDialogRoute(segments.map((segment) => segment.path));
  if (!route) return null;
  return {
    consumed: segments,
    posParams: route.mode === 'edit' ? { id: segments[1] } : undefined,
  };
}

// Dueños y veterinarios comparten el mismo patrón; estos nombres conservan los usos existentes.
export type OwnerDialogRoute = ResourceDialogRoute;
export const ownerDialogRoute = resourceDialogRoute;
export const ownersListMatcher = resourceListMatcher;
