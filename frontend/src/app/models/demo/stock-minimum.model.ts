// Mínimo de existencias (demo, S3): el API aún no guarda un mínimo por medicamento.
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
