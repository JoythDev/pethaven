import { ActivatedRoute } from '@angular/router';

// Configuración de las pantallas compartidas de medicamentos según el área que las monta.
// Llega por el "data" de la ruta para que el mismo componente sirva al admin y al veterinario.
export interface DrugAreaConfig {
  /** Ruta base del listado en el área (sin barra final). */
  basePath: string;
  /** Si es true se muestran crear, editar, eliminar e importar (solo el administrador). */
  canManage: boolean;
}

export const DRUG_AREA_DATA_KEY = 'drugArea';

export const ADMIN_DRUG_AREA: DrugAreaConfig = { basePath: '/administrador/medicamentos', canManage: true };

export const VET_DRUG_AREA: DrugAreaConfig = { basePath: '/veterinario/drugs', canManage: false };

// Lee la configuración de la ruta. Sin configuración, se asume solo lectura en el área del veterinario.
export const readDrugArea = (route: ActivatedRoute): DrugAreaConfig =>
  (route.snapshot.data[DRUG_AREA_DATA_KEY] as DrugAreaConfig | undefined) ?? VET_DRUG_AREA;
