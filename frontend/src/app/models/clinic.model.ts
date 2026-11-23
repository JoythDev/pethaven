// Datos públicos de la sede (ClinicResponse). Las horas llegan como 'HH:mm:ss'; un día sin atención trae null.
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface ClinicHours {
  day: DayOfWeek;
  open: string | null;
  close: string | null;
}

export interface Clinic {
  name: string;
  address: string;
  phone: string;
  timeZone: string;
  slotMinutes: number;
  hours: ClinicHours[];
}
