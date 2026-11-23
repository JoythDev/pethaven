import { formatDate } from '@angular/common';
import { APPOINTMENT_REASON_LABELS, Appointment } from '../../models/appointment.model';
import { Clinic } from '../../models/clinic.model';
import { CareItem } from '../../models/demo/care-plan.model';
import { Vaccine, VaccineCard, VaccineSummary } from '../../models/demo/vaccine.model';
import { Pet } from '../../models/pet.model';
import { ClienteTreatment } from '../../models/treatment-cliente.model';
import { Evolution, evolutionLabel } from '../../models/treatment.model';
import { dayNumber, formatTime } from '../time/calendar';
import { evolutionSeverity } from '../pet-detail/pet-detail';
import { TagSeverity } from '../status/pet-status';

// Lógica pura del Inicio del cliente (11.6, 11.15): mascota destacada, textos del héroe, próxima
// cita, tratamientos recientes y resumen del carné de vacunas.

export interface PetPlan {
  pet: Pet;
  plan: CareItem[];
}

// Mascota destacada del héroe y del plan: la activa con más cuidados pendientes hoy; si hay empate
// o ninguna tiene cuidados, la primera activa; y si todas están inactivas, la primera de la lista.
export function pickFeaturedPet(entries: PetPlan[], doneIds: ReadonlySet<string>): Pet | null {
  let best: { pet: Pet; pending: number } | null = null;
  for (const { pet, plan } of entries) {
    if (!pet.active) {
      continue;
    }
    const pending = plan.filter((item) => !doneIds.has(item.id)).length;
    if (!best || pending > best.pending) {
      best = { pet, pending };
    }
  }
  return best?.pet ?? entries[0]?.pet ?? null;
}

export interface HeroCopy {
  headline: string;
  description: string;
}

const HERO_CARE_DESCRIPTION = 'Marca cada cuidado cuando lo completes. Tu clínica verá el avance en la próxima cita.';

// Texto del héroe cuando los tratamientos no pudieron cargar: sigue siendo útil sin el plan.
export const HERO_FALLBACK: HeroCopy = {
  headline: 'Revisa cómo van tus mascotas',
  description: 'No pudimos cargar su plan de hoy, pero puedes seguir con sus fichas y tus citas.',
};

// Texto del héroe de un dueño sin mascotas registradas.
export const HERO_NO_PETS: HeroCopy = {
  headline: 'Aún no tienes mascotas registradas',
  description: 'Tu veterinario las registra en la primera visita. Mientras tanto, puedes agendar tu cita.',
};

export function heroCopy(petName: string, pending: number, total: number): HeroCopy {
  if (total === 0) {
    return {
      headline: `Hoy ${petName} no tiene cuidados por marcar`,
      description: 'Cuando haya un plan de cuidado para hoy, lo verás aquí.',
    };
  }
  if (pending === 0) {
    return { headline: `Todo al día con ${petName}. Buen trabajo`, description: HERO_CARE_DESCRIPTION };
  }
  const noun = pending === 1 ? 'cuidado pendiente' : 'cuidados pendientes';
  return { headline: `${petName} tiene ${pending} ${noun} hoy`, description: HERO_CARE_DESCRIPTION };
}

const capitalize = (text: string): string => text.charAt(0).toUpperCase() + text.slice(1);

// «Martes 17 · 10:30» (12.2). A más de una semana, antes o después, se añade el mes para que no haya dudas.
export function appointmentWhen(start: Date, today: Date): string {
  const far = Math.abs(dayNumber(start) - dayNumber(today)) > 6;
  const day = capitalize(formatDate(start, far ? 'EEEE d MMM' : 'EEEE d', 'es-CO'));
  return `${day} · ${formatTime(start)}`;
}

// Etiqueta de cercanía de la cita: En curso, Hoy, Mañana o En N días.
export function whenTag(appointment: Pick<Appointment, 'start' | 'status'>, today: Date): string {
  if (appointment.status === 'IN_PROGRESS') {
    return 'En curso';
  }
  const days = dayNumber(appointment.start) - dayNumber(today);
  if (days <= 0) {
    return 'Hoy';
  }
  return days === 1 ? 'Mañana' : `En ${days} días`;
}

export interface RecentTreatment {
  id: number;
  // Día corto, por ejemplo «10 nov».
  day: string;
  diagnosis: string;
  veterinarian: string;
  evolution: string;
  severity: TagSeverity;
}

// Una fecha del API es ISO; se leen sus componentes de día para no desplazarla por la zona horaria.
function localDay(value: string): Date {
  const [year, month, day] = value.slice(0, 10).split('-').map(Number);
  return new Date(year, month - 1, day);
}

// Los tratamientos más recientes primero; con la misma fecha, el de id mayor.
export function recentTreatments(treatments: ClienteTreatment[], limit: number): RecentTreatment[] {
  return [...treatments]
    .sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id)
    .slice(0, limit)
    .map((treatment) => {
      const evolution = treatment.evolution as Evolution | null;
      return {
        id: treatment.id,
        day: formatDate(localDay(treatment.date), 'd MMM', 'es-CO'),
        diagnosis: treatment.diagnosis,
        veterinarian: treatment.veterinarianName,
        evolution: evolutionLabel(evolution),
        severity: evolutionSeverity(evolution),
      };
    });
}

// Las vacunas más urgentes primero (vencidas, luego las que vencen antes), hasta el límite.
export function vaccineHighlights(card: VaccineCard, limit: number): Vaccine[] {
  return [...card.vaccines].sort((a, b) => a.daysUntilDue - b.daysUntilDue).slice(0, limit);
}

// «4 vigentes de 5. 1 vence pronto.» con el vocabulario de 12.3.
export function vaccineSummaryText(summary: VaccineSummary): string {
  if (summary.total === 0) {
    return 'Aún no hay vacunas registradas.';
  }
  const head = `${summary.upToDate} ${summary.upToDate === 1 ? 'vigente' : 'vigentes'} de ${summary.total}.`;
  const parts: string[] = [];
  if (summary.soon > 0) {
    parts.push(`${summary.soon} ${summary.soon === 1 ? 'vence' : 'vencen'} pronto`);
  }
  if (summary.overdue > 0) {
    parts.push(`${summary.overdue} ${summary.overdue === 1 ? 'vencida' : 'vencidas'}`);
  }
  return parts.length ? `${head} ${parts.join(' y ')}.` : `${head} Todo al día.`;
}

export interface Reminder {
  // «Hoy», «Mar 17» o «Vencida».
  when: string;
  text: string;
}

// «Mar 17»: día corto sin punto, para la lista de recordatorios.
function shortDay(date: Date): string {
  return capitalize(formatDate(date, 'EEE d', 'es-CO').replace('.', ''));
}

interface ReminderSources {
  petName: string | null;
  // Cuidados de hoy sin marcar.
  pending: number;
  next: Pick<Appointment, 'start' | 'reason' | 'petName'> | null;
  // La vacuna más urgente de la mascota destacada.
  vaccine: Vaccine | null;
}

// Recordatorios de la barra lateral: cuidados de hoy, próxima cita y la vacuna que pide atención.
export function buildReminders({ petName, pending, next, vaccine }: ReminderSources): Reminder[] {
  const reminders: Reminder[] = [];
  if (petName && pending > 0) {
    reminders.push({ when: 'Hoy', text: `${pending} ${pending === 1 ? 'cuidado' : 'cuidados'} de ${petName}` });
  }
  if (next) {
    reminders.push({ when: shortDay(next.start), text: `${APPOINTMENT_REASON_LABELS[next.reason]} de ${next.petName}` });
  }
  if (petName && vaccine && vaccine.status !== 'ok') {
    reminders.push({
      when: vaccine.status === 'overdue' ? 'Vencida' : shortDay(vaccine.nextDue),
      text: `Vacuna de ${vaccine.name} de ${petName}`,
    });
  }
  return reminders;
}

const DAY_NAMES: Record<Clinic['hours'][number]['day'], string> = {
  MONDAY: 'lunes',
  TUESDAY: 'martes',
  WEDNESDAY: 'miércoles',
  THURSDAY: 'jueves',
  FRIDAY: 'viernes',
  SATURDAY: 'sábado',
  SUNDAY: 'domingo',
};

const WEEK_ORDER: Clinic['hours'][number]['day'][] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

// '08:00:00' → '8:00'.
const shortTime = (time: string): string => time.slice(0, 5).replace(/^0/, '');

/**
 * Horario de la tarjeta «Tu clínica», agrupando los días seguidos con el mismo horario:
 * «Lunes a viernes, 8:00 a 17:00 · Sábado, 8:00 a 13:00». Los días sin atención no se nombran.
 */
export function clinicHoursText(hours: Clinic['hours']): string {
  const groups: { from: string; to: string; range: string }[] = [];
  // Recorre la semana de lunes a domingo, sin depender del orden en que llegan los días.
  let previousRange: string | null = null;
  for (const day of WEEK_ORDER) {
    const entry = hours.find((h) => h.day === day);
    const range = entry?.open && entry.close ? `${shortTime(entry.open)} a ${shortTime(entry.close)}` : null;
    const last = groups.at(-1);
    if (range && last && previousRange === range) {
      last.to = DAY_NAMES[day];
    } else if (range) {
      groups.push({ from: DAY_NAMES[day], to: DAY_NAMES[day], range });
    }
    previousRange = range;
  }
  return groups
    .map(({ from, to, range }) => `${capitalize(from === to ? from : `${from} a ${to}`)}, ${range}`)
    .join(' · ');
}

