// Carné de vacunas (demo, S3): funcionalidad pendiente de backend.
export type VaccineStatus = 'ok' | 'soon' | 'overdue';

export interface Vaccine {
  name: string;
  appliedOn: Date;
  nextDue: Date;
  // Días hasta el vencimiento; negativo si ya venció.
  daysUntilDue: number;
  status: VaccineStatus;
  label: string;
}

export interface VaccineSummary {
  total: number;
  upToDate: number;
  soon: number;
  overdue: number;
}

export interface VaccineCard {
  petId: number;
  vaccines: Vaccine[];
  summary: VaccineSummary;
}
