import { DrugStockInfo, StockAlerts } from '../../models/demo/stock-minimum.model';
import { Drug } from '../../models/drug.model';
import { stockLevel } from '../status/stock-level';
import { seededInt } from './seeded';

const MIN_FLOOR = 8;
const MIN_SPREAD = 18; // mínimos de 8 a 25 unidades

// Mínimo demo de un medicamento; el API aún no guarda uno, así que sale de su id.
export function demoMinimumStock(drug: Drug): number {
  return MIN_FLOOR + seededInt(drug.id, 'min-stock', MIN_SPREAD);
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

// Agotados por nombre; bajo el mínimo, de mayor a menor faltante.
export function stockAlerts(drugs: Drug[]): StockAlerts {
  const infos = stockStatuses(drugs);
  return {
    out: infos.filter((i) => i.level === 'out').sort(byName),
    low: infos.filter((i) => i.level === 'low').sort((a, b) => b.deficit - a.deficit || byName(a, b)),
  };
}
