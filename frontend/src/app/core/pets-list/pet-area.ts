import { Data } from '@angular/router';

import type { DetailTab } from '../pet-detail/pet-detail';

// Configuración de las pantallas de mascotas según el área que las monta (veterinario, administrador o cliente).
// Llega por el `data` de la ruta, igual que la de medicamentos, para que un mismo componente sirva a todas.
export interface PetAreaConfig {
  /** Ruta base del listado en el área (sin barra final); puede llevar el marcador `:ownerId` (ver `ownerScoped`). */
  basePath: string;
  /** Si es true el área vive bajo `/cliente/:ownerId`: la ruta base lleva ese id y solo se ven mascotas de ese dueño. */
  ownerScoped: boolean;
  /** Si es true se ofrecen crear, editar, activar y registrar tratamientos (solo el veterinario). */
  canManage: boolean;
  /** Si es true se ofrece agendar una cita (solo el cliente). */
  canSchedule: boolean;
  /** Si es true la ficha muestra precios de venta, subtotales y total de la receta; el cliente nunca los ve (11.10). */
  showPrices: boolean;
  /** Si es true la historia clínica muestra el costo de compra y la ganancia (solo el administrador, 11.10). */
  showCost: boolean;
  /** Si es true la ficha nombra al dueño de la mascota; el cliente es el dueño y no se le repite. */
  showOwner: boolean;
  /** Ruta base de las fichas de dueño; null si el área no tiene esa pantalla (el nombre queda como texto). */
  ownersPath: string | null;
  /** Nombre del listado en el enlace de regreso. */
  listLabel: string;
  /** Pestaña de tratamientos: valor en `?pestana`, etiqueta de la pestaña y del botón del resumen. */
  history: { param: string; label: string; action: string };
}

export const PET_AREA_DATA_KEY = 'petArea';

export const VET_PET_AREA: PetAreaConfig = {
  basePath: '/veterinario/mascotas',
  ownerScoped: false,
  canManage: true,
  canSchedule: false,
  showPrices: true,
  showCost: false,
  showOwner: true,
  ownersPath: '/veterinario/duenos',
  listLabel: 'Mascotas',
  history: { param: 'historia', label: 'Historia clínica', action: 'Ver historia' },
};

export const ADMIN_PET_AREA: PetAreaConfig = {
  basePath: '/administrador/mascotas',
  ownerScoped: false,
  canManage: false,
  canSchedule: false,
  showPrices: true,
  showCost: true,
  showOwner: true,
  ownersPath: null,
  listLabel: 'Mascotas',
  history: { param: 'historia', label: 'Historia clínica', action: 'Ver historia' },
};

// S21: el cliente consulta y agenda, pero no edita ni ve precios.
export const CLIENT_PET_AREA: PetAreaConfig = {
  basePath: '/cliente/:ownerId/mascotas',
  ownerScoped: true,
  canManage: false,
  canSchedule: true,
  showPrices: false,
  showCost: false,
  showOwner: false,
  ownersPath: null,
  listLabel: 'Mis mascotas',
  history: { param: 'tratamientos', label: 'Tratamientos', action: 'Ver tratamientos' },
};

/** Configuración que viene en los datos de la ruta; sin ella se asume el área del veterinario. */
export const resolvePetArea = (data: Data | undefined): PetAreaConfig =>
  (data?.[PET_AREA_DATA_KEY] as PetAreaConfig | undefined) ?? VET_PET_AREA;

/** Ruta base del listado: el cliente sustituye `:ownerId` por el de su ruta (`x` si es inválido); las demás áreas son estáticas. */
export const resolvePetBasePath = (area: PetAreaConfig, ownerId: number | null): string =>
  area.basePath.replace(':ownerId', ownerId === null ? 'x' : String(ownerId));

/** Pestaña interna que pide la URL: el valor propio de la pestaña de tratamientos del área equivale a `historia`. */
export const historyTabFromParam = (area: PetAreaConfig, value: string | null): string | null =>
  value !== null && value === area.history.param ? 'historia' : value;

/** Valor de `?pestana` para una pestaña: la de tratamientos usa el valor propio del área; `resumen` no se escribe. */
export const historyTabToParam = (area: PetAreaConfig, tab: DetailTab): string | null =>
  tab === 'resumen' ? null : tab === 'historia' ? area.history.param : tab;
