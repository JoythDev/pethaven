import { formatDate, formatNumber } from '@angular/common';
import {
  APPOINTMENT_STATUS_LABELS,
  Appointment,
  AppointmentStatus,
  DayAgenda,
  DayCount,
} from '../../models/demo/appointment.model';
import { Pet } from '../../models/pet.model';
import { Treatment } from '../../models/treatment.model';
import { TimelineSegment } from '../day-timeline/day-timeline';
import { addDays, dayKey, isSameDay, isSunday, startOfDay } from '../demo/demo-date';
import { TagSeverity } from '../status/pet-status';

// Lógica pura de la Agenda del veterinario: URL, etiquetas, calendario y detalle de la visita.

const LOCALE = 'es-CO';
const DAY_PARAM = /^(\d{4})-(\d{2})-(\d{2})$/;
const OPEN_DAYS = 6;

export type AgendaView = 'dia' | 'semana';

export interface AgendaState {
  day: Date;
  view: AgendaView;
  appointmentId: string | null;
}

// Lo que necesita de los parámetros de la URL: sirve `ParamMap` y `URLSearchParams`.
export interface ParamReader {
  get(name: string): string | null;
}

const PARAM = { day: 'dia', view: 'vista', appointment: 'cita' } as const;

function parseDay(raw: string | null): Date | null {
  const match = raw ? DAY_PARAM.exec(raw) : null;
  if (!match) {
    return null;
  }
  const [year, month, day] = [Number(match[1]), Number(match[2]), Number(match[3])];
  const date = new Date(year, month - 1, day);
  // Una fecha que el calendario corrige (30 de febrero) no es válida.
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day ? date : null;
}

/** Estado de la agenda según la URL; lo ausente o inválido cae en hoy, vista del día y sin cita. */
export function parseAgendaParams(params: ParamReader, today: Date): AgendaState {
  const appointmentId = params.get(PARAM.appointment)?.trim();
  return {
    day: parseDay(params.get(PARAM.day)) ?? startOfDay(today),
    view: params.get(PARAM.view) === 'semana' ? 'semana' : 'dia',
    appointmentId: appointmentId || null,
  };
}

/** Parámetros de la URL del estado: omite lo que vale lo mismo que el valor por defecto. */
export function agendaQueryParams(state: AgendaState, today: Date): Record<string, string> {
  const params: Record<string, string> = {};
  if (!isSameDay(state.day, today)) {
    params[PARAM.day] = dayKey(state.day);
  }
  if (state.view === 'semana') {
    params[PARAM.view] = 'semana';
  }
  if (state.appointmentId) {
    params[PARAM.appointment] = state.appointmentId;
  }
  return params;
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

// «Martes 17 de noviembre».
export function dayLabelEs(date: Date): string {
  return capitalize(formatDate(date, "EEEE d 'de' MMMM", LOCALE));
}

// «Noviembre 2026».
export function monthTitleEs(date: Date): string {
  return capitalize(formatDate(date, 'MMMM y', LOCALE));
}

const plural = (count: number, one: string, many: string): string => `${count} ${count === 1 ? one : many}`;

// Subtítulo de la página: el día y cuántas citas tiene; sin la agenda solo el día.
export function agendaSubtitle(date: Date, agenda: DayAgenda | null): string {
  const label = dayLabelEs(date);
  if (!agenda) {
    return label;
  }
  if (agenda.closed) {
    return `${label} · La clínica no atiende`;
  }
  return `${label} · ${agenda.counts.total ? plural(agenda.counts.total, 'cita', 'citas') : 'Sin citas'}`;
}

// «17 de noviembre, 9 citas»: lo que lee el lector de pantalla en cada día del calendario.
export function dayCellLabel(date: Date, count: DayCount | null): string {
  const label = formatDate(date, "d 'de' MMMM", LOCALE);
  if (count?.closed ?? isSunday(date)) {
    return `${label}, cerrado`;
  }
  if (!count) {
    return label;
  }
  return `${label}, ${count.count ? plural(count.count, 'cita', 'citas') : 'sin citas'}`;
}

/** Semanas del mes con el lunes primero; los días de otro mes van en `null`. */
export function monthGrid(month: Date): (Date | null)[][] {
  const first = new Date(month.getFullYear(), month.getMonth(), 1);
  const days = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  const offset = (first.getDay() + 6) % 7;
  const cells: (Date | null)[] = [
    ...Array<null>(offset).fill(null),
    ...Array.from({ length: days }, (_, i) => new Date(first.getFullYear(), first.getMonth(), i + 1)),
  ];
  while (cells.length % 7 !== 0) {
    cells.push(null);
  }
  return Array.from({ length: cells.length / 7 }, (_, i) => cells.slice(i * 7, i * 7 + 7));
}

/** Los seis días de atención (lunes a sábado) de la semana que contiene la fecha. */
export function weekDays(date: Date): Date[] {
  const monday = addDays(startOfDay(date), -((date.getDay() + 6) % 7));
  return Array.from({ length: OPEN_DAYS }, (_, i) => addDays(monday, i));
}

/** Cita que arranca seleccionada: la que sigue sin terminar si el día es hoy; si no, la primera. */
export function initialAppointment(agenda: DayAgenda, now: Date): Appointment | null {
  const list = agenda.appointments;
  if (!list.length) {
    return null;
  }
  if (isSameDay(agenda.date, now)) {
    const current = list.find((a) => a.start.getTime() + a.durationMinutes * 60_000 > now.getTime());
    if (current) {
      return current;
    }
  }
  return list[0];
}

/** La cita del id si está en la agenda; si no, la inicial (un id viejo de la URL no rompe nada). */
export function resolveSelection(agenda: DayAgenda, id: string | null, now: Date): Appointment | null {
  return agenda.appointments.find((a) => a.id === id) ?? initialAppointment(agenda, now);
}

export interface TimelineRow {
  segment: TimelineSegment;
  // Fila de la rejilla (desde 1) donde empieza el segmento y cuántas franjas ocupa.
  row: number;
  span: number;
}

/** Posición de cada segmento en una rejilla de una fila por franja de 30 min. */
export function timelineRows(segments: TimelineSegment[]): TimelineRow[] {
  let row = 1;
  return segments.map((segment) => {
    const placed = { segment, row, span: segment.span };
    row += segment.span;
    return placed;
  });
}

export interface AppointmentStatusTag {
  label: string;
  severity: TagSeverity;
}

const STATUS_SEVERITY: Record<AppointmentStatus, TagSeverity> = {
  scheduled: 'secondary',
  confirmed: 'info',
  'in-progress': 'warn',
  completed: 'success',
};

export function appointmentStatusTag(status: AppointmentStatus): AppointmentStatusTag {
  return { label: APPOINTMENT_STATUS_LABELS[status], severity: STATUS_SEVERITY[status] };
}

// «12 nov 2026» con la última atención real de la mascota; sin ninguna, «Sin visitas previas».
export function lastVisitLabel(treatments: Treatment[], petId: number): string {
  const latest = treatments
    .filter((t) => t.petId === petId)
    .reduce<Treatment | null>(
      (best, t) => (!best || new Date(t.date).getTime() > new Date(best.date).getTime() ? t : best),
      null,
    );
  return latest ? formatDate(latest.date, 'd MMM y', LOCALE) : 'Sin visitas previas';
}

// «Labrador · 4 años · 28 kg»: raza, edad y peso reales de la mascota.
export function petSummaryLine(pet: Pick<Pet, 'breed' | 'age' | 'weight'>): string {
  const age = pet.age === 1 ? '1 año' : `${pet.age} años`;
  return `${pet.breed} · ${age} · ${formatNumber(pet.weight, LOCALE, '1.0-1')} kg`;
}
