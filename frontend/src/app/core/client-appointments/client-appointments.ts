import { formatDate } from '@angular/common';
import {
  APPOINTMENT_KIND_LABELS,
  AppointmentKind,
  AvailableSlot,
  VetRef,
} from '../../models/demo/appointment.model';
import { Pet, Species } from '../../models/pet.model';
import { ClienteTreatment } from '../../models/treatment-cliente.model';
import { daySlots, SLOT_MINUTES } from '../demo/appointments';
import { addDays, dayKey, isSunday, startOfDay } from '../demo/demo-date';
import { parseRouteId } from '../http/route-id';
import { StickerName } from '../stickers/stickers';

// Lógica pura del asistente «Agendar cita» del cliente (demo, S3): validez de cada paso, días y
// franjas disponibles, mascota preelegida con `?mascota=` y textos de confirmación.

export const BOOKING_STEPS = ['Mascota', 'Motivo', 'Fecha y hora', 'Confirmar'] as const;
export const LAST_STEP = BOOKING_STEPS.length - 1;
export const BOOKING_DAYS = 7;
export const BOOKING_DURATION = `${SLOT_MINUTES} min`;

// Opción «Mascota nueva» del primer paso (S21): se registra en la primera visita.
export const NEW_PET = 'new' as const;
// Id de la mascota provisional: negativo para no chocar nunca con una mascota real.
export const NEW_PET_ID = -1;

// Veterinaria de ejemplo para un dueño que aún no tiene ninguna conocida (sin tratamientos).
export const FALLBACK_VET: VetRef = { id: 0, name: 'Dra. Sofía Rojas' };

export interface BookingReason {
  kind: AppointmentKind;
  label: string;
  sticker: StickerName;
}

// Los motivos son los tipos de cita del servicio demo; así lo agendado aparece igual en la agenda.
export const BOOKING_REASONS: readonly BookingReason[] = [
  { kind: 'consultation', label: APPOINTMENT_KIND_LABELS.consultation, sticker: 'heart' },
  { kind: 'vaccination', label: APPOINTMENT_KIND_LABELS.vaccination, sticker: 'syringe' },
  { kind: 'control', label: APPOINTMENT_KIND_LABELS.control, sticker: 'pill' },
  { kind: 'urgency', label: APPOINTMENT_KIND_LABELS.urgency, sticker: 'star' },
];

export type PetChoice = number | typeof NEW_PET | null;

// Lo elegido hasta ahora. El día va como clave 'YYYY-MM-DD' para compararlo sin horas.
export interface BookingDraft {
  pet: PetChoice;
  newPetName: string;
  newPetSpecies: Species;
  kind: AppointmentKind | null;
  day: string | null;
  time: string | null;
  notes: string;
}

export const EMPTY_DRAFT: BookingDraft = {
  pet: null,
  newPetName: '',
  newPetSpecies: Species.DOG,
  kind: null,
  day: null,
  time: null,
  notes: '',
};

/** Si el paso (0 a 3) tiene lo necesario para continuar. */
export function isStepValid(step: number, draft: BookingDraft): boolean {
  switch (step) {
    case 0:
      return draft.pet === NEW_PET ? draft.newPetName.trim() !== '' : draft.pet !== null;
    case 1:
      return draft.kind !== null;
    case 2:
      return draft.day !== null && draft.time !== null;
    default:
      return true;
  }
}

export interface BookableDay {
  key: string;
  date: Date;
  // «Lun», «Mar»…
  weekday: string;
  number: number;
  // El domingo la clínica no atiende.
  disabled: boolean;
}

const WEEKDAYS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'] as const;

/** Los siete días desde hoy (incluido) que se ofrecen para agendar. */
export function bookableDays(today: Date): BookableDay[] {
  const first = startOfDay(today);
  return Array.from({ length: BOOKING_DAYS }, (_, i) => {
    const date = addDays(first, i);
    return { key: dayKey(date), date, weekday: WEEKDAYS[date.getDay()], number: date.getDate(), disabled: isSunday(date) };
  });
}

export interface SlotOption {
  time: string;
  disabled: boolean;
}

/** Todas las franjas del día; las que no están en `available` (ocupadas o pasadas) quedan deshabilitadas. */
export function slotOptions(day: Date, available: AvailableSlot[]): SlotOption[] {
  const free = new Set(available.map((slot) => slot.time));
  return daySlots(day).map((time) => ({ time, disabled: !free.has(time) }));
}

/** Elegir otro día borra la hora elegida; volver a elegir el mismo la conserva. */
export function withDay(draft: BookingDraft, key: string): BookingDraft {
  return key === draft.day ? draft : { ...draft, day: key, time: null };
}

/** Id de `?mascota=` si es una mascota del dueño; null si falta, es inválido o es de otro. */
export function petPreselection(param: string | null, pets: Pick<Pet, 'id'>[]): number | null {
  const id = parseRouteId(param);
  return id !== null && pets.some((pet) => pet.id === id) ? id : null;
}

/**
 * Veterinario de la cita: el del último tratamiento de esa mascota (a igual fecha, el id mayor),
 * si no el primero que el dueño conoce y, sin ninguno, el de ejemplo.
 */
export function vetForPet(petId: number | null, treatments: ClienteTreatment[], knownVets: VetRef[]): VetRef {
  const latest = treatments
    .filter((t) => t.petId === petId)
    .reduce<ClienteTreatment | null>(
      (best, t) => (best === null || t.date > best.date || (t.date === best.date && t.id > best.id) ? t : best),
      null,
    );
  if (latest) {
    return { id: latest.veterinarianId, name: latest.veterinarianName };
  }
  return knownVets[0] ?? FALLBACK_VET;
}

/** Mascota provisional para «Mascota nueva»: el veterinario completa la ficha en la primera visita. */
export function newPetFrom(draft: BookingDraft, owner: { id: number; name: string }): Pet {
  return {
    id: NEW_PET_ID,
    name: draft.newPetName.trim(),
    species: draft.newPetSpecies,
    breed: 'Por registrar',
    age: 0,
    weight: 0,
    disease: null,
    photoUrl: null,
    active: true,
    ownerId: owner.id,
    ownerName: owner.name,
  };
}

const capitalize = (text: string): string => text.charAt(0).toUpperCase() + text.slice(1);

/** «Martes 17 nov». */
export function dayLabel(date: Date): string {
  return capitalize(formatDate(date, 'EEEE d MMM', 'es-CO'));
}

/** «Martes 17 de noviembre» (resumen de la revisión). */
export function longDayLabel(date: Date): string {
  return capitalize(formatDate(date, "EEEE d 'de' MMMM", 'es-CO'));
}

/** Texto de la cita agendada: «{Nombre} tiene cita el {día} {n} de {mes} a las {hora}. Te avisaremos un día antes.» */
export function confirmationText(petName: string, start: Date): string {
  const day = formatDate(start, "EEEE d 'de' MMMM", 'es-CO');
  const time = formatDate(start, 'HH:mm', 'es-CO');
  return `${petName} tiene cita el ${day} a las ${time}. Te avisaremos un día antes.`;
}

/**
 * Citas que muestra «Citas agendadas»: las primeras hasta el límite y, además, las agendadas en esta
 * sesión aunque queden más lejos, para que la persona vea lo que acaba de agendar. Conserva el orden.
 */
export function visibleUpcoming<T extends { id: string }>(upcoming: T[], bookedIds: ReadonlySet<string>, limit: number): T[] {
  return upcoming.filter((appointment, index) => index < limit || bookedIds.has(appointment.id));
}
