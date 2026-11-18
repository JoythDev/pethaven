import { Owner } from '../../models/owner.model';
import { Pet } from '../../models/pet.model';
import { SortAccessors } from '../list/list-sort';
import { normalizeText } from '../text/normalize-text';

/** Clave del filtro «Con varias mascotas» en la URL. */
export const OWNER_FILTER_KEYS = ['varias'] as const;

/** Dueño con su cantidad de mascotas; null si no se pudieron consultar. */
export interface OwnerRow extends Owner {
  petsCount: number | null;
}

export const OWNER_SORT_ACCESSORS: SortAccessors<OwnerRow> = {
  name: (o) => o.name,
  document: (o) => o.document,
  phone: (o) => o.phone,
  email: (o) => o.email,
  pets: (o) => o.petsCount,
};

/** Cuenta las mascotas de cada dueño; con `pets` null el total queda desconocido. */
export function buildOwnerRows(owners: readonly Owner[], pets: readonly Pet[] | null): OwnerRow[] {
  const counts = new Map<number, number>();
  for (const pet of pets ?? []) {
    counts.set(pet.ownerId, (counts.get(pet.ownerId) ?? 0) + 1);
  }
  return owners.map((owner) => ({ ...owner, petsCount: pets === null ? null : (counts.get(owner.id) ?? 0) }));
}

/** Mascotas de un dueño, en el orden recibido. */
export const petsOfOwner = (pets: readonly Pet[] | null, ownerId: number): Pet[] =>
  (pets ?? []).filter((pet) => pet.ownerId === ownerId);

export interface OwnerCriteria {
  search: string;
  /** Solo dueños con más de una mascota. */
  multiplePets: boolean;
}

/** Búsqueda por nombre, cédula o correo (sin tildes ni mayúsculas) más «Con varias mascotas». */
export function ownerMatches(owner: OwnerRow, criteria: OwnerCriteria): boolean {
  if (criteria.multiplePets && (owner.petsCount ?? 0) <= 1) {
    return false;
  }
  const term = normalizeText(criteria.search);
  return !term || normalizeText(`${owner.name} ${owner.document} ${owner.email}`).includes(term);
}

export const ownersCountLabel = (total: number): string =>
  `${total} ${total === 1 ? 'dueño registrado' : 'dueños registrados'}`;

/** «Sin mascotas», «1 mascota», «3 mascotas»; «—» si el total no se pudo consultar. */
export function petsCountLabel(count: number | null): string {
  if (count === null) {
    return '—';
  }
  return count === 0 ? 'Sin mascotas' : `${count} ${count === 1 ? 'mascota' : 'mascotas'}`;
}
