import { Appointment, DayAgenda } from '../../models/demo/appointment.model';
import { SLOT_MINUTES } from '../demo/appointments';
import { dayNumber, formatTime, isSameDay } from '../demo/demo-date';
import { IdentityHue, identityHue } from '../identity/identity';

// «Resumen del día» (11.6, S25, S30): línea de tiempo con franjas de 30 min, marcas de hora y «ahora».
// Todo es función pura: la hora actual llega como parámetro.

const TICK_EVERY_MINUTES = 120;
const MINUTE_MS = 60_000;

export interface TimelineSegment {
  // Hora de inicio ('HH:mm') y número de franjas de 30 min que ocupa (una cita de 60 min ocupa dos).
  time: string;
  span: number;
  appointment: Appointment | null;
  free: boolean;
  // Franja libre que ya pasó: no se puede agendar.
  past: boolean;
  completed: boolean;
  hue: IdentityHue | null;
  // Texto del tooltip de PrimeNG (varias líneas) y alternativa de texto para lectores de pantalla.
  tooltip: string;
  label: string;
}

export interface TimelineTick {
  label: string;
  percent: number;
}

export interface UpNext {
  appointment: Appointment;
  time: string;
  relative: string;
  // Textos del bloque: hoy «Sigue · en 25 min» y «09:00 · Luna»; otro día «Primera cita · 08:00» y «Luna».
  kicker: string;
  headline: string;
}

// Hoy, un día que aún no llega o uno que ya pasó, respecto a la hora actual.
export type DayPhase = 'past' | 'today' | 'future';

// Aviso que reemplaza al bloque «Sigue»: día cerrado o sin nada más que atender.
export interface DayNotice {
  title: string;
  note: string;
}

export interface DayTimeline {
  closed: boolean;
  phase: DayPhase;
  // Día cerrado, o día sin cita siguiente (terminado o vacío); null mientras haya un bloque «Sigue».
  notice: DayNotice | null;
  segments: TimelineSegment[];
  ticks: TimelineTick[];
  // Posición de «ahora» en la barra; null fuera del horario de atención o de otro día.
  nowPercent: number | null;
  figures: { total: number; urgent: number; free: number };
  summary: string;
  upNext: UpNext | null;
}

// «ahora», «en 25 min», «en 1 h 30 min».
export function relativeStart(start: Date, now: Date): string {
  const minutes = Math.ceil((start.getTime() - now.getTime()) / MINUTE_MS);
  if (minutes <= 0) {
    return 'ahora';
  }
  if (minutes < 60) {
    return `en ${minutes} min`;
  }
  const rest = minutes % 60;
  return rest ? `en ${Math.floor(minutes / 60)} h ${rest} min` : `en ${minutes / 60} h`;
}

function appointmentEnd(a: Appointment): number {
  return a.start.getTime() + a.durationMinutes * MINUTE_MS;
}

function appointmentSegment(a: Appointment, span: number): TimelineSegment {
  const time = formatTime(a.start);
  const tooltip = `${time} · ${a.petName}\n${a.reason} · ${a.durationMinutes} min\n${a.ownerName}`;
  const flags = [a.urgent ? 'urgencia' : '', a.status === 'completed' ? 'completada' : ''].filter(Boolean);
  return {
    time,
    span,
    appointment: a,
    free: false,
    past: false,
    completed: a.status === 'completed',
    hue: identityHue(a.petId),
    tooltip,
    label: [time, a.petName, a.reason, `${a.durationMinutes} minutos`, `dueño ${a.ownerName}`, ...flags].join(', '),
  };
}

function freeSegment(time: string, past: boolean): TimelineSegment {
  return {
    time,
    span: 1,
    appointment: null,
    free: true,
    past,
    completed: false,
    hue: null,
    tooltip: `${time} · Libre\n${past ? 'Franja pasada' : 'Disponible para agendar'}`,
    label: `${time}, libre${past ? ', franja pasada' : ', disponible para agendar'}`,
  };
}

function plural(count: number, one: string, many: string): string {
  return `${count} ${count === 1 ? one : many}`;
}

function dayPhase(date: Date, now: Date): DayPhase {
  const diff = dayNumber(date) - dayNumber(now);
  return diff === 0 ? 'today' : diff > 0 ? 'future' : 'past';
}

function upNextFor(appointment: Appointment, phase: DayPhase, now: Date): UpNext {
  const time = formatTime(appointment.start);
  const relative = relativeStart(appointment.start, now);
  return phase === 'today'
    ? { appointment, time, relative, kicker: `Sigue · ${relative}`, headline: `${time} · ${appointment.petName}` }
    : { appointment, time, relative, kicker: `Primera cita · ${time}`, headline: appointment.petName };
}

// Aviso del día sin bloque «Sigue»: depende de si es hoy, ya pasó o aún no llega.
function endNotice(phase: DayPhase, total: number): DayNotice {
  if (total > 0) {
    return phase === 'today'
      ? { title: 'Terminaste las citas de hoy', note: `Atendiste ${plural(total, 'cita', 'citas')}.` }
      : { title: 'Día terminado', note: `${total} ${total === 1 ? 'cita atendida' : 'citas atendidas'}.` };
  }
  if (phase === 'past') {
    return { title: 'Sin citas este día', note: 'Ese día no tuviste citas.' };
  }
  return phase === 'today'
    ? { title: 'No hay citas para hoy', note: 'Cuando agenden una, aparecerá aquí.' }
    : { title: 'No hay citas este día', note: 'Cuando agenden una, aparecerá aquí.' };
}

export function buildDayTimeline(agenda: DayAgenda, now: Date): DayTimeline {
  const phase = dayPhase(agenda.date, now);
  const sorted = [...agenda.appointments].sort((a, b) => a.start.getTime() - b.start.getTime());
  // Un día pasado no tiene «siguiente»; uno futuro empieza por su primera cita.
  const upNextAppointment =
    phase === 'past' ? null : (sorted.find((a) => appointmentEnd(a) > now.getTime()) ?? null);
  const upNext: UpNext | null = upNextAppointment ? upNextFor(upNextAppointment, phase, now) : null;

  if (agenda.closed || agenda.slots.length === 0) {
    const title = phase === 'today' ? 'La clínica no atiende hoy.' : 'La clínica no atiende este día.';
    return {
      closed: true,
      phase,
      notice: { title, note: 'Atendemos de lunes a sábado.' },
      segments: [],
      ticks: [],
      nowPercent: null,
      figures: { total: 0, urgent: 0, free: 0 },
      summary: title,
      upNext: null,
    };
  }

  // Una cita larga ocupa varias franjas seguidas: se junta en un solo segmento.
  const segments: TimelineSegment[] = [];
  for (const slot of agenda.slots) {
    if (slot.appointment && !slot.starts && segments.length) {
      segments[segments.length - 1].span += 1;
    } else if (slot.appointment) {
      segments.push(appointmentSegment(slot.appointment, 1));
    } else {
      segments.push(freeSegment(slot.time, slot.start.getTime() + SLOT_MINUTES * MINUTE_MS <= now.getTime()));
    }
  }

  const totalMinutes = agenda.slots.length * SLOT_MINUTES;
  const opening = agenda.slots[0].start;
  const openingHour = opening.getHours() + opening.getMinutes() / 60;
  const ticks: TimelineTick[] = [];
  for (let offset = 0; offset < totalMinutes; offset += TICK_EVERY_MINUTES) {
    ticks.push({
      label: String(Math.floor(openingHour + offset / 60)).padStart(2, '0'),
      percent: (offset / totalMinutes) * 100,
    });
  }

  const sinceOpening = (now.getTime() - opening.getTime()) / MINUTE_MS;
  const nowPercent =
    isSameDay(agenda.date, now) && sinceOpening >= 0 && sinceOpening < totalMinutes
      ? (sinceOpening / totalMinutes) * 100
      : null;

  const free = segments.filter((s) => s.free && !s.past).length;
  return {
    closed: false,
    phase,
    notice: upNext ? null : endNotice(phase, agenda.counts.total),
    segments,
    ticks,
    nowPercent,
    figures: { total: agenda.counts.total, urgent: agenda.counts.urgent, free },
    summary: `Línea de tiempo del día: ${plural(agenda.counts.total, 'cita', 'citas')} y ${plural(free, 'franja libre', 'franjas libres')}.`,
    upNext,
  };
}
