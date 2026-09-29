import type { Drug } from './drug';
import type { Treatment } from './treatment';

export interface TreatmentDrug {
  id: number;
  treatment: Treatment;
  drug: Drug;
  units: number;
  unitPurchasePrice: number;
  unitSalePrice: number;
}
