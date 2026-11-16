import { WeightAxis, WeightCurve, WeightPoint } from '../../models/demo/weight-curve.model';
import { PET_WEIGHT_MIN, Pet, Species } from '../../models/pet.model';
import { addMonths } from './demo-date';
import { seeded, seededInt } from './seeded';

const MIN_POINTS = 6;
const EXTRA_POINTS = 3; // 6 a 8 puntos
// Variación mensual máxima de un adulto, por especie.
const VOLATILITY: Record<Species, number> = { [Species.DOG]: 0.03, [Species.CAT]: 0.02 };

const round1 = (value: number): number => Math.round(value * 10) / 10;

// Eje con valores redondos que contiene todos los puntos.
function axisFor(min: number, max: number): WeightAxis {
  const range = max - min;
  const step = range <= 3 ? 0.5 : range <= 8 ? 1 : range <= 20 ? 2 : 5;
  return {
    min: Math.max(0, Math.floor((min - step * 0.5) / step) * step),
    max: Math.ceil((max + step * 0.5) / step) * step,
    step,
  };
}

// Curva mensual que termina hoy con el peso real de la mascota. Hacia atrás parte de ese peso:
// los adultos varían poco; los menores de dos años pesaban menos cuanto más atrás.
export function generateWeightCurve(pet: Pet, today: Date): WeightCurve {
  const count = MIN_POINTS + seededInt(pet.id, 'weight-count', EXTRA_POINTS);
  const growing = pet.age <= 1;
  const next = seeded(pet.id, 'weight-walk');
  const weights = new Array<number>(count);
  weights[count - 1] = pet.weight;
  let current = pet.weight;
  for (let i = count - 2; i >= 0; i--) {
    current = growing
      ? current * (1 - (0.05 + next() * 0.05))
      : current * (1 + (next() * 2 - 1) * VOLATILITY[pet.species]);
    weights[i] = Math.max(PET_WEIGHT_MIN, round1(current));
  }
  const points: WeightPoint[] = weights.map((weight, i) => ({
    date: i === count - 1 ? new Date(today) : addMonths(today, i - (count - 1)),
    weight,
  }));
  const min = Math.min(...weights);
  const max = Math.max(...weights);
  return {
    petId: pet.id,
    points,
    min,
    max,
    axis: axisFor(min, max),
    current: pet.weight,
    change: pet.weight - weights[count - 2],
  };
}
