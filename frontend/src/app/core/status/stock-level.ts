export type StockLevel = 'ok' | 'low' | 'out';

// En cero está agotado; por debajo del mínimo, bajo; desde el mínimo, disponible.
export function stockLevel(units: number, minimum: number): StockLevel {
  if (units <= 0) {
    return 'out';
  }
  return units < minimum ? 'low' : 'ok';
}

export interface StockMeterScale {
  // Porcentajes del ancho de la barra.
  fill: number;
  tick: number;
  // Tope de la escala en unidades (para aria-valuemax).
  max: number;
}

// Sin máximo explícito, la escala es el triple del mínimo o las unidades, lo que sea mayor.
export function stockMeterScale(units: number, minimum: number, max?: number): StockMeterScale {
  const scale = max ?? Math.max(minimum * 3, units);
  if (scale <= 0) {
    return { fill: 0, tick: 0, max: 0 };
  }
  return {
    fill: Math.min(100, (Math.max(0, units) / scale) * 100),
    tick: Math.min(98, (minimum / scale) * 100),
    max: scale,
  };
}
