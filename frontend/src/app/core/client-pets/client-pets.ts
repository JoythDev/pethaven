import { Appointment } from '../../models/appointment.model';
import { Pet } from '../../models/pet.model';
import { PetAreaConfig } from '../pets-list/pet-area';

// Lógica pura de «Mis mascotas» del cliente (11.7): subtítulo, último tratamiento y próxima cita de cada tarjeta.

/** «3 mascotas registradas a tu nombre»; en singular y sin mascotas el texto cambia. */
export function petCountText(count: number): string {
  if (count === 0) {
    return 'Aún no tienes mascotas registradas';
  }
  return count === 1 ? '1 mascota registrada a tu nombre' : `${count} mascotas registradas a tu nombre`;
}

/** Diagnóstico del tratamiento más reciente (a igual fecha, el id mayor); null si no hay ninguno. */
export function lastDiagnosis(treatments: { id: number; date: string; diagnosis: string }[]): string | null {
  const latest = treatments.reduce<(typeof treatments)[number] | null>(
    (best, current) =>
      best === null || current.date.localeCompare(best.date) > 0 || (current.date === best.date && current.id > best.id)
        ? current
        : best,
    null,
  );
  return latest?.diagnosis ?? null;
}

/** Primera cita próxima de una mascota entre las citas próximas del dueño (ya ordenadas); null si no tiene. */
export const nextAppointmentFor = (petId: number, upcoming: Appointment[]): Appointment | null =>
  upcoming.find((appointment) => appointment.petId === petId) ?? null;

/** Enlace a «Citas» del dueño (`/cliente/:ownerId/citas`); con un id inválido apunta a `x`, como el resto del área. */
export const bookingLink = (ownerId: number | null): (string | number)[] => ['/cliente', ownerId ?? 'x', 'citas'];

/** El área del cliente solo muestra mascotas del dueño de la ruta; las demás áreas ven cualquiera. */
export const belongsToArea = (area: Pick<PetAreaConfig, 'ownerScoped'>, pet: Pick<Pet, 'ownerId'>, ownerId: number | null): boolean =>
  !area.ownerScoped || pet.ownerId === ownerId;
