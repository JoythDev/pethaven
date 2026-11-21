// Periodos del panorama del administrador (demo, S32): ocupación y desgloses son demo;
// en «Todo», ventas y ganancia son los totales reales del API.
export type DashboardPeriodId = 'month' | 'quarter' | 'year' | 'all';

export interface PeriodMetrics {
  // Pesos colombianos sin decimales.
  sales: number;
  profit: number;
  // Porcentaje entero.
  occupancy: number;
}

export interface PeriodChange {
  // Variación porcentual frente al periodo anterior.
  salesPct: number;
  profitPct: number;
  // Diferencia en puntos porcentuales.
  occupancyPoints: number;
}

export interface PeriodSeries {
  labels: string[];
  sales: number[];
  profit: number[];
  occupancy: number[];
}

export interface DashboardPeriod {
  id: DashboardPeriodId;
  label: string;
  metrics: PeriodMetrics;
  // Null en «Todo»: no hay periodo anterior con el que comparar.
  previous: PeriodMetrics | null;
  change: PeriodChange | null;
  series: PeriodSeries;
}

export interface DashboardTotals {
  totalSales: number;
  grossProfit: number;
}
