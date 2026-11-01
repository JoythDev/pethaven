// Evolución en una consulta de seguimiento (Evolution del backend). RECOVERED solo aparece
// en el registro del alta (sin medicamentos), nunca en un tratamiento.
// En el ingreso (la mascota llega estable) no hay evolución: viene en null.
export type Evolution = 'STABLE' | 'IMPROVING' | 'WORSENING' | 'RECOVERED';

export const EVOLUTION_LABELS: Record<Evolution, string> = {
  STABLE: 'Sin cambios',
  IMPROVING: 'Mejorando',
  WORSENING: 'Empeorando',
  RECOVERED: 'Alta (estable)',
};

export const evolutionLabel = (evolution: Evolution | null): string =>
  evolution ? EVOLUTION_LABELS[evolution] : 'Ingreso';

// Tratamiento con los medicamentos administrados que devuelve el API.
export interface Treatment {
  id: number;
  date: string;
  petId: number;
  petName: string;
  veterinarianId: number;
  veterinarianName: string;
  weight: number;
  diagnosis: string;
  evolution: Evolution | null;
  notes: string | null;
  drugs: TreatmentDrug[];
}

// Medicamento suministrado dentro de un tratamiento.
export interface TreatmentDrug {
  id: number;
  drugId: number;
  drugName: string;
  units: number;
  unitPurchasePrice: number;
  unitSalePrice: number;
}

// Cuerpo para registrar un tratamiento (TreatmentRequest): con date en null
// el API usa la fecha actual, y la fecha no puede ser futura. Los medicamentos
// viajan junto al tratamiento y el API los registra todos o ninguno.
export interface TreatmentRequest {
  petId: number;
  veterinarianId: number;
  date: string | null;
  weight: number;
  diagnosis: string;
  evolution: Evolution | null;
  notes: string | null;
  drugs: TreatmentDrugRequest[];
}

// Cuerpo para administrar un medicamento en un tratamiento (TreatmentDrugRequest).
export interface TreatmentDrugRequest {
  drugId: number;
  units: number;
}
