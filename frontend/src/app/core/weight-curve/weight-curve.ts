import { WeightAxis, WeightCurve, WeightPoint, WeightRecordResponse } from '../../models/weight.model';
import { addMonths } from '../time/calendar';

// Lógica pura de la curva de peso (S12.3): un punto por pesaje en su fecha real dentro del tramo.

/** Pesajes necesarios en el tramo para dibujar una curva. */
export const MIN_CURVE_POINTS = 2;

// Una fecha sin zona del API ('2026-10-12T10:30:15') se interpreta en hora local, que es la de la clínica.
// Se recorta a segundos: el API puede enviar microsegundos, que no todos los navegadores leen.
export const toWeightPoints = (records: WeightRecordResponse[]): WeightPoint[] =>
  records.map((record) => ({ date: new Date(record.date.slice(0, 19)), weight: record.weight }));

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

/**
 * Curva de los últimos `months` meses hasta `today`, con los pesajes ya ordenados del más antiguo al más
 * reciente. El peso anterior al tramo se conserva para comparar el primer punto. Null si en el tramo hay
 * menos de {@link MIN_CURVE_POINTS} pesajes: la vista muestra entonces un estado vacío.
 */
export function weightCurve(history: WeightPoint[], months: number, today: Date): WeightCurve | null {
  const from = addMonths(today, -months);
  // Sin límite superior: un pesaje recién guardado con el reloj del servidor puede ir un poco por delante del navegador.
  const points = history.filter((point) => point.date >= from);
  if (points.length < MIN_CURVE_POINTS) {
    return null;
  }
  const last = points[points.length - 1].date;
  const before = history.filter((point) => point.date < from);
  const weights = points.map((point) => point.weight);
  const min = Math.min(...weights);
  const max = Math.max(...weights);
  return {
    points,
    from,
    to: last > today ? last : today,
    min,
    max,
    axis: axisFor(min, max),
    previous: before.length > 0 ? before[before.length - 1].weight : null,
  };
}
