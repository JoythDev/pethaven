import {
  APPOINTMENT_KIND_LABELS,
  AgendaSlot,
  Appointment,
  AppointmentKind,
  AppointmentStatus,
  AvailableSlot,
  BookingRequest,
  DayAgenda,
  DayCount,
  OwnerAppointments,
  VetRef,
} from '../../models/demo/appointment.model';
import { Pet } from '../../models/pet.model';
import { addDays, atTime, dayKey, formatTime, isSameDay, isSunday, startOfDay } from './demo-date';
import { seededInt } from './seeded';

export const SLOT_MINUTES = 30;
const WEEKDAY_SLOTS = 18; // 08:00 a 17:00
const SATURDAY_SLOTS = 10; // 08:00 a 13:00
const OPENING_MINUTES = 8 * 60;

// Probabilidad (por mil) de que una mascota tenga cita con un veterinario en un día abierto.
// Las que están en tratamiento vienen a controles seguidos; las estables, rara vez.
const VISIT_PERMILLE_TREATED = 250;
const VISIT_PERMILLE_STABLE = 70;
const MAX_PER_VET_DAY = 9;
// Franja de «confirmada»: las citas que empiezan en las próximas 48 horas.
const CONFIRMED_WINDOW_MS = 48 * 3_600_000;
// Franjas que ocupan otros clientes y que esta demo no puede ver sin backend.
const SIMULATED_BUSY_PERCENT = 35;
// Ventanas con las que se calculan las citas de un cliente.
const OWNER_UPCOMING_DAYS = 30;
const OWNER_PAST_DAYS = 60;

// Horas de inicio de las franjas del día; vacío si la clínica está cerrada (domingo).
export function daySlots(date: Date): string[] {
  if (isSunday(date)) {
    return [];
  }
  const count = date.getDay() === 6 ? SATURDAY_SLOTS : WEEKDAY_SLOTS;
  return Array.from({ length: count }, (_, i) => {
    const minutes = OPENING_MINUTES + i * SLOT_MINUTES;
    return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`;
  });
}

// Semilla de la relación mascota-veterinario: estable aunque cambie la lista de mascotas conocida.
function visitSeed(petId: number, vetId: number): number {
  return petId * 1009 + vetId;
}

function pickKind(pet: Pet, seed: number, key: string): AppointmentKind {
  const roll = seededInt(seed, `kind-${key}`, 100);
  if (roll < 6) {
    return 'urgency';
  }
  if (pet.disease?.trim()) {
    return roll < 75 ? 'control' : 'consultation';
  }
  return roll < 45 ? 'vaccination' : roll < 85 ? 'consultation' : 'control';
}

function statusFor(start: Date, durationMinutes: number, today: Date): AppointmentStatus {
  const end = start.getTime() + durationMinutes * 60_000;
  if (end <= today.getTime()) {
    return 'completed';
  }
  if (start.getTime() <= today.getTime()) {
    return 'in-progress';
  }
  return start.getTime() - today.getTime() <= CONFIRMED_WINDOW_MS ? 'confirmed' : 'scheduled';
}

function buildAppointment(
  pet: Pet,
  vet: VetRef,
  kind: AppointmentKind,
  start: Date,
  durationMinutes: number,
  status: AppointmentStatus,
): Appointment {
  return {
    id: `apt-${dayKey(start)}-${vet.id}-${formatTime(start)}`,
    petId: pet.id,
    petName: pet.name,
    species: pet.species,
    ownerId: pet.ownerId,
    ownerName: pet.ownerName,
    veterinarianId: vet.id,
    veterinarianName: vet.name,
    kind,
    reason: APPOINTMENT_KIND_LABELS[kind],
    start,
    durationMinutes,
    status,
    urgent: kind === 'urgency',
  };
}

// Fin de una cita y si cubre un instante: la misma regla para franjas, huecos libres y traslapes.
function appointmentEnd(a: Appointment): number {
  return a.start.getTime() + a.durationMinutes * 60_000;
}

function covers(a: Appointment, at: number): boolean {
  return a.start.getTime() <= at && at < appointmentEnd(a);
}

// Estado de una cita agendada en la sesión según la hora actual, igual que las generadas.
function withCurrentStatus(a: Appointment, today: Date): Appointment {
  return { ...a, status: statusFor(a.start, a.durationMinutes, today) };
}

function firstFreeSlot(taken: boolean[], preferred: number, span: number): number {
  for (let offset = 0; offset < taken.length; offset++) {
    const index = (preferred + offset) % taken.length;
    if (index + span <= taken.length && taken.slice(index, index + span).every((busy) => !busy)) {
      return index;
    }
  }
  return -1;
}

// Citas de un veterinario real en un día, para las mascotas activas dadas. Sin reloj ni azar:
// depende solo de los ids y de la fecha.
export function appointmentsForVet(vet: VetRef, pets: Pet[], date: Date, today: Date): Appointment[] {
  const slots = daySlots(date);
  if (!slots.length) {
    return [];
  }
  const key = dayKey(date);
  const candidates = pets
    .filter((pet) => {
      if (!pet.active) {
        return false;
      }
      const chance = pet.disease?.trim() ? VISIT_PERMILLE_TREATED : VISIT_PERMILLE_STABLE;
      return seededInt(visitSeed(pet.id, vet.id), `visit-${key}`, 1000) < chance;
    })
    .map((pet) => ({ pet, priority: seededInt(visitSeed(pet.id, vet.id), `priority-${key}`, 1_000_000) }))
    .sort((a, b) => a.priority - b.priority || a.pet.id - b.pet.id)
    .slice(0, MAX_PER_VET_DAY);

  const taken = new Array<boolean>(slots.length).fill(false);
  const result: Appointment[] = [];
  for (const { pet } of candidates) {
    const seed = visitSeed(pet.id, vet.id);
    const kind = pickKind(pet, seed, key);
    const span = kind === 'consultation' && seededInt(seed, `span-${key}`, 2) === 0 ? 2 : 1;
    const index = firstFreeSlot(taken, seededInt(seed, `slot-${key}`, slots.length), span);
    if (index === -1) {
      continue;
    }
    taken.fill(true, index, index + span);
    const start = atTime(date, slots[index]);
    const duration = span * SLOT_MINUTES;
    result.push(buildAppointment(pet, vet, kind, start, duration, statusFor(start, duration, today)));
  }
  return result.sort((a, b) => a.start.getTime() - b.start.getTime());
}

export function appointmentsForVets(vets: VetRef[], pets: Pet[], date: Date, today: Date): Appointment[] {
  return vets
    .flatMap((vet) => appointmentsForVet(vet, pets, date, today))
    .sort((a, b) => a.start.getTime() - b.start.getTime());
}

// Agenda de un veterinario en un día. `appointments` son las citas de ese veterinario
// (generadas más las agendadas en la sesión); las de otros días se ignoran.
export function buildDayAgenda(date: Date, appointments: Appointment[], today: Date): DayAgenda {
  const day = appointments
    .filter((a) => isSameDay(a.start, date))
    .sort((a, b) => a.start.getTime() - b.start.getTime());
  const slots: AgendaSlot[] = daySlots(date).map((time) => {
    const start = atTime(date, time);
    const appointment = day.find((a) => covers(a, start.getTime())) ?? null;
    return { time, start, appointment, starts: appointment !== null && appointment.start.getTime() === start.getTime() };
  });
  const completed = day.filter((a) => a.status === 'completed').length;
  return {
    date,
    closed: isSunday(date),
    slots,
    appointments: day,
    next: day.find((a) => a.start.getTime() > today.getTime()) ?? null,
    counts: {
      total: day.length,
      urgent: day.filter((a) => a.urgent).length,
      completed,
      pending: day.length - completed,
      freeSlots: slots.filter((s) => !s.appointment).length,
    },
  };
}

// Conteo de citas por día para un calendario (puntos de los días con citas).
export function countsByDay(appointments: Appointment[], from: Date, days: number): DayCount[] {
  const first = startOfDay(from);
  return Array.from({ length: days }, (_, i) => {
    const date = addDays(first, i);
    const key = dayKey(date);
    const day = appointments.filter((a) => dayKey(a.start) === key);
    return { date, dateKey: key, count: day.length, urgent: day.filter((a) => a.urgent).length, closed: isSunday(date) };
  });
}

// Una franja de otros clientes que no vemos: ocupada de forma estable según el veterinario y la hora.
function simulatedBusy(vet: VetRef, key: string, time: string): boolean {
  return seededInt(vet.id, `busy-${key}-${time}`, 100) < SIMULATED_BUSY_PERCENT;
}

// Franjas libres de un veterinario para agendar. Descarta las pasadas, las que ocupan sus citas
// (las que conocemos de `pets`, más las agendadas en la sesión) y las que simulan otros clientes.
export function availableSlots(vet: VetRef, pets: Pet[], date: Date, today: Date, booked: Appointment[]): AvailableSlot[] {
  const key = dayKey(date);
  const occupied = [
    ...appointmentsForVet(vet, pets, date, today),
    ...booked.filter((b) => b.veterinarianId === vet.id && isSameDay(b.start, date)),
  ];
  return daySlots(date)
    .map((time) => ({ time, start: atTime(date, time) }))
    .filter(({ time, start }) => {
      const at = start.getTime();
      return (
        at > today.getTime() &&
        !occupied.some((a) => covers(a, at)) &&
        !simulatedBusy(vet, key, time)
      );
    });
}

// Suma las citas agendadas en la sesión a las generadas: si se traslapan con el mismo veterinario,
// gana la agendada. El estado de las agendadas se recalcula con la hora actual.
export function mergeBooked(generated: Appointment[], booked: Appointment[], today: Date): Appointment[] {
  const overlaps = (a: Appointment, b: Appointment): boolean =>
    a.veterinarianId === b.veterinarianId &&
    a.start.getTime() < appointmentEnd(b) &&
    b.start.getTime() < appointmentEnd(a);
  return [
    ...generated.filter((g) => !booked.some((b) => overlaps(g, b))),
    ...booked.map((b) => withCurrentStatus(b, today)),
  ].sort((a, b) => a.start.getTime() - b.start.getTime());
}

// Una cita agendada en la sesión: dura una franja y queda programada.
export function bookingToAppointment(request: BookingRequest): Appointment {
  return buildAppointment(request.pet, request.veterinarian, request.kind, request.start, SLOT_MINUTES, 'scheduled');
}

// Citas de un dueño con los veterinarios que conoce: próximas de la más cercana a la más lejana
// y pasadas de la más reciente a la más antigua. La interfaz muestra solo las primeras de cada lista.
export function ownerAppointments(
  vets: VetRef[],
  ownerPets: Pet[],
  today: Date,
  booked: Appointment[],
): OwnerAppointments {
  const own = booked.filter((a) => ownerPets.some((p) => p.id === a.petId));
  const first = startOfDay(today);
  const generated: Appointment[] = [];
  if (vets.length && ownerPets.length) {
    for (let offset = -OWNER_PAST_DAYS; offset <= OWNER_UPCOMING_DAYS; offset++) {
      generated.push(...appointmentsForVets(vets, ownerPets, addDays(first, offset), today));
    }
  }
  const all = mergeBooked(generated, own, today);
  return {
    upcoming: all.filter((a) => a.status !== 'completed').sort((a, b) => a.start.getTime() - b.start.getTime()),
    past: all.filter((a) => a.status === 'completed').sort((a, b) => b.start.getTime() - a.start.getTime()),
  };
}

// Veterinarios reales que aparecen en los tratamientos de las mascotas de un dueño: el único
// listado de veterinarios que un dueño puede conocer con el API actual.
export function vetsFromTreatments(treatments: { veterinarianId: number; veterinarianName: string }[]): VetRef[] {
  const byId = new Map<number, VetRef>();
  for (const t of treatments) {
    byId.set(t.veterinarianId, { id: t.veterinarianId, name: t.veterinarianName });
  }
  return [...byId.values()].sort((a, b) => a.id - b.id);
}
