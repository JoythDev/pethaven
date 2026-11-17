import { SortAccessors } from '../list/list-sort';
import { petStatus } from '../status/pet-status';
import { normalizeText } from '../text/normalize-text';
import { Pet, Species } from '../../models/pet.model';
import { Treatment } from '../../models/treatment.model';

/** Claves de filtro de la lista de mascotas en la URL. */
export const PET_FILTER_KEYS = ['especie', 'estado', 'mios'] as const;

/** Valores del filtro `estado`. */
export const PET_STATUS_FILTERS = { active: 'activa', inactive: 'inactiva' } as const;

export const speciesLabel = (species: Species): string => (species === Species.DOG ? 'Perro' : 'Gato');

export const PET_SORT_ACCESSORS: SortAccessors<Pet> = {
  name: (p) => p.name,
  species: (p) => speciesLabel(p.species),
  owner: (p) => p.ownerName,
  age: (p) => p.age,
  weight: (p) => p.weight,
  status: (p) => petStatus(p).label,
};

export interface PetCriteria {
  search: string;
  /** `DOG` o `CAT`; vacío no filtra. */
  species: string;
  /** `activa` o `inactiva`; vacío no filtra. */
  status: string;
  /** Ids de los pacientes del veterinario; null no filtra. */
  patientIds: ReadonlySet<number> | null;
}

/** Búsqueda por nombre, raza o dueño (sin tildes ni mayúsculas) más especie, estado y «mis pacientes». */
export function petMatches(pet: Pet, criteria: PetCriteria): boolean {
  if (criteria.patientIds && !criteria.patientIds.has(pet.id)) {
    return false;
  }
  if (criteria.species && pet.species !== criteria.species) {
    return false;
  }
  if (criteria.status && pet.active !== (criteria.status === PET_STATUS_FILTERS.active)) {
    return false;
  }
  const term = normalizeText(criteria.search);
  return !term || normalizeText(`${pet.name} ${pet.breed} ${pet.ownerName}`).includes(term);
}

export const patientsCountLabel = (total: number): string => `${total} ${total === 1 ? 'paciente' : 'pacientes'}`;

export function ageLabel(age: number): string {
  if (age < 1) {
    return 'Menos de 1 año';
  }
  return `${age} ${age === 1 ? 'año' : 'años'}`;
}

/** Tratamiento más reciente; con la misma fecha gana el de id mayor. */
export function lastTreatment(treatments: readonly Treatment[]): Treatment | null {
  let last: Treatment | null = null;
  for (const t of treatments) {
    if (!last || t.date > last.date || (t.date === last.date && t.id > last.id)) {
      last = t;
    }
  }
  return last;
}

/** Sustituye la mascota por id y conserva el orden y las demás referencias. */
export const replacePet = (pets: readonly Pet[], updated: Pet): Pet[] =>
  pets.map((p) => (p.id === updated.id ? updated : p));

/** Aviso de éxito al activar o desactivar una mascota. */
export function petActiveToast(pet: Pick<Pet, 'name' | 'active'>): { summary: string; detail: string } {
  return {
    summary: pet.active ? 'Mascota activada' : 'Mascota desactivada',
    detail: `${pet.name} ahora está ${pet.active ? 'activa' : 'inactiva'}.`,
  };
}
