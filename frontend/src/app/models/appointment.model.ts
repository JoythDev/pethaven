import { Species } from './pet.model';

// Citas tal como las maneja el API (AppointmentResponse y sus peticiones). Las fechas llegan en hora
// local de la clínica, sin zona; el servicio las convierte a Date al recibirlas.

export type AppointmentReason =
  | 'CONTROL'
  | 'VACCINATION'
  | 'CONSULTATION'
  | 'DEWORMING'
  | 'NUTRITION'
  | 'FIRST_VISIT'
  | 'DISCHARGE'
  | 'BRIEFING';

export type AppointmentStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export type AppointmentActor = 'OWNER' | 'VETERINARIAN' | 'SYSTEM';

export type AppointmentOutcome = 'AMBULATORY' | 'ADMISSION';

export const APPOINTMENT_REASON_LABELS: Record<AppointmentReason, string> = {
  CONTROL: 'Control',
  VACCINATION: 'Vacunación',
  CONSULTATION: 'Consulta general',
  DEWORMING: 'Desparasitación',
  NUTRITION: 'Nutrición',
  FIRST_VISIT: 'Primera consulta',
  DISCHARGE: 'Alta y entrega',
  BRIEFING: 'Reunión informativa',
};

export const APPOINTMENT_STATUS_LABELS: Record<AppointmentStatus, string> = {
  SCHEDULED: 'Programada',
  IN_PROGRESS: 'En curso',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
  NO_SHOW: 'No asistió',
};

// Respuesta del API (AppointmentResponse), con las fechas aún como texto.
export interface AppointmentResponse {
  id: number;
  startTime: string;
  endTime: string;
  reason: AppointmentReason;
  status: AppointmentStatus;
  petId: number | null;
  petName: string;
  species: Species;
  provisionalPet: boolean;
  provisionalBreed: string | null;
  provisionalAge: number | null;
  ownerId: number;
  ownerName: string;
  ownerPhone: string;
  veterinarianId: number;
  veterinarianName: string;
  ownerNotes: string | null;
  clinicalNotes: string | null;
  outcome: AppointmentOutcome | null;
  treatmentId: number | null;
  createdAt: string;
  cancelledBy: AppointmentActor | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
}

// Cita lista para la vista: inicio y fin ya son Date.
export interface Appointment extends Omit<AppointmentResponse, 'startTime' | 'endTime'> {
  start: Date;
  end: Date;
}

// Mascota aún no registrada (ProvisionalPetRequest).
export interface ProvisionalPetRequest {
  name: string;
  species: Species;
  breed: string | null;
  age: number | null;
}

// Reserva del dueño (AppointmentRequest): `petId` o `provisionalPet`, nunca los dos.
export interface AppointmentRequest {
  petId: number | null;
  provisionalPet: ProvisionalPetRequest | null;
  reason: AppointmentReason | null;
  startTime: string;
  ownerNotes: string | null;
}

// Nueva franja de una cita (AppointmentRescheduleRequest).
export interface AppointmentRescheduleRequest {
  startTime: string;
  ownerNotes: string | null;
}

// Disponibilidad de un día (AvailabilityDayResponse): las franjas llegan como 'HH:mm:ss'.
export interface AvailabilityDay {
  date: string;
  open: boolean;
  freeSlots: string[];
}

// Motivos de las citas que reserva el dueño: al completarlas, el veterinario elige ambulatoria o ingreso.
export const OWNER_REASONS: readonly AppointmentReason[] = [
  'CONTROL',
  'VACCINATION',
  'CONSULTATION',
  'DEWORMING',
  'NUTRITION',
  'FIRST_VISIT',
];

// Motivos que agenda el veterinario para una mascota hospitalizada (S16.1).
export type VeterinarianReason = Extract<AppointmentReason, 'DISCHARGE' | 'BRIEFING'>;

// Cita del veterinario con el dueño de una mascota hospitalizada (VeterinarianAppointmentRequest).
export interface VeterinarianAppointmentRequest {
  petId: number;
  reason: VeterinarianReason;
  startTime: string;
}

// Cierre de una cita en curso (AppointmentCompletionRequest).
export interface AppointmentCompletionRequest {
  outcome: AppointmentOutcome | null;
  treatmentId: number | null;
  clinicalNotes: string | null;
}

// Una franja del horario de la clínica en la agenda del veterinario; `appointment` es la cita que la
// ocupa (las canceladas la dejan libre).
export interface AgendaSlot {
  time: string;
  start: Date;
  end: Date;
  appointment: Appointment | null;
}

export interface DayAgenda {
  date: Date;
  closed: boolean;
  slots: AgendaSlot[];
  // Citas del día sin las canceladas, por hora.
  appointments: Appointment[];
  counts: { total: number; completed: number; freeSlots: number };
}

// Citas de un día para el calendario y «Esta semana» (sin las canceladas).
export interface DayCount {
  date: Date;
  dateKey: string;
  count: number;
  completed: number;
  closed: boolean;
}
