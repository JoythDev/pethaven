import { Drug } from '../../../models/drug.model';

// Hasta este número de unidades (incluido) el medicamento se considera con stock bajo.
export const LOW_STOCK_THRESHOLD = 10;

export type DrugStockStatus = 'OUT' | 'LOW' | 'AVAILABLE';

type TagSeverity = 'success' | 'warn' | 'danger';

interface DrugStockInfo {
  label: string;
  severity: TagSeverity;
}

const STOCK_INFO: Record<DrugStockStatus, DrugStockInfo> = {
  OUT: { label: 'Agotado', severity: 'danger' },
  LOW: { label: 'Stock bajo', severity: 'warn' },
  AVAILABLE: { label: 'Disponible', severity: 'success' },
};

// Opciones del filtro de estado de stock, en el mismo orden en que se muestran.
export const STOCK_FILTER_OPTIONS: { label: string; value: DrugStockStatus }[] = [
  { label: STOCK_INFO.AVAILABLE.label, value: 'AVAILABLE' },
  { label: STOCK_INFO.LOW.label, value: 'LOW' },
  { label: STOCK_INFO.OUT.label, value: 'OUT' },
];

// 0 unidades = agotado; hasta LOW_STOCK_THRESHOLD = stock bajo; más = disponible.
export const getStockStatus = (unitsAvailable: number): DrugStockStatus => {
  if (unitsAvailable <= 0) {
    return 'OUT';
  }
  return unitsAvailable <= LOW_STOCK_THRESHOLD ? 'LOW' : 'AVAILABLE';
};

export const getStockInfo = (unitsAvailable: number): DrugStockInfo => STOCK_INFO[getStockStatus(unitsAvailable)];

// Margen bruto por unidad: precio de venta menos precio de compra.
export const getMargin = (drug: Drug): number => drug.salePrice - drug.purchasePrice;

// Margen como porcentaje del precio de venta. Con precio de venta 0 no hay porcentaje (null).
export const getMarginPercent = (drug: Drug): number | null =>
  drug.salePrice > 0 ? (getMargin(drug) / drug.salePrice) * 100 : null;

const currencyFormatter = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

const integerFormatter = new Intl.NumberFormat('es-CO');

const percentFormatter = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 });

export const formatCurrency = (value: number): string => currencyFormatter.format(value);

export const formatUnits = (value: number): string => integerFormatter.format(value);

export const formatPercent = (value: number | null): string =>
  value === null ? '—' : `${percentFormatter.format(value)} %`;
