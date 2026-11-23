import { formatDate } from '@angular/common';
import {
  APPOINTMENT_REASON_LABELS,
  Appointment,
  AppointmentReason,
  AppointmentRequest,
  AvailabilityDay,
} from '../../models/appointment.model';
import { Clinic, DayOfWeek } from '../../models/clinic.model';
import { Pet, Species, isHospitalized } from '../../models/pet.model';
import { parseRouteId } from '../http/route-id';
import { StickerName } from '../stickers/stickers';
import { atTime, formatTime } from '../time/calendar';

// Lógica pura de «Agendar cita» del cliente (S16): validez de cada paso, días y franjas a partir de la
// disponibilidad del API y del horario de la clínica, qué mascotas se pueden agendar, la petición de
// reserva, la reprogramación y los textos de confirmación.

export const BOOKING_STEPS = ['Mascota', 'Motivo', 'Fecha y hora', 'Confirmar'] as const;
export const LAST_STEP = BOOKING_STEPS.length - 1;
export const DATE_STEP = 2;

// Antelación mínima del dueño para reservar, reprogramar o cancelar (S16.5, S16.8).
export const MIN_NOTICE_HOURS = 2;

// Opción «Mascota nueva» del primer paso: se registra en la primera visita.
export const NEW_PET = 'new' as const;
export const NEW_PET_NAME_MAX = 60;
export const NOTES_MAX = 500;

export interface BookingReason {
  reason: AppointmentReason;
  label: string;
  sticker: StickerName;
}

// Motivos que el dueño puede reservar; las urgencias no se agendan en la app (S16.2).
export const BOOKING_REASONS: readonly BookingReason[] = [
  { reason: 'CONSULTATION', label: APPOINTMENT_REASON_LABELS.CONSULTATION, sticker: 'heart' },
  { reason: 'VACCINATION', label: APPOINTMENT_REASON_LABELS.VACCINATION, sticker: 'syringe' },
  { reason: 'CONTROL', label: APPOINTMENT_REASON_LABELS.CONTROL, sticker: 'pill' },
  { reason: 'DEWORMING', label: APPOINTMENT_REASON_LABELS.DEWORMING, sticker: 'shield' },
  { reason: 'NUTRITION', label: APPOINTMENT_REASON_LABELS.NUTRITION, sticker: 'food-bowl' },
];

export type PetChoice = number | typeof NEW_PET | null;

// Lo elegido hasta ahora. El día va como clave 'YYYY-MM-DD' para compararlo sin horas.
export interface BookingDraft {
  pet: PetChoice;
  newPetName: string;
  newPetSpecies: Species;
  reason: AppointmentReason | null;
  day: string | null;
  time: string | null;
  notes: string;
}

export const EMPTY_DRAFT: BookingDraft = {
  pet: null,
  newPetName: '',
  newPetSpecies: Species.DOG,
  reason: null,
  day: null,
  time: null,
  notes: '',
};

/** Si el paso (0 a 3) tiene lo necesario para continuar. Una mascota nueva no elige motivo: es su primera consulta. */
export function isStepValid(step: number, draft: BookingDraft): boolean {
  switch (step) {
    case 0:
      return draft.pet === NEW_PET ? draft.newPetName.trim() !== '' : draft.pet !== null;
    case 1:
      return draft.pet === NEW_PET || draft.reason !== null;
    case 2:
      return draft.day !== null && draft.time !== null;
    default:
      return true;
  }
}

/** Motivo que se muestra: el elegido o, para una mascota nueva, «Primera consulta». */
export function draftReasonLabel(draft: BookingDraft): string | null {
  if (draft.pet === NEW_PET) {
    return APPOINTMENT_REASON_LABELS.FIRST_VISIT;
  }
  return draft.reason ? APPOINTMENT_REASON_LABELS[draft.reason] : null;
}

// ---------- Mascotas ----------

/** Citas que aún cuentan como activas para su mascota: programadas a futuro o en curso (S16.5). */
export function isActiveAppointment(appointment: Appointment, now: Date): boolean {
  return appointment.status === 'IN_PROGRESS' || (appointment.status === 'SCHEDULED' && appointment.start >= now);
}

/**
 * Por qué no se puede agendar a la mascota (null si sí se puede): las hospitalizadas las agenda el
 * veterinario y cada mascota tiene como máximo una cita activa.
 */
export function petBlocker(pet: Pet, appointments: Appointment[], now: Date): string | null {
  if (isHospitalized(pet)) {
    return 'Hospitalizada: tu veterinario agenda sus citas';
  }
  const active = appointments.find((appointment) => appointment.petId === pet.id && isActiveAppointment(appointment, now));
  return active ? `Ya tiene cita el ${dayLabel(active.start).toLowerCase()}` : null;
}

/** Id de `?mascota=` si es una mascota del dueño que se puede agendar; null en cualquier otro caso. */
export function petPreselection(param: string | null, bookable: Pick<Pet, 'id'>[]): number | null {
  const id = parseRouteId(param);
  return id !== null && bookable.some((pet) => pet.id === id) ? id : null;
}

// ---------- Días y franjas ----------

export interface BookableDay {
  key: string;
  date: Date;
  // «Lun», «Mar»…
  weekday: string;
  number: number;
  // La clínica no atiende ese día.
  disabled: boolean;
  freeSlots: string[];
}

const WEEKDAYS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'] as const;
const DAYS_OF_WEEK: DayOfWeek[] = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

// 'YYYY-MM-DD' del API como fecha local, sin desplazarla por la zona horaria.
function localDate(key: string): Date {
  const [year, month, day] = key.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** Los días de la ventana de reserva, tal como los devuelve el API; las franjas quedan como 'HH:mm'. */
export function bookableDays(availability: AvailabilityDay[]): BookableDay[] {
  return availability.map((entry) => {
    const date = localDate(entry.date);
    return {
      key: entry.date,
      date,
      weekday: WEEKDAYS[date.getDay()],
      number: date.getDate(),
      disabled: !entry.open,
      freeSlots: entry.freeSlots.map((slot) => slot.slice(0, 5)),
    };
  });
}

/** Inicio de cada franja del día según el horario de la clínica ('HH:mm'); vacío si no atiende. */
export function clinicDaySlots(date: Date, clinic: Pick<Clinic, 'hours' | 'slotMinutes'>): string[] {
  const hours = clinic.hours.find((entry) => entry.day === DAYS_OF_WEEK[date.getDay()]);
  if (!hours?.open || !hours.close) {
    return [];
  }
  const close = atTime(date, hours.close.slice(0, 5));
  const slots: string[] = [];
  for (let start = atTime(date, hours.open.slice(0, 5)); start.getTime() + clinic.slotMinutes * 60_000 <= close.getTime();
    start = new Date(start.getTime() + clinic.slotMinutes * 60_000)) {
    slots.push(formatTime(start));
  }
  return slots;
}

export interface SlotOption {
  time: string;
  disabled: boolean;
}

/** Todas las franjas del día; las que no están libres (ocupadas o sin la antelación mínima) quedan deshabilitadas. */
export function slotOptions(day: BookableDay, clinic: Pick<Clinic, 'hours' | 'slotMinutes'>): SlotOption[] {
  const free = new Set(day.freeSlots);
  return clinicDaySlots(day.date, clinic).map((time) => ({ time, disabled: !free.has(time) }));
}

/** Elegir otro día borra la hora elegida; volver a elegir el mismo la conserva. */
export function withDay(draft: BookingDraft, key: string): BookingDraft {
  return key === draft.day ? draft : { ...draft, day: key, time: null };
}

// ---------- Peticiones ----------

/** Fecha y hora en el formato local sin zona que espera el API ('2026-11-24T09:30:00'). */
export function apiDateTime(day: string, time: string): string {
  return `${day}T${time}:00`;
}

const clean = (text: string): string | null => (text.trim() === '' ? null : text.trim());

/** Petición de reserva: mascota registrada con su motivo, o mascota nueva (primera consulta). */
export function bookingRequest(draft: BookingDraft): AppointmentRequest | null {
  if (!draft.day || !draft.time || draft.pet === null) {
    return null;
  }
  const base = { startTime: apiDateTime(draft.day, draft.time), ownerNotes: clean(draft.notes) };
  if (draft.pet === NEW_PET) {
    return {
      ...base,
      petId: null,
      reason: null,
      provisionalPet: { name: draft.newPetName.trim(), species: draft.newPetSpecies, breed: null, age: null },
    };
  }
  return draft.reason ? { ...base, petId: draft.pet, reason: draft.reason, provisionalPet: null } : null;
}

// ---------- Citas del dueño ----------

/** Próximas (la más cercana primero) y anteriores (la más reciente primero). */
export function splitAppointments(appointments: Appointment[], now: Date): { upcoming: Appointment[]; past: Appointment[] } {
  const upcoming = appointments.filter((appointment) => isActiveAppointment(appointment, now))
    .sort((a, b) => a.start.getTime() - b.start.getTime());
  const past = appointments.filter((appointment) => !isActiveAppointment(appointment, now))
    .sort((a, b) => b.start.getTime() - a.start.getTime());
  return { upcoming, past };
}

/** El dueño puede cambiar o cancelar una cita programada hasta {@link MIN_NOTICE_HOURS} horas antes. */
export function canChange(appointment: Appointment, now: Date): boolean {
  return appointment.status === 'SCHEDULED' && appointment.start.getTime() - now.getTime() >= MIN_NOTICE_HOURS * 3_600_000;
}

/** Borrador para reprogramar: la misma mascota y motivo, sin día ni hora. */
export function rescheduleDraft(appointment: Appointment): BookingDraft {
  return {
    ...EMPTY_DRAFT,
    pet: appointment.petId ?? NEW_PET,
    newPetName: appointment.petId === null ? appointment.petName : '',
    newPetSpecies: appointment.species,
    reason: appointment.reason,
    notes: appointment.ownerNotes ?? '',
  };
}

export const reasonLabel = (appointment: Pick<Appointment, 'reason'>): string => APPOINTMENT_REASON_LABELS[appointment.reason];

// ---------- Textos ----------

const capitalize = (text: string): string => text.charAt(0).toUpperCase() + text.slice(1);

/** «Martes 17 nov». */
export function dayLabel(date: Date): string {
  return capitalize(formatDate(date, 'EEEE d MMM', 'es-CO'));
}

/** «Martes 17 de noviembre» (resumen de la revisión). */
export function longDayLabel(date: Date): string {
  return capitalize(formatDate(date, "EEEE d 'de' MMMM", 'es-CO'));
}

/** «{Mascota} tiene cita el {día} a las {hora} con {veterinario}. Te avisaremos un día antes.» */
export function confirmationText(appointment: Pick<Appointment, 'petName' | 'start' | 'veterinarianName'>): string {
  const day = formatDate(appointment.start, "EEEE d 'de' MMMM", 'es-CO');
  const time = formatDate(appointment.start, 'HH:mm', 'es-CO');
  return `${appointment.petName} tiene cita el ${day} a las ${time} con ${appointment.veterinarianName}. Te avisaremos un día antes.`;
}
