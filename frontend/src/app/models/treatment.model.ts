// Tratamiento con los medicamentos administrados que devuelve el API.
export interface Treatment {
  id: number;
  date: string;
  petId: number;
  petName: string;
  veterinarianId: number;
  veterinarianName: string;
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
  drugs: TreatmentDrugRequest[];
}

// Cuerpo para administrar un medicamento en un tratamiento (TreatmentDrugRequest).
export interface TreatmentDrugRequest {
  drugId: number;
  units: number;
}
