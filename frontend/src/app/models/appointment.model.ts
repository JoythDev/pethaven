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
