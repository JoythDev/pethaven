import { formatDate, formatNumber } from '@angular/common';

import { WeightCurve } from '../../../models/weight.model';

// Textos de la curva de peso. Un solo lugar para los formatos (12.2: coma decimal, meses en español).
const LOCALE = 'es-CO';

const kg = (value: number): string => `${formatNumber(value, LOCALE, '1.0-1')} kg`;

/** «12 oct»: etiqueta del eje horizontal. */
export const axisLabel = (date: Date | number): string => formatDate(date, 'd MMM', LOCALE).replace('.', '');

/** «sube 0,3 kg», «baja 0,4 kg» o «sin cambios». */
export function changeText(delta: number): string {
  const rounded = Math.round(delta * 10) / 10;
  if (rounded === 0) {
    return 'sin cambios';
  }
  return `${rounded > 0 ? 'sube' : 'baja'} ${kg(Math.abs(rounded))}`;
}

/** «12 de julio de 2026 · sube 0,3 kg frente al registro anterior»; solo el primer registro no se compara. */
export function pointCaption(curve: WeightCurve, index: number): string {
  const point = curve.points[index];
  const day = formatDate(point.date, "d 'de' MMMM 'de' y", LOCALE);
  const before = index === 0 ? curve.previous : curve.points[index - 1].weight;
  if (before === null) {
    return `${day} · primer registro`;
  }
  return `${day} · ${changeText(point.weight - before)} frente al registro anterior`;
}

/** Texto alternativo del gráfico: el recorrido, el mínimo y el máximo. */
export function chartSummary(curve: WeightCurve, petName: string): string {
  const first = curve.points[0];
  const last = curve.points[curve.points.length - 1];
  const day = (date: Date) => formatDate(date, "d 'de' MMMM 'de' y", LOCALE);
  return (
    `Curva de peso de ${petName}: de ${kg(first.weight)} en ${day(first.date)} a ${kg(last.weight)} en ${day(last.date)}; ` +
    `mínimo ${kg(curve.min)}, máximo ${kg(curve.max)}.`
  );
}
