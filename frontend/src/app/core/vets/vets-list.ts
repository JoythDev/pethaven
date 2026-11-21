import { Veterinarian } from '../../models/veterinarian.model';
import { SortAccessors } from '../list/list-sort';
import { normalizeText } from '../text/normalize-text';

/** Claves de filtro de la lista de veterinarios en la URL. */
export const VET_FILTER_KEYS = ['estado'] as const;

/** Valores del filtro `estado`. */
export const VET_STATUS_FILTERS = { active: 'activo', inactive: 'inactivo' } as const;

export const vetStatusLabel = (active: boolean): string => (active ? 'Activo' : 'Inactivo');

export const VET_SORT_ACCESSORS: SortAccessors<Veterinarian> = {
  name: (v) => v.name,
  specialty: (v) => v.specialty,
  document: (v) => v.document,
  attentions: (v) => v.attentions,
  // Activos primero en orden ascendente.
  status: (v) => (v.active ? 0 : 1),
};

export interface VetCriteria {
  search: string;
  /** `activo` o `inactivo`; vacío no filtra. */
  status: string;
}

/** Búsqueda por nombre, cédula o especialidad (sin tildes ni mayúsculas) más el estado de la cuenta. */
export function vetMatches(vet: Veterinarian, criteria: VetCriteria): boolean {
  if (criteria.status && vet.active !== (criteria.status === VET_STATUS_FILTERS.active)) {
    return false;
  }
  const term = normalizeText(criteria.search);
  return !term || normalizeText(`${vet.name} ${vet.document} ${vet.specialty}`).includes(term);
}

/** «5 activos de 6». */
export function vetsCountLabel(vets: readonly Veterinarian[]): string {
  const active = vets.filter((vet) => vet.active).length;
  return `${active} ${active === 1 ? 'activo' : 'activos'} de ${vets.length}`;
}

/** Sustituye el veterinario por id y conserva el orden y las demás referencias. */
export const replaceVet = (vets: readonly Veterinarian[], updated: Veterinarian): Veterinarian[] =>
  vets.map((vet) => (vet.id === updated.id ? updated : vet));

/** Aviso de éxito al activar o desactivar un veterinario. */
export function vetActiveToast(vet: Pick<Veterinarian, 'name' | 'active'>): { summary: string; detail: string } {
  return {
    summary: vet.active ? 'Veterinario activado' : 'Veterinario desactivado',
    detail: `${vet.name} ahora está ${vet.active ? 'activo' : 'inactivo'}.`,
  };
}
