import { Treatment } from '../../models/treatment.model';

export const RECENT_PATIENTS_MAX = 4;

export interface RecentPatient {
  petId: number;
  petName: string;
  /** Fecha (AAAA-MM-DD) de su último tratamiento con este veterinario. */
  lastDate: string;
}

/** Actividad del veterinario, derivada de sus tratamientos: el API no la entrega ya calculada. */
export interface VetActivity {
  /** Mascotas distintas que ha atendido. */
  patients: number;
  treatmentsThisMonth: number;
  recentPatients: RecentPatient[];
}

const monthPrefix = (date: Date): string => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;

export function deriveVetActivity(
  treatments: readonly Treatment[],
  now: Date,
  recentLimit: number = RECENT_PATIENTS_MAX,
): VetActivity {
  const prefix = monthPrefix(now);
  // Último tratamiento de cada mascota; con la misma fecha gana el de id mayor.
  const last = new Map<number, Treatment>();
  let treatmentsThisMonth = 0;
  for (const treatment of treatments) {
    if (treatment.date.startsWith(prefix)) {
      treatmentsThisMonth++;
    }
    const seen = last.get(treatment.petId);
    if (!seen || treatment.date > seen.date || (treatment.date === seen.date && treatment.id > seen.id)) {
      last.set(treatment.petId, treatment);
    }
  }
  const recentPatients = [...last.values()]
    .sort((a, b) => (a.date === b.date ? b.id - a.id : a.date < b.date ? 1 : -1))
    .slice(0, recentLimit)
    .map((t): RecentPatient => ({ petId: t.petId, petName: t.petName, lastDate: t.date }));
  return { patients: last.size, treatmentsThisMonth, recentPatients };
}
