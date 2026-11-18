import { Drug } from '../../models/drug.model';
import { LOW_STOCK_THRESHOLD, DrugStockStatus, getStockStatus } from '../../pages/shared/drugs/drug-stock';
import { SortAccessors } from '../list/list-sort';
import { StockLevel } from '../status/stock-level';
import { normalizeText } from '../text/normalize-text';

/** Claves de la URL: estado de existencias y rango de precio de venta (11.16). */
export const DRUG_FILTER_KEYS = ['estado', 'min', 'max'] as const;

/** Valor de `estado` en la URL para cada nivel de existencias. */
export const DRUG_STATUS_PARAM: Readonly<Record<StockLevel, string>> = {
  ok: 'disponible',
  low: 'bajo',
  out: 'agotado',
};

/** Nivel de existencias que representa el valor de `estado`; null si no es uno conocido. */
export const drugStatusFromParam = (value: string | undefined): StockLevel | null =>
  (Object.keys(DRUG_STATUS_PARAM) as StockLevel[]).find((level) => DRUG_STATUS_PARAM[level] === value) ?? null;

/** Medicamento con el mínimo que dibuja el medidor y el nivel de existencias que sale de la regla real. */
export interface DrugRow extends Drug {
  minimum: number;
  level: StockLevel;
}

// «Stock bajo» es de 5 unidades o menos (drug-stock): el medidor pide 6 para estar fuera de ese nivel.
const MINIMUM = LOW_STOCK_THRESHOLD + 1;

const LEVEL: Record<DrugStockStatus, StockLevel> = { OUT: 'out', LOW: 'low', AVAILABLE: 'ok' };

export function buildDrugRows(drugs: readonly Drug[]): DrugRow[] {
  return drugs.map((drug) => ({ ...drug, minimum: MINIMUM, level: LEVEL[getStockStatus(drug.unitsAvailable)] }));
}

type TagSeverity = 'success' | 'warn' | 'danger';

const STATUS: Record<StockLevel, { label: string; severity: TagSeverity }> = {
  ok: { label: 'Disponible', severity: 'success' },
  low: { label: 'Stock bajo', severity: 'warn' },
  out: { label: 'Agotado', severity: 'danger' },
};

/** Etiqueta y variante del estado de existencias (12.3). */
export const drugStatus = (level: StockLevel): { label: string; severity: TagSeverity } => STATUS[level];

// Orden de «Estado»: del más urgente (agotado) al mejor.
const STATUS_RANK: Record<StockLevel, number> = { out: 0, low: 1, ok: 2 };

export const DRUG_SORT_ACCESSORS: SortAccessors<DrugRow> = {
  name: (d) => d.name,
  stock: (d) => d.unitsAvailable,
  price: (d) => d.salePrice,
  sold: (d) => d.unitsSold,
  status: (d) => STATUS_RANK[d.level],
};

/** Un límite de precio válido es un entero no negativo; cualquier otra cosa se ignora. */
export function parsePriceBound(raw: string | undefined): number | null {
  return raw !== undefined && /^\d{1,12}$/.test(raw) ? Number(raw) : null;
}

/** Deja solo los dígitos de lo que escribe la persona en un campo de precio. */
export const sanitizePriceInput = (raw: string): string => raw.replace(/\D/g, '');

export interface PriceRange {
  min: number | null;
  max: number | null;
  /** El mínimo supera al máximo: el rango se ignora por completo. */
  invalid: boolean;
}

export function priceRangeOf(filters: Readonly<Record<string, string>>): PriceRange {
  const min = parsePriceBound(filters['min']);
  const max = parsePriceBound(filters['max']);
  if (min !== null && max !== null && min > max) {
    return { min: null, max: null, invalid: true };
  }
  return { min, max, invalid: false };
}

export interface DrugCriteria {
  search: string;
  status: StockLevel | null;
  /** Rango ya resuelto con `priceRangeOf` (null = sin límite). */
  min: number | null;
  max: number | null;
}

/** Búsqueda por nombre (sin tildes ni mayúsculas), estado de existencias y rango del precio de venta. */
export function drugMatches(drug: DrugRow, criteria: DrugCriteria): boolean {
  const term = normalizeText(criteria.search);
  return (
    (!term || normalizeText(drug.name).includes(term)) &&
    (criteria.status === null || drug.level === criteria.status) &&
    (criteria.min === null || drug.salePrice >= criteria.min) &&
    (criteria.max === null || drug.salePrice <= criteria.max)
  );
}

export const drugsCountLabel = (total: number): string => `${total} ${total === 1 ? 'medicamento' : 'medicamentos'}`;
