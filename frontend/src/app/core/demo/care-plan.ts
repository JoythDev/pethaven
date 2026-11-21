import { CareItem, CareProgress, CareTreatment } from '../../models/demo/care-plan.model';
import { Pet, Species } from '../../models/pet.model';
import { dayKey } from './demo-date';
import { seededInt } from './seeded';

type CareDraft = Omit<CareItem, 'id' | 'petId'>;

const DOSE_NOTES = ['Con la comida', 'Con un poco de agua', 'Después del paseo'];
const SINGLE_DOSE_TIMES = ['08:00', '12:00', '18:00'];

// Último tratamiento con medicamentos que sigue en curso (un alta no cuenta).
export function latestActiveTreatment<T extends CareTreatment>(treatments: T[]): T | null {
  const candidates = treatments.filter((t) => t.drugs.length > 0 && t.evolution !== 'RECOVERED');
  return [...candidates].sort((a, b) => b.date.localeCompare(a.date))[0] ?? null;
}

function medicationItems(pet: Pet, treatment: CareTreatment): CareDraft[] {
  return treatment.drugs.flatMap((drug, index) => {
    const doses = 1 + seededInt(drug.drugId, `doses-${pet.id}`, 2);
    const note = DOSE_NOTES[seededInt(drug.drugId, `dose-note-${pet.id}`, DOSE_NOTES.length)];
    if (doses === 1) {
      const time = SINGLE_DOSE_TIMES[(index + seededInt(drug.drugId, `dose-time-${pet.id}`, SINGLE_DOSE_TIMES.length)) % SINGLE_DOSE_TIMES.length];
      return [{ time, title: drug.drugName, detail: `1 dosis · ${note}`, sticker: 'pill', kind: 'medication' } as CareDraft];
    }
    return ['08:00', '20:00'].map(
      (time, i): CareDraft => ({
        time,
        title: drug.drugName,
        detail: `Dosis ${i + 1} de 2 · ${note}`,
        sticker: 'pill',
        kind: 'medication',
      }),
    );
  });
}

const WATER: CareDraft = {
  time: '13:00',
  title: 'Agua fresca',
  detail: 'Cambia el agua del bebedero',
  sticker: 'water-bowl',
  kind: 'water',
};

function activityItem(pet: Pet, treated: boolean): CareDraft {
  if (pet.species === Species.CAT) {
    return treated
      ? { time: '15:00', title: 'Juego tranquilo', detail: 'Diez minutos, sin saltos', sticker: 'heart', kind: 'activity' }
      : { time: '10:00', title: 'Juego', detail: 'Diez minutos con su juguete favorito', sticker: 'heart', kind: 'activity' };
  }
  return treated
    ? { time: '15:00', title: 'Paseo corto', detail: '15 minutos, sin correr', sticker: 'leash', kind: 'activity' }
    : { time: '10:00', title: 'Paseo', detail: '30 minutos, a su ritmo', sticker: 'leash', kind: 'activity' };
}

// Plan del día de una mascota. Con diagnóstico y un tratamiento con medicamentos, parte de esos
// medicamentos reales; si no, son los cuidados generales. Una mascota inactiva no tiene plan.
export function generateCarePlan(pet: Pet, treatment: CareTreatment | null, today: Date): CareItem[] {
  if (!pet.active) {
    return [];
  }
  const active = pet.disease?.trim() && treatment && treatment.drugs.length > 0 ? treatment : null;
  const drafts: CareDraft[] = active
    ? [...medicationItems(pet, active), activityItem(pet, true), WATER]
    : [
        { time: '07:30', title: 'Desayuno', detail: 'Su porción de la mañana', sticker: 'food-bowl', kind: 'food' },
        activityItem(pet, false),
        WATER,
        { time: '18:30', title: 'Cena', detail: 'Su porción de la noche', sticker: 'food-bowl', kind: 'food' },
      ];
  const key = dayKey(today);
  return drafts
    .map((draft, order) => ({ draft, order }))
    .sort((a, b) => a.draft.time.localeCompare(b.draft.time) || a.order - b.order)
    .map(({ draft }, index) => ({ ...draft, id: `care-${pet.id}-${key}-${index}`, petId: pet.id }));
}

export function carePlanProgress(items: CareItem[], doneIds: ReadonlySet<string>): CareProgress {
  return { done: items.filter((item) => doneIds.has(item.id)).length, total: items.length };
}
