import { AuthRole } from '../../models/auth.model';
import { Drug } from '../../models/drug.model';
import { Owner } from '../../models/owner.model';
import { Pet, Species } from '../../models/pet.model';
import { Veterinarian } from '../../models/veterinarian.model';
import { normalizeText } from '../text/normalize-text';

export type SearchKind = 'pet' | 'owner' | 'drug' | 'veterinarian';

export const SEARCH_GROUP_LABELS: Record<SearchKind, string> = {
  pet: 'Mascotas',
  owner: 'Dueños',
  drug: 'Medicamentos',
  veterinarian: 'Veterinarios',
};

/** Resultados que muestra cada grupo (11.16). */
export const SEARCH_GROUP_LIMIT = 5;

export interface SearchEntry {
  kind: SearchKind;
  id: number;
  label: string;
  /** Segunda línea del resultado. */
  detail: string;
  route: string;
  /** Texto extra por el que también se encuentra el recurso (raza, documento, especialidad…). */
  keywords: string;
  /** Solo las mascotas: el avatar usa la especie y la foto. */
  species?: Species;
  photoUrl?: string | null;
}

export interface SearchGroup {
  kind: SearchKind;
  label: string;
  entries: SearchEntry[];
  /** Coincidencias totales del grupo, aunque solo se listen las primeras. */
  total: number;
}

export interface SearchData {
  pets?: readonly Pet[];
  owners?: readonly Owner[];
  drugs?: readonly Drug[];
  veterinarians?: readonly Veterinarian[];
}

// Tipos que busca cada rol, en el orden en que se agrupan los resultados.
const KINDS_BY_ROLE: Record<AuthRole, readonly SearchKind[]> = {
  VETERINARIAN: ['pet', 'owner', 'drug'],
  ADMIN: ['veterinarian', 'pet', 'drug'],
  OWNER: ['pet'],
};

export function searchKindsFor(role: AuthRole): readonly SearchKind[] {
  return KINDS_BY_ROLE[role];
}

const SPECIES_WORD: Record<Species, string> = { [Species.DOG]: 'perro', [Species.CAT]: 'gato' };

function joinText(parts: readonly (string | number | null | undefined)[]): string {
  return parts.filter((part) => part !== null && part !== undefined && part !== '').join(' ');
}

// El administrador no tiene ficha de veterinario ni de mascota: sus resultados llevan a la lista.
function petRoute(role: AuthRole, pet: Pet, ownerId: number): string {
  switch (role) {
    case 'ADMIN':
      return '/administrador/mascotas';
    case 'OWNER':
      return `/cliente/${ownerId}/mascotas/${pet.id}`;
    default:
      return `/veterinario/mascotas/${pet.id}`;
  }
}

/** Convierte los datos que el rol ya puede pedir al API en entradas de búsqueda, con la ruta de su ficha. */
export function buildSearchEntries(role: AuthRole, data: SearchData, ownerId: number): SearchEntry[] {
  const entries: SearchEntry[] = [];
  for (const kind of KINDS_BY_ROLE[role]) {
    switch (kind) {
      case 'pet':
        for (const pet of data.pets ?? []) {
          entries.push({
            kind,
            id: pet.id,
            label: pet.name,
            detail: role === 'OWNER' ? pet.breed : joinText([pet.breed, '·', pet.ownerName]),
            route: petRoute(role, pet, ownerId),
            keywords: joinText([pet.breed, pet.ownerName, SPECIES_WORD[pet.species]]),
            species: pet.species,
            photoUrl: pet.photoUrl,
          });
        }
        break;
      case 'owner':
        for (const owner of data.owners ?? []) {
          entries.push({
            kind,
            id: owner.id,
            label: owner.name,
            detail: joinText([owner.document, '·', owner.email]),
            route: `/veterinario/duenos/${owner.id}`,
            keywords: joinText([owner.document, owner.email, owner.phone]),
          });
        }
        break;
      case 'drug':
        for (const drug of data.drugs ?? []) {
          entries.push({
            kind,
            id: drug.id,
            label: drug.name,
            detail: `${drug.unitsAvailable} ${drug.unitsAvailable === 1 ? 'unidad' : 'unidades'} en existencias`,
            route: `${role === 'ADMIN' ? '/administrador' : '/veterinario'}/medicamentos/${drug.id}`,
            keywords: '',
          });
        }
        break;
      case 'veterinarian':
        for (const vet of data.veterinarians ?? []) {
          entries.push({
            kind,
            id: vet.id,
            label: vet.name,
            detail: vet.specialty,
            route: '/administrador/veterinarios',
            keywords: joinText([vet.specialty, vet.email, vet.document]),
            photoUrl: vet.photoUrl,
          });
        }
        break;
    }
  }
  return entries;
}

// 0: el nombre empieza por la consulta; 1: alguna palabra del nombre; 2: solo coincide en otros datos.
function rank(label: string, query: string): number {
  if (label.startsWith(query)) {
    return 0;
  }
  return label.split(/\s+/).some((word) => word.startsWith(query)) ? 1 : 2;
}

/**
 * Busca sin distinguir mayúsculas ni tildes. Cada palabra de la consulta debe aparecer en el nombre
 * o en los datos extra del recurso. Agrupa por tipo, en el orden de `kinds`, y lista como mucho
 * `limit` resultados por grupo: primero los que empiezan por la consulta, luego los demás en su orden.
 */
export function searchGroups(
  entries: readonly SearchEntry[],
  query: string,
  kinds: readonly SearchKind[],
  limit = SEARCH_GROUP_LIMIT,
): SearchGroup[] {
  const text = normalizeText(query).trim();
  if (!text) {
    return [];
  }
  const words = text.split(/\s+/);

  const groups: SearchGroup[] = [];
  for (const kind of kinds) {
    const matches = entries
      .filter((entry) => entry.kind === kind)
      .map((entry, index) => ({ entry, index, label: normalizeText(entry.label) }))
      .filter(({ entry, label }) => {
        const haystack = `${label} ${normalizeText(entry.keywords)}`;
        return words.every((word) => haystack.includes(word));
      })
      .map((match) => ({ ...match, rank: rank(match.label, text) }))
      .sort((a, b) => a.rank - b.rank || a.index - b.index);
    if (matches.length) {
      groups.push({
        kind,
        label: SEARCH_GROUP_LABELS[kind],
        entries: matches.slice(0, limit).map(({ entry }) => entry),
        total: matches.length,
      });
    }
  }
  return groups;
}

/** Resultados en el orden de pantalla, para moverse con el teclado. */
export function flattenSearchGroups(groups: readonly SearchGroup[]): SearchEntry[] {
  return groups.flatMap((group) => group.entries);
}
