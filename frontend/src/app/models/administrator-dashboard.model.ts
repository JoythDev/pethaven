export interface AdministratorDashboardResponse {
  monthlyPeriodStart: string;
  monthlyPeriodEndExclusive: string;
  monthlyPeriodTimeZone: string;
  administeredTreatmentsLastMonth: number;
  treatmentsByMedicationLastMonth: MedicationTreatmentCount[];
  activeVeterinarians: number;
  inactiveVeterinarians: number;
  totalPets: number;
  activePets: number;
  totalSales: number;
  grossProfit: number;
  topThreeMedicationsByUnitsSold: TopMedicationByUnitsSold[];
}

export interface MedicationTreatmentCount {
  medicationId: number | null;
  medicationName: string;
  treatmentCount: number;
}

export interface TopMedicationByUnitsSold {
  medicationId: number | null;
  medicationName: string;
  unitsSold: number;
}
