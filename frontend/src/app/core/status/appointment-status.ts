import { APPOINTMENT_STATUS_LABELS, Appointment } from '../../models/appointment.model';
import { StatusInfo, TagSeverity } from './pet-status';

const SEVERITY: Record<Appointment['status'], TagSeverity> = {
  SCHEDULED: 'info',
  IN_PROGRESS: 'warn',
  COMPLETED: 'success',
  CANCELLED: 'secondary',
  NO_SHOW: 'danger',
};

// Estado de una cita con su variante de p-tag (12.3).
export function appointmentStatus(appointment: Pick<Appointment, 'status'>): StatusInfo {
  return { label: APPOINTMENT_STATUS_LABELS[appointment.status], severity: SEVERITY[appointment.status] };
}
