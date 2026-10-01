import type { Pet } from './pet';
import type { TreatmentDrug } from './treatment-drug';
import type { Veterinarian } from './veterinarian';

export interface Treatment {
  id: number;
  pet: Pet;
  veterinarian: Veterinarian;
  date: string;
  treatmentDrugs: TreatmentDrug[];
}
