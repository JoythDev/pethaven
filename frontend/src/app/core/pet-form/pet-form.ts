import { Species } from '../../models/pet.model';
import { ageLabel, formatKg } from '../pet-detail/pet-detail';

/** Límite de la URL de la foto que acepta el API. */
export const PHOTO_URL_MAX = 500;
/** Espera antes de intentar cargar la foto, para no parpadear en cada tecla. */
export const PHOTO_DEBOUNCE_MS = 400;

const LIST_URL = '/veterinario/mascotas';

export type PetFormField = 'name' | 'species' | 'breed' | 'age' | 'weight' | 'photoUrl' | 'ownerId' | 'active';

export interface PetFormLimits {
  weightMin: number;
  weightMax: number;
  ageMax: number;
}

export const normalizePhotoUrl = (value: string | null | undefined): string => (value ?? '').trim();

/** URL que la vista previa intenta cargar: solo http(s) válida y dentro del límite; si no, null. */
export function previewPhotoUrl(value: string | null | undefined): string | null {
  const url = normalizePhotoUrl(value);
  if (!url || url.length > PHOTO_URL_MAX || /\s/.test(url)) {
    return null;
  }
  try {
    const parsed = new URL(url);
    return (parsed.protocol === 'http:' || parsed.protocol === 'https:') && parsed.hostname ? url : null;
  } catch {
    return null;
  }
}

export const previewName = (value: string): string => value.trim() || 'Sin nombre todavía';

export interface PreviewData {
  species: Species;
  breed: string;
  age: number | null;
  weight: number | null;
}

/** «Perro · Beagle · 4 años · 11,2 kg»; omite lo que aún no se ha escrito. */
export function previewLine({ species, breed, age, weight }: PreviewData): string {
  const parts = [species === Species.CAT ? 'Gato' : 'Perro'];
  if (breed.trim()) {
    parts.push(breed.trim());
  }
  if (age !== null) {
    parts.push(ageLabel(age));
  }
  if (weight !== null) {
    parts.push(formatKg(weight));
  }
  return parts.join(' · ');
}

type ErrorMap = Record<string, unknown> | null | undefined;

/** Mensaje de error de un campo: qué pasa y cómo corregirlo (11.4). Null si no hay error. */
export function fieldError(field: PetFormField, errors: ErrorMap, limits: PetFormLimits): string | null {
  if (!errors || Object.keys(errors).length === 0) {
    return null;
  }
  if (typeof errors['server'] === 'string') {
    return errors['server'];
  }
  const has = (key: string): boolean => key in errors;
  const weightRange = `El peso debe estar entre ${String(limits.weightMin).replace('.', ',')} y ${limits.weightMax} kg.`;
  const ageRange = `La edad debe estar entre 0 y ${limits.ageMax} años.`;

  switch (field) {
    case 'name':
      if (has('maxlength')) return 'Usa 60 caracteres o menos en el nombre.';
      return 'Escribe el nombre de la mascota.';
    case 'species':
      return 'Elige la especie.';
    case 'breed':
      if (has('maxlength')) return 'Usa 60 caracteres o menos en la raza.';
      if (has('required') || has('pattern')) return 'Escribe la raza, por ejemplo Beagle.';
      break;
    case 'age':
      if (has('required')) return 'Escribe la edad en años, por ejemplo 4.';
      if (has('min') || has('max')) return ageRange;
      if (has('pattern')) return 'Escribe la edad con números enteros, por ejemplo 4.';
      break;
    case 'weight':
      if (has('required')) return 'Escribe el peso con números, por ejemplo 11,2.';
      if (has('min') || has('max')) return weightRange;
      break;
    case 'ownerId':
      return 'Elige el dueño de la mascota.';
    case 'photoUrl':
      if (has('required')) return 'Pega el enlace de la foto, por ejemplo https://ejemplo.com/luna.jpg.';
      if (has('maxlength')) return `Usa un enlace de ${PHOTO_URL_MAX} caracteres o menos.`;
      if (has('pattern')) {
        return 'El enlace debe empezar con http:// o https://, por ejemplo https://ejemplo.com/luna.jpg.';
      }
      break;
    case 'active':
      break;
  }
  return 'Revisa este campo.';
}

export interface BackTarget {
  url: string;
  label: string;
}

const pathOf = (url: string): string => url.split(/[?#]/)[0];

/**
 * A dónde vuelve el enlace «atrás» y «Cancelar»: a la lista (con sus filtros) o a la ficha de esta
 * mascota (con su pestaña) si de ahí vino la persona; si no, a la ficha al editar y a la lista al crear.
 * Al crear con un dueño preelegido (`?dueno=`) vuelve a la ficha de ese dueño.
 */
export function backTarget(previousUrl: string | null, editId: number | null, ownerId: number | null = null): BackTarget {
  if (editId === null && ownerId !== null) {
    return { url: `/veterinario/duenos/${ownerId}`, label: 'Volver al dueño' };
  }
  const list: BackTarget = { url: LIST_URL, label: 'Mascotas' };
  const detail: BackTarget | null =
    editId === null ? null : { url: `${LIST_URL}/${editId}`, label: 'Volver a la ficha' };

  if (previousUrl) {
    const path = pathOf(previousUrl);
    if (path === LIST_URL) {
      return { url: previousUrl, label: list.label };
    }
    if (detail && path === detail.url) {
      return { url: previousUrl, label: detail.label };
    }
  }
  return detail ?? list;
}
