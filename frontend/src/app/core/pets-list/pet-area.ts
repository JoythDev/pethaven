import { Data } from '@angular/router';

// Configuración de las pantallas de mascotas según el área que las monta (veterinario o administrador).
// Llega por el `data` de la ruta, igual que la de medicamentos, para que un mismo componente sirva a ambos.
export interface PetAreaConfig {
  /** Ruta base del listado en el área (sin barra final). */
  basePath: string;
  /** Si es true se ofrecen crear, editar, activar y registrar tratamientos (solo el veterinario). */
  canManage: boolean;
  /** Si es true la historia clínica muestra el costo de compra y la ganancia (solo el administrador, 11.10). */
  showCost: boolean;
  /** Ruta base de las fichas de dueño; null si el área no tiene esa pantalla (el nombre queda como texto). */
  ownersPath: string | null;
}

export const PET_AREA_DATA_KEY = 'petArea';

export const VET_PET_AREA: PetAreaConfig = {
  basePath: '/veterinario/mascotas',
  canManage: true,
  showCost: false,
  ownersPath: '/veterinario/duenos',
};

export const ADMIN_PET_AREA: PetAreaConfig = {
  basePath: '/administrador/mascotas',
  canManage: false,
  showCost: true,
  ownersPath: null,
};

/** Configuración que viene en los datos de la ruta; sin ella se asume el área del veterinario. */
export const resolvePetArea = (data: Data | undefined): PetAreaConfig =>
  (data?.[PET_AREA_DATA_KEY] as PetAreaConfig | undefined) ?? VET_PET_AREA;
