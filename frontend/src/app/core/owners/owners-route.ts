import { UrlMatchResult, UrlSegment } from '@angular/router';

/** Qué muestra la ruta de dueños: la lista sola o con el diálogo de alta o de edición abierto. */
export type OwnerDialogRoute = { mode: 'list' } | { mode: 'create' } | { mode: 'edit'; rawId: string };

/** Lee los segmentos de la URL tras `duenos`: «», «add» o «update/:id». Cualquier otra cosa no coincide. */
export function ownerDialogRoute(segments: readonly string[]): OwnerDialogRoute | null {
  if (segments.length === 0) return { mode: 'list' };
  if (segments.length === 1 && segments[0] === 'add') return { mode: 'create' };
  if (segments.length === 2 && segments[0] === 'update') return { mode: 'edit', rawId: segments[1] };
  return null;
}

/** Una sola ruta para la lista y sus dos diálogos: así la lista no se vuelve a crear al abrir o cerrar el diálogo. */
export function ownersListMatcher(segments: UrlSegment[]): UrlMatchResult | null {
  const route = ownerDialogRoute(segments.map((segment) => segment.path));
  if (!route) return null;
  return {
    consumed: segments,
    posParams: route.mode === 'edit' ? { id: segments[1] } : undefined,
  };
}
