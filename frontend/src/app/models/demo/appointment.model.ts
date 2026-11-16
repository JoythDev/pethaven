// Citas (demo, S3): funcionalidad pendiente de backend. Todo se genera a partir de entidades reales.
import { Pet, Species } from '../pet.model';
import { Veterinarian } from '../veterinarian.model';

export type AppointmentKind = 'control' | 'vaccination' | 'consultation' | 'urgency';

export const APPOINTMENT_KIND_LABELS: Record<AppointmentKind, string> = {
  control: 'Control de tratamiento',
  vaccination: 'Vacunación',
  consultation: 'Consulta general',
  urgency: 'Urgencia',
};

export type AppointmentStatus = 'scheduled' | 'confirmed' | 'in-progress' | 'completed';

export const APPOINTMENT_STATUS_LABELS: Record<AppointmentStatus, string> = {
  scheduled: 'Programada',
  confirmed: 'Confirmada',
  'in-progress': 'En curso',
  completed: 'Completada',
};

// Lo mínimo que se necesita saber de un veterinario real para agendarle citas.
export type VetRef = Pick<Veterinarian, 'id' | 'name'>;

export interface Appointment {
  id: string;
  petId: number;
  petName: string;
  species: Species;
  ownerId: number;
  ownerName: string;
  veterinarianId: number;
  veterinarianName: string;
  kind: AppointmentKind;
  // Motivo en texto; coincide con la etiqueta del tipo.
  reason: string;
  start: Date;
  durationMinutes: number;
  status: AppointmentStatus;
  urgent: boolean;
}

// Una franja de 30 minutos de la jornada. `appointment` es la cita que ocupa la franja
// (una de 60 minutos cubre dos) y `starts` indica que la cita empieza en esta franja.
export interface AgendaSlot {
  time: string;
  start: Date;
  appointment: Appointment | null;
  starts: boolean;
}

export interface DayAgenda {
  date: Date;
  closed: boolean;
  slots: AgendaSlot[];
  appointments: Appointment[];
  // Primera cita que aún no ha empezado; null si no queda ninguna.
  next: Appointment | null;
  counts: { total: number; urgent: number; completed: number; pending: number; freeSlots: number };
}

export interface DayCount {
  date: Date;
  dateKey: string;
  count: number;
  urgent: number;
  closed: boolean;
}

export interface OwnerAppointments {
  upcoming: Appointment[];
  past: Appointment[];
}

export interface AvailableSlot {
  time: string;
  start: Date;
}

// Datos para agendar: la mascota y el veterinario son entidades reales.
export interface BookingRequest {
  pet: Pet;
  veterinarian: VetRef;
  kind: AppointmentKind;
  start: Date;
}
