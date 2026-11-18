import { Appointment } from '../../models/demo/appointment.model';
import { CareItem } from '../../models/demo/care-plan.model';
import { DashboardPeriod } from '../../models/demo/dashboard-period.model';
import { DemoNotification } from '../../models/demo/demo-notification.model';
import { Drug } from '../../models/drug.model';
import { Pet } from '../../models/pet.model';
import { Veterinarian } from '../../models/veterinarian.model';
import { formatTime, addDays, atTime, dayNumber, isSameDay, startOfDay } from './demo-date';
import { seededInt } from './seeded';
import { stockAlerts } from './stock-minimum';
import { generateVaccineCard } from './vaccines';

export interface ClientNotificationInput {
  today: Date;
  ownerId: number;
  pets: Pet[];
  // Próximas citas de sus mascotas, de la más cercana a la más lejana.
  upcoming: Appointment[];
  care: { pet: Pet; items: CareItem[] }[];
  doneCareIds: ReadonlySet<string>;
}

export interface VeterinarianNotificationInput {
  today: Date;
  // Citas de hoy del veterinario.
  todayAppointments: Appointment[];
  pets: Pet[];
  drugs: Drug[];
}

export interface AdministratorNotificationInput {
  today: Date;
  drugs: Drug[];
  veterinarians: Veterinarian[];
  periods: DashboardPeriod[];
}

const WEEKDAYS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];
const MAX_LISTED_NAMES = 3;
const MAX_CLIENT_ITEMS = 8;

interface Draft {
  id: string;
  icon: string;
  title: string;
  detail: string;
  at: Date;
  route: string | null;
  // Por defecto, solo las de hoy quedan sin leer.
  unread?: boolean;
}

// Un aviso de hoy, unos minutos antes de ahora y sin cruzar la medianoche.
function earlierToday(today: Date, minutes: number): Date {
  return new Date(Math.max(startOfDay(today).getTime(), today.getTime() - minutes * 60_000));
}

// Un aviso de hace `days` días, a la hora indicada.
function daysBefore(today: Date, days: number, time: string): Date {
  return atTime(addDays(today, -days), time);
}

function minutesFor(id: string, min: number, spread: number): number {
  return min + seededInt(id, 'notification-minutes', spread);
}

function finish(drafts: Draft[], today: Date): DemoNotification[] {
  return drafts
    .map((draft): DemoNotification => {
      const group = isSameDay(draft.at, today) ? 'Hoy' : 'Esta semana';
      return {
        id: draft.id,
        icon: draft.icon,
        title: draft.title,
        detail: draft.detail,
        at: draft.at,
        group,
        unread: draft.unread ?? group === 'Hoy',
        route: draft.route,
      };
    })
    .sort((a, b) => b.at.getTime() - a.at.getTime() || a.id.localeCompare(b.id));
}

function namesList(names: string[]): string {
  const shown = names.slice(0, MAX_LISTED_NAMES).join(', ');
  const rest = names.length - MAX_LISTED_NAMES;
  return rest > 0 ? `${shown} y ${rest} más.` : `${shown}.`;
}

function plural(count: number, one: string, many: string): string {
  return `${count} ${count === 1 ? one : many}`;
}

function whenPhrase(start: Date, today: Date): string {
  const time = formatTime(start);
  const days = dayNumber(start) - dayNumber(today);
  if (days === 0) {
    return `hoy a las ${time}`;
  }
  return days === 1 ? `mañana a las ${time}` : `el ${WEEKDAYS[start.getDay()]} ${start.getDate()} a las ${time}`;
}

export function clientNotifications(input: ClientNotificationInput): DemoNotification[] {
  const { today, ownerId, pets, upcoming, care, doneCareIds } = input;
  const drafts: Draft[] = [];

  const next = upcoming[0];
  if (next) {
    drafts.push({
      id: `ntf-cli-appointment-${next.id}`,
      icon: 'calendar',
      title: `Cita de ${next.petName} ${whenPhrase(next.start, today)}`,
      detail: `${next.reason} con ${next.veterinarianName}.`,
      at: earlierToday(today, minutesFor(next.id, 30, 180)),
      route: `/cliente/${ownerId}/citas`,
    });
  }

  for (const { pet, items } of care) {
    const pending = items.filter((item) => !doneCareIds.has(item.id));
    if (!pending.length) {
      continue;
    }
    drafts.push({
      id: `ntf-cli-care-${pet.id}`,
      icon: 'pill',
      title:
        pending.length === 1
          ? `Queda 1 cuidado de ${pet.name} por marcar`
          : `Quedan ${pending.length} cuidados de ${pet.name} por marcar`,
      detail: `El siguiente es ${pending[0].title.toLowerCase()} a las ${pending[0].time}.`,
      at: earlierToday(today, minutesFor(`care-${pet.id}`, 60, 240)),
      route: `/cliente/${ownerId}/mascotas/${pet.id}`,
    });
  }

  const vaccineDrafts = pets
    .flatMap((pet) => {
      const due = generateVaccineCard(pet, today)
        .vaccines.filter((v) => v.status !== 'ok')
        .sort((a, b) => a.daysUntilDue - b.daysUntilDue);
      return due.length ? [{ pet, vaccine: due[0], others: due.length - 1 }] : [];
    })
    .sort((a, b) => a.vaccine.daysUntilDue - b.vaccine.daysUntilDue)
    .slice(0, 3);
  for (const { pet, vaccine, others } of vaccineDrafts) {
    const days = vaccine.daysUntilDue;
    const title =
      vaccine.status === 'overdue'
        ? `La vacuna de ${vaccine.name} de ${pet.name} está vencida`
        : days === 0
          ? `La vacuna de ${vaccine.name} de ${pet.name} vence hoy`
          : `La vacuna de ${vaccine.name} de ${pet.name} vence en ${plural(days, 'día', 'días')}`;
    const action = vaccine.status === 'overdue' ? 'Agenda el refuerzo cuanto antes.' : 'Agenda el refuerzo antes de que venza.';
    drafts.push({
      id: `ntf-cli-vaccine-${pet.id}`,
      icon: 'syringe',
      title,
      detail: others > 0 ? `${action} Hay ${plural(others, 'vacuna', 'vacunas')} más por revisar.` : action,
      at: daysBefore(today, 1, `${String(8 + seededInt(pet.id, 'vaccine-hour', 9)).padStart(2, '0')}:00`),
      route: `/cliente/${ownerId}/mascotas/${pet.id}`,
    });
  }

  return finish(drafts, today).slice(0, MAX_CLIENT_ITEMS);
}

export function veterinarianNotifications(input: VeterinarianNotificationInput): DemoNotification[] {
  const { today, todayAppointments, pets, drugs } = input;
  const drafts: Draft[] = [];
  const day = [...todayAppointments].sort((a, b) => a.start.getTime() - b.start.getTime());

  if (day.length) {
    const first = formatTime(day[0].start);
    const last = formatTime(day[day.length - 1].start);
    drafts.push({
      id: 'ntf-vet-agenda',
      icon: 'calendar',
      title: `Tienes ${plural(day.length, 'cita', 'citas')} hoy`,
      detail: day.length === 1 ? `Es a las ${first}.` : `La primera es a las ${first} y la última a las ${last}.`,
      at: earlierToday(today, 240),
      route: '/veterinario/agenda',
    });
  }

  for (const appointment of day.filter((a) => a.urgent)) {
    drafts.push({
      id: `ntf-vet-urgency-${appointment.id}`,
      icon: 'triangle-alert',
      title: `Urgencia agendada a las ${formatTime(appointment.start)}`,
      detail: `${appointment.petName}. Dueño: ${appointment.ownerName}.`,
      at: earlierToday(today, minutesFor(appointment.id, 20, 90)),
      route: '/veterinario/agenda',
    });
  }

  const { out } = stockAlerts(drugs);
  if (out.length) {
    drafts.push({
      id: 'ntf-vet-out-of-stock',
      icon: 'package-x',
      title: out.length === 1 ? `${out[0].drug.name} está agotado` : `${out.length} medicamentos agotados`,
      detail:
        out.length === 1
          ? 'No se puede recetar hasta que se reponga.'
          : namesList(out.map((i) => i.drug.name)),
      at: earlierToday(today, 150),
      route: '/veterinario/medicamentos',
    });
  }

  const checkups = pets
    .filter((pet) => pet.active && pet.disease?.trim())
    .sort((a, b) => seededInt(a.id, 'checkup', 1000) - seededInt(b.id, 'checkup', 1000))
    .slice(0, 2);
  for (const pet of checkups) {
    drafts.push({
      id: `ntf-vet-checkup-${pet.id}`,
      icon: 'clipboard-check',
      title: `${pet.name} tiene control pendiente`,
      detail: `Han pasado ${10 + seededInt(pet.id, 'checkup-days', 12)} días desde su último tratamiento.`,
      at: daysBefore(today, 1, '16:30'),
      route: `/veterinario/mascotas/${pet.id}`,
    });
  }

  return finish(drafts, today);
}

export function administratorNotifications(input: AdministratorNotificationInput): DemoNotification[] {
  const { today, drugs, veterinarians, periods } = input;
  const drafts: Draft[] = [];
  const { out, low } = stockAlerts(drugs);

  if (out.length) {
    drafts.push({
      id: 'ntf-adm-out-of-stock',
      icon: 'package-x',
      title: `${plural(out.length, 'medicamento agotado', 'medicamentos agotados')}`,
      detail: namesList(out.map((i) => i.drug.name)),
      at: earlierToday(today, 190),
      route: '/administrador/medicamentos',
    });
  }
  if (low.length) {
    drafts.push({
      id: 'ntf-adm-low-stock',
      icon: 'package-minus',
      title: `${low.length} ${low.length === 1 ? 'medicamento con stock bajo' : 'medicamentos con stock bajo'}`,
      detail: namesList(low.map((i) => i.drug.name)),
      at: earlierToday(today, 185),
      route: '/administrador/medicamentos',
    });
  }

  const month = periods.find((p) => p.id === 'month');
  if (month?.change) {
    const pct = month.change.salesPct;
    drafts.push({
      id: 'ntf-adm-sales-trend',
      icon: pct >= 0 ? 'trending-up' : 'trending-down',
      title: pct === 0 ? 'Las ventas del mes se mantienen' : `Las ventas del mes ${pct > 0 ? 'suben' : 'bajan'} ${Math.abs(pct)} %`,
      detail: `Frente al periodo anterior. La ocupación del mes es de ${month.metrics.occupancy} %.`,
      at: daysBefore(today, 1, '18:00'),
      route: '/administrador/dashboard',
    });
  }

  const inactive = veterinarians.filter((v) => !v.active);
  if (inactive.length) {
    drafts.push({
      id: 'ntf-adm-inactive-vets',
      icon: 'user-x',
      title:
        inactive.length === 1
          ? `${inactive[0].name} sigue con la cuenta inactiva`
          : `${inactive.length} veterinarios con la cuenta inactiva`,
      detail: 'No reciben pacientes mientras sigan inactivos.',
      at: daysBefore(today, 3, '09:30'),
      route: '/administrador/veterinarios',
    });
  }

  if (drugs.length) {
    drafts.push({
      id: 'ntf-adm-import',
      icon: 'file-up',
      title: 'Importación de Excel completada',
      detail: `${plural(drugs.length, 'medicamento procesado', 'medicamentos procesados')}, 0 filas con errores.`,
      at: daysBefore(today, 4 + seededInt(drugs.length, 'import-days', 3), '11:15'),
      route: '/administrador/medicamentos/import',
      unread: false,
    });
  }

  return finish(drafts, today);
}
