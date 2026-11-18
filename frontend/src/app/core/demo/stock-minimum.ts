import { DrugStockInfo, StockAlerts } from '../../models/demo/stock-minimum.model';
import { Drug } from '../../models/drug.model';
import { LOW_STOCK_THRESHOLD } from '../../pages/shared/drugs/drug-stock';
import { stockLevel } from '../status/stock-level';

// Mínimo de existencias de un medicamento (L61): el API aún no guarda uno configurable, así que todas las
// pantallas usan la regla real de «Stock bajo» (hasta 5 unidades). El mínimo es la primera cantidad fuera de ella.
export function demoMinimumStock(_drug: Drug): number {
  return LOW_STOCK_THRESHOLD + 1;
}

export function stockStatuses(drugs: Drug[]): DrugStockInfo[] {
  return drugs.map((drug) => {
    const minimum = demoMinimumStock(drug);
    return {
      drug,
      minimum,
      level: stockLevel(drug.unitsAvailable, minimum),
      deficit: Math.max(0, minimum - drug.unitsAvailable),
    };
  });
}

const byName = (a: DrugStockInfo, b: DrugStockInfo): number => a.drug.name.localeCompare(b.drug.name, 'es');

// Agotados por nombre; con stock bajo, de mayor a menor faltante.
export function stockAlerts(drugs: Drug[]): StockAlerts {
  const infos = stockStatuses(drugs);
  return {
    out: infos.filter((i) => i.level === 'out').sort(byName),
    low: infos.filter((i) => i.level === 'low').sort((a, b) => b.deficit - a.deficit || byName(a, b)),
  };
}
