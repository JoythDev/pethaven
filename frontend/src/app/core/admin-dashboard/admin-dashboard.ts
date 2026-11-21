import { formatDate } from '@angular/common';

import { Drug } from '../../models/drug.model';
import { DashboardPeriod, DashboardPeriodId } from '../../models/demo/dashboard-period.model';
import { LOW_STOCK_THRESHOLD, formatCurrency, getStockStatus } from '../../pages/shared/drugs/drug-stock';

// Lógica pura del Panorama del administrador (11.6): periodo en la URL, alertas de inventario,
// porcentajes, anchos de barra y series de la gráfica.

const LOCALE = 'es-CO';
const MILLION = 1_000_000;
const THOUSAND = 1_000;
const MIN_BAR_WIDTH = 4;
const API_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;

// --- Periodo (S32) ---------------------------------------------------------------------------

export const DEFAULT_PERIOD: DashboardPeriodId = 'month';

export const PERIOD_OPTIONS: { label: string; value: DashboardPeriodId }[] = [
  { label: 'Mes', value: 'month' },
  { label: 'Trimestre', value: 'quarter' },
  { label: 'Año', value: 'year' },
  { label: 'Todo', value: 'all' },
];

// Valor de `?periodo=` para cada periodo; sin tilde ni eñe para que la URL sea estable.
const PERIOD_PARAM: Readonly<Record<DashboardPeriodId, string>> = {
  month: 'mes',
  quarter: 'trimestre',
  year: 'anio',
  all: 'todo',
};

/** Periodo que dice la URL; lo ausente o desconocido cae en el mes. */
export function parsePeriodParam(raw: string | null): DashboardPeriodId {
  return PERIOD_OPTIONS.find((option) => PERIOD_PARAM[option.value] === raw)?.value ?? DEFAULT_PERIOD;
}

/** Parámetros de la URL del periodo: el valor por defecto no se escribe. */
export function periodQueryParams(period: DashboardPeriodId): Record<string, string> {
  return period === DEFAULT_PERIOD ? {} : { periodo: PERIOD_PARAM[period] };
}

// --- Inventario (L61: 0 agotado, 1–5 bajo, 6 o más disponible) -------------------------------

/** Primera cantidad fuera de «Stock bajo»: la marca del medidor de existencias. */
export const STOCK_MINIMUM = LOW_STOCK_THRESHOLD + 1;

export interface InventoryAlert {
  drug: Drug;
  level: 'out' | 'low';
  /** Filtro de la lista de medicamentos que deja a este a la vista. */
  /** Selecciona el medicamento en el inventario (`?seleccion=`) y abre su panel. */
  queryParams: { seleccion: number };
}

export interface InventoryAlerts {
  rows: InventoryAlert[];
  /** Total de medicamentos agotados o con stock bajo, aunque `rows` esté recortada. */
  total: number;
  outCount: number;
  lowCount: number;
}

const byName = (a: Drug, b: Drug): number => a.name.localeCompare(b.name, LOCALE);

/** Agotados por nombre y luego los de stock bajo, de menos a más unidades. */
export function inventoryAlerts(drugs: readonly Drug[], limit = Number.POSITIVE_INFINITY): InventoryAlerts {
  const out = drugs.filter((drug) => getStockStatus(drug.unitsAvailable) === 'OUT').sort(byName);
  const low = drugs
    .filter((drug) => getStockStatus(drug.unitsAvailable) === 'LOW')
    .sort((a, b) => a.unitsAvailable - b.unitsAvailable || byName(a, b));
  const toRow = (drug: Drug, level: 'out' | 'low'): InventoryAlert => ({ drug, level, queryParams: { seleccion: drug.id } });
  return {
    rows: [...out.map((drug) => toRow(drug, 'out')), ...low.map((drug) => toRow(drug, 'low'))].slice(0, limit),
    total: out.length + low.length,
    outCount: out.length,
    lowCount: low.length,
  };
}

// --- Porcentajes y barras --------------------------------------------------------------------

/** Porcentaje entero de `part` sobre `total`, entre 0 y 100; 0 si no hay total. */
export function share(part: number, total: number): number {
  if (part <= 0 || total <= 0) {
    return 0;
  }
  return Math.min(100, Math.round((part / total) * 100));
}

/** Ancho (%) de cada barra frente a la mayor; un valor pequeño pero no cero conserva un mínimo visible. */
export function relativeWidths(values: readonly number[]): number[] {
  const max = Math.max(...values, 0);
  return values.map((value) => (max <= 0 || value <= 0 ? 0 : Math.max(MIN_BAR_WIDTH, (value / max) * 100)));
}

/** Primeros `limit` elementos, o todos si está expandida; `hidden` es cuántos faltan por mostrar. */
export function limitList<T>(items: readonly T[], limit: number, expanded: boolean): { items: T[]; hidden: number } {
  if (expanded || items.length <= limit) {
    return { items: [...items], hidden: 0 };
  }
  return { items: items.slice(0, limit), hidden: items.length - limit };
}

export interface Trend {
  text: string;
  direction: 'up' | 'down' | 'flat';
}

/** Variación porcentual redactada con espacio antes del signo (12.2); null si no hay con qué comparar. */
export function trendOf(percent: number | null): Trend | null {
  if (percent === null) {
    return null;
  }
  if (percent > 0) {
    return { text: `+${percent} %`, direction: 'up' };
  }
  return { text: `${percent} %`, direction: percent < 0 ? 'down' : 'flat' };
}

// --- Dinero (12.2) ---------------------------------------------------------------------------

const decimalFormatter = new Intl.NumberFormat(LOCALE, { maximumFractionDigits: 1 });

/** «$18,4 M» desde un millón; por debajo, la moneda completa («$14.500»). */
export function compactCurrency(value: number): string {
  return value >= MILLION ? `$${decimalFormatter.format(value / MILLION)} M` : formatCurrency(value);
}

export interface MoneyUnit {
  divisor: number;
  label: string;
}

/** Unidad del eje de la gráfica según el valor más alto. */
export function chartMoneyUnit(max: number): MoneyUnit {
  if (max >= MILLION) {
    return { divisor: MILLION, label: 'Millones de pesos' };
  }
  return max >= THOUSAND * 10
    ? { divisor: THOUSAND, label: 'Miles de pesos' }
    : { divisor: 1, label: 'Pesos' };
}

export interface SalesChartSeries {
  labels: string[];
  sales: number[];
  profit: number[];
  unit: MoneyUnit;
}

const roundTo2 = (value: number): number => Math.round(value * 100) / 100;

/** Series de ventas y ganancia del periodo en la unidad del eje. */
export function salesChartSeries(period: DashboardPeriod): SalesChartSeries {
  const { labels, sales, profit } = period.series;
  const unit = chartMoneyUnit(Math.max(...sales, ...profit, 0));
  const convert = (values: number[]): number[] => values.map((value) => roundTo2(value / unit.divisor));
  return { labels: [...labels], sales: convert(sales), profit: convert(profit), unit };
}

/** Texto para lectores de pantalla de la gráfica: cada punto con ventas y ganancia. */
export function salesChartSummary(period: DashboardPeriod): string {
  const { labels, sales, profit } = period.series;
  const points = labels.map(
    (label, i) => `${label}: ventas ${compactCurrency(sales[i])}, ganancia ${compactCurrency(profit[i])}`,
  );
  return `Ventas y ganancia, periodo ${period.label}. ${points.join('; ')}.`;
}

// --- Periodo mensual del API -----------------------------------------------------------------

export interface MonthlyPeriod {
  monthlyPeriodStart: string;
  monthlyPeriodEndExclusive: string;
  monthlyPeriodTimeZone: string;
}

function parseApiDate(value: string): Date | null {
  const match = API_DATE.exec(value);
  if (!match) {
    return null;
  }
  const [year, month, day] = [Number(match[1]), Number(match[2]), Number(match[3])];
  const date = new Date(Date.UTC(year, month - 1, day));
  const valid = date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
  return valid ? date : null;
}

/** «19 oct – 17 nov 2026 · America/Bogota»: el fin del API es exclusivo, así que se resta un día. */
export function formatPeriodRange(period: MonthlyPeriod): string {
  const start = parseApiDate(period.monthlyPeriodStart);
  const endExclusive = parseApiDate(period.monthlyPeriodEndExclusive);
  if (!start || !endExclusive) {
    return `${period.monthlyPeriodStart} – ${period.monthlyPeriodEndExclusive} · ${period.monthlyPeriodTimeZone}`;
  }
  endExclusive.setUTCDate(endExclusive.getUTCDate() - 1);
  const from = formatDate(start, 'd MMM', LOCALE, 'UTC');
  const to = formatDate(endExclusive, 'd MMM y', LOCALE, 'UTC');
  return `${from} – ${to} · ${period.monthlyPeriodTimeZone}`;
}

/** Mes en curso y mes anterior en minúscula («noviembre», «octubre»), para los textos de la barra lateral. */
export function monthLabels(now: Date): { current: string; previous: string } {
  const previous = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  return {
    current: formatDate(new Date(now.getFullYear(), now.getMonth(), 1), 'LLLL', LOCALE),
    previous: formatDate(previous, 'LLLL', LOCALE),
  };
}
