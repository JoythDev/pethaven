import { formatDate } from '@angular/common';
import { Appointment, DayAgenda, DayCount } from '../../models/appointment.model';
import { Pet } from '../../models/pet.model';
import { Treatment } from '../../models/treatment.model';
import { formatTime } from '../time/calendar';

// Lógica pura del Inicio del veterinario.

// «Lunes 16 de noviembre de 2026» (12.2, localización es-CO de Angular).
export function longDateEs(date: Date): string {
  const text = formatDate(date, "EEEE d 'de' MMMM 'de' y", 'es-CO');
  return text.charAt(0).toUpperCase() + text.slice(1);
}

// Primer nombre sin el tratamiento «Dr.» o «Dra.»; null si no hay nombre.
export function firstName(name?: string | null): string | null {
  const word = name?.replace(/^\s*Dra?\.\s*/i, '').trim().split(/\s+/)[0];
  return word || null;
}

// Citas de hoy por atender: en curso o programadas que aún no terminan, en orden, hasta el límite.
export function remainingAppointments(agenda: DayAgenda, now: Date, limit: number): Appointment[] {
  return agenda.appointments
    .filter((a) => a.status === 'IN_PROGRESS' || (a.status === 'SCHEDULED' && a.end.getTime() > now.getTime()))
    .slice(0, limit);
}

export interface HeroCopy {
  headline: string;
  description: string;
}

// Texto del héroe cuando la agenda no pudo cargar: sigue siendo útil sin las citas.
export const HERO_FALLBACK: HeroCopy = {
  headline: 'Revisa cómo va tu día',
  description: 'No pudimos cargar tus citas, pero puedes seguir con tus pacientes.',
};

export function heroCopy(agenda: DayAgenda): HeroCopy {
  if (agenda.closed) {
    return {
      headline: 'Hoy la clínica no atiende',
      description: 'No hay citas hoy. Puedes consultar a tus pacientes cuando lo necesites.',
    };
  }
  const first = agenda.appointments[0];
  if (!first) {
    return {
      headline: 'Hoy no tienes citas agendadas',
      description: 'Es un buen momento para revisar a tus pacientes o registrar un tratamiento pendiente.',
    };
  }
  const time = formatTime(first.start);
  const description = 'Revisa la agenda del día y registra los tratamientos al terminar cada consulta.';
  return agenda.appointments.length === 1
    ? { headline: `Tienes 1 cita hoy. Es a las ${time}`, description }
    : { headline: `Tienes ${agenda.appointments.length} citas hoy. La primera es a las ${time}`, description };
}

export interface RecentPatient {
  pet: Pet;
  // Fecha del tratamiento más reciente de la mascota.
  lastDate: string;
}

// Mascotas distintas de los tratamientos del veterinario, de la atención más reciente a la más antigua.
// Se omiten las que ya no están en la lista de mascotas.
export function recentPatients(treatments: Treatment[], pets: Pet[], limit: number): RecentPatient[] {
  const byId = new Map(pets.map((pet) => [pet.id, pet]));
  const ordered = [...treatments].sort(
    (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime() || b.id - a.id,
  );
  const seen = new Set<number>();
  const result: RecentPatient[] = [];
  for (const treatment of ordered) {
    const pet = byId.get(treatment.petId);
    if (!pet || seen.has(pet.id)) {
      continue;
    }
    seen.add(pet.id);
    result.push({ pet, lastDate: treatment.date });
    if (result.length === limit) {
      break;
    }
  }
  return result;
}

export interface WeekSummary {
  total: number;
  attended: number;
  percent: number;
}

// Citas de la semana (sin las canceladas) y cuántas ya se completaron.
export function weekSummary(days: DayCount[]): WeekSummary {
  const total = days.reduce((sum, day) => sum + day.count, 0);
  const attended = days.reduce((sum, day) => sum + day.completed, 0);
  return { total, attended, percent: total ? Math.round((attended / total) * 100) : 0 };
}

export interface VetStats {
  /** Mascotas distintas atendidas por el veterinario. */
  patientCount: number;
  /** Atenciones (tratamientos) registradas por el veterinario. */
  treatmentCount: number;
  /** Mascotas atendidas que hoy siguen activas. */
  activePatientCount: number;
  /** Tratamiento más reciente: fecha y hora ya redactadas; null si no hay atenciones. */
  latestAttention: { date: string; time: string } | null;
}

const attentionDate = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' });
const attentionTime = new Intl.DateTimeFormat('es-CO', { timeStyle: 'short' });

// Indicadores reales del Inicio (los de la versión anterior): se calculan con los tratamientos del
// veterinario y la lista completa de mascotas, activas o no.
export function vetStats(treatments: Treatment[], pets: Pet[]): VetStats {
  const patientIds = new Set(treatments.map((treatment) => treatment.petId));
  const latest = treatments.reduce<Treatment | null>(
    (best, treatment) =>
      !best || new Date(treatment.date).getTime() > new Date(best.date).getTime() ? treatment : best,
    null,
  );
  return {
    patientCount: patientIds.size,
    treatmentCount: treatments.length,
    activePatientCount: pets.filter((pet) => pet.active && patientIds.has(pet.id)).length,
    latestAttention: latest
      ? { date: attentionDate.format(new Date(latest.date)), time: attentionTime.format(new Date(latest.date)) }
      : null,
  };
}
