import { formatDate } from '@angular/common';
import {
  APPOINTMENT_REASON_LABELS,
  Appointment,
  AppointmentCompletionRequest,
  AppointmentOutcome,
  OWNER_REASONS,
  VeterinarianReason,
} from '../../models/appointment.model';
import { Species } from '../../models/pet.model';
import { addDays, dayKey, isSameDay } from '../time/calendar';

// Lógica pura de las acciones del veterinario sobre sus citas (S16.7–S16.9): qué se puede hacer en
// cada estado, los textos que lo explican y las peticiones de cierre y de cita nueva.

export const CLINICAL_NOTES_MAX = 1000;
// Días que muestran los diálogos para elegir franja (hoy incluido).
export const PICKER_DAYS = 14;

/** Rango 'YYYY-MM-DD' de los diálogos que eligen franja: desde hoy, {@link PICKER_DAYS} días. */
export function pickerRange(today: Date): { from: string; to: string } {
  return { from: dayKey(today), to: dayKey(addDays(today, PICKER_DAYS - 1)) };
}

// ---------- Estados y acciones ----------

/** «Iniciar consulta»: una cita programada, solo el día en que está agendada. */
export function canStart(appointment: Appointment, now: Date): boolean {
  return appointment.status === 'SCHEDULED' && isSameDay(appointment.start, now);
}

/** «No asistió»: una cita programada cuya hora de inicio ya pasó. */
export function canMarkNoShow(appointment: Appointment, now: Date): boolean {
  return appointment.status === 'SCHEDULED' && appointment.start.getTime() <= now.getTime();
}

/** Por qué «Iniciar consulta» está deshabilitado; null si se puede iniciar. */
export function startHint(appointment: Appointment, now: Date): string | null {
  if (canStart(appointment, now)) {
    return null;
  }
  return appointment.start.getTime() > now.getTime()
    ? `Podrás iniciarla el ${formatDate(appointment.start, "EEEE d 'de' MMMM", 'es-CO')}.`
    : 'El día de la cita ya pasó: márcala como «No asistió» o cancélala.';
}

/** Una cita en curso con mascota provisional se completa después de registrar a la mascota. */
export function needsPetRegistration(appointment: Appointment): boolean {
  return appointment.status === 'IN_PROGRESS' && appointment.petId === null;
}

export type CompletionKind = 'consultation' | 'discharge' | 'briefing';

/** Qué pide el cierre: las citas del dueño eligen ambulatoria o ingreso; alta y reunión, las suyas. */
export function completionKind(appointment: Pick<Appointment, 'reason'>): CompletionKind {
  if (appointment.reason === 'DISCHARGE') {
    return 'discharge';
  }
  return appointment.reason === 'BRIEFING' ? 'briefing' : 'consultation';
}

export const isOwnerAppointment = (appointment: Pick<Appointment, 'reason'>): boolean =>
  OWNER_REASONS.includes(appointment.reason);

/** Si la nota clínica es obligatoria al cerrar (ambulatoria y reunión informativa). */
export function notesRequired(kind: CompletionKind, outcome: AppointmentOutcome | null): boolean {
  return kind === 'briefing' || (kind === 'consultation' && outcome === 'AMBULATORY');
}

/** Cierre sin tratamiento: ambulatoria, alta y entrega o reunión informativa. El ingreso cierra desde el tratamiento. */
export function completionRequest(kind: CompletionKind, notes: string): AppointmentCompletionRequest {
  return {
    outcome: kind === 'consultation' ? 'AMBULATORY' : null,
    treatmentId: null,
    clinicalNotes: notes.trim() || null,
  };
}

/** Cierre de una consulta que terminó en ingreso, con el tratamiento recién registrado. */
export function admissionRequest(treatmentId: number): AppointmentCompletionRequest {
  return { outcome: 'ADMISSION', treatmentId, clinicalNotes: null };
}

// ---------- Textos ----------

export const OUTCOME_LABELS: Record<AppointmentOutcome, string> = {
  AMBULATORY: 'Ambulatoria',
  ADMISSION: 'Ingreso',
};

/** «Perro · Labrador · 3 años» con los datos provisionales que dejó el dueño. */
export function provisionalPetLine(appointment: Appointment): string {
  const species = appointment.species === Species.CAT ? 'Gato' : 'Perro';
  const age = appointment.provisionalAge === null ? null : appointment.provisionalAge === 1 ? '1 año' : `${appointment.provisionalAge} años`;
  return [species, appointment.provisionalBreed, age].filter(Boolean).join(' · ');
}

/** Parámetros de la Agenda que abren el día y la cita (vuelta desde los formularios). */
export function agendaParams(appointment: Pick<Appointment, 'id' | 'start'>): Record<string, string> {
  return { dia: dayKey(appointment.start), cita: String(appointment.id) };
}

export const reasonLabel = (appointment: Pick<Appointment, 'reason'>): string => APPOINTMENT_REASON_LABELS[appointment.reason];

// ---------- Cita nueva del veterinario ----------

export interface VetReasonOption {
  label: string;
  value: VeterinarianReason;
}

export const VET_REASON_OPTIONS: readonly VetReasonOption[] = [
  { label: APPOINTMENT_REASON_LABELS.DISCHARGE, value: 'DISCHARGE' },
  { label: APPOINTMENT_REASON_LABELS.BRIEFING, value: 'BRIEFING' },
];
