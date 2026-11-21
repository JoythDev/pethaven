import { Vaccine, VaccineCard, VaccineStatus } from '../../models/demo/vaccine.model';
import { Pet, Species } from '../../models/pet.model';
import { addDays, dayNumber, startOfDay } from './demo-date';
import { seededInt } from './seeded';

const DOG_VACCINES = ['Rabia', 'Parvovirus', 'Moquillo', 'Leptospirosis', 'Tos de las perreras'];
const CAT_VACCINES = ['Rabia', 'Triple felina', 'Leucemia felina'];

// Todas son anuales; vencen pronto cuando faltan 30 días o menos.
const VALID_DAYS = 365;
const SOON_DAYS = 30;

// Etiqueta del estado con el vocabulario de 12.3: Vigente, Vence en N días, Vencida.
export function vaccineStatusLabel(status: VaccineStatus, daysUntilDue: number): string {
  if (status === 'ok') {
    return 'Vigente';
  }
  if (status === 'overdue') {
    return 'Vencida';
  }
  if (daysUntilDue === 0) {
    return 'Vence hoy';
  }
  return daysUntilDue === 1 ? 'Vence en 1 día' : `Vence en ${daysUntilDue} días`;
}

function statusFor(daysUntilDue: number): VaccineStatus {
  if (daysUntilDue < 0) {
    return 'overdue';
  }
  return daysUntilDue <= SOON_DAYS ? 'soon' : 'ok';
}

// Días desde la última aplicación. Cerca del 60 % queda al día, 25 % próxima y 15 % vencida,
// para que el carné muestre los tres estados; una mascota menor de un año siempre está al día.
function daysSinceApplied(pet: Pet, vaccine: string): number {
  const salt = `vaccine-${vaccine}`;
  const bucket = seededInt(pet.id, salt, 100);
  if (pet.age < 1) {
    return 20 + seededInt(pet.id, `${salt}-young`, 150);
  }
  if (bucket < 60) {
    return 20 + seededInt(pet.id, `${salt}-ok`, 280);
  }
  if (bucket < 85) {
    return VALID_DAYS - SOON_DAYS + 1 + seededInt(pet.id, `${salt}-soon`, 29);
  }
  return VALID_DAYS + 1 + seededInt(pet.id, `${salt}-late`, 90);
}

export function generateVaccineCard(pet: Pet, today: Date): VaccineCard {
  const names = pet.species === Species.CAT ? CAT_VACCINES : DOG_VACCINES;
  const vaccines: Vaccine[] = names.map((name) => {
    const appliedOn = startOfDay(addDays(today, -daysSinceApplied(pet, name)));
    const nextDue = addDays(appliedOn, VALID_DAYS);
    const daysUntilDue = dayNumber(nextDue) - dayNumber(today);
    const status = statusFor(daysUntilDue);
    return { name, appliedOn, nextDue, daysUntilDue, status, label: vaccineStatusLabel(status, daysUntilDue) };
  });
  return {
    petId: pet.id,
    vaccines,
    summary: {
      total: vaccines.length,
      upToDate: vaccines.filter((v) => v.status === 'ok').length,
      soon: vaccines.filter((v) => v.status === 'soon').length,
      overdue: vaccines.filter((v) => v.status === 'overdue').length,
    },
  };
}
