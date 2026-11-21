// Existencias con la regla real (L61): el API aún no guarda un mínimo configurable por medicamento, así que
// `minimum` es el umbral común de «Stock bajo» más uno.
import { StockLevel } from '../../core/status/stock-level';
import { Drug } from '../drug.model';

export interface DrugStockInfo {
  drug: Drug;
  minimum: number;
  level: StockLevel;
  // Unidades que faltan para llegar al mínimo (0 si ya lo alcanza).
  deficit: number;
}

export interface StockAlerts {
  out: DrugStockInfo[];
  low: DrugStockInfo[];
}
