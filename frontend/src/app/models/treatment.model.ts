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
