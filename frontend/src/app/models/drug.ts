import type { TreatmentDrug } from './treatment-drug';

export interface Drug {
  id: number;
  name: string;
  purchasePrice: number;
  salePrice: number;
  unitsAvailable: number;
  unitsSold: number;
  treatmentDrugs: TreatmentDrug[];
}
