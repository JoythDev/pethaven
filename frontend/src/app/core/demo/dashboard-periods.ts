import {
  DashboardPeriod,
  DashboardPeriodId,
  DashboardTotals,
  PeriodMetrics,
  PeriodSeries,
} from '../../models/demo/dashboard-period.model';
import { SHORT_MONTHS } from './demo-date';
import { seeded } from './seeded';

const PERIOD_LABELS: Record<DashboardPeriodId, string> = {
  month: 'Mes',
  quarter: 'Trimestre',
  year: 'Año',
  all: 'Todo',
};

const clamp = (value: number, min: number, max: number): number => Math.min(max, Math.max(min, value));

// Reparte un total entre los puntos según sus pesos; el último absorbe el redondeo.
function distribute(total: number, weights: number[]): number[] {
  const sum = weights.reduce((a, b) => a + b, 0);
  const parts = weights.map((w) => Math.round((total * w) / sum));
  parts[parts.length - 1] += total - parts.reduce((a, b) => a + b, 0);
  return parts;
}

function labelsFor(id: DashboardPeriodId, today: Date): string[] {
  switch (id) {
    case 'month':
      return ['S1', 'S2', 'S3', 'S4'];
    case 'quarter':
      return [2, 1, 0].map((back) => SHORT_MONTHS[(today.getMonth() - back + 12) % 12]);
    case 'year':
      return ['T1', 'T2', 'T3', 'T4'];
    case 'all':
      return [3, 2, 1, 0].map((back) => String(today.getFullYear() - back));
  }
}

// Ventas y ganancia de «Todo» son los totales reales; el resto son fracciones estables de ellos.
// La ocupación es totalmente demo. Con ceros reales, todo queda en cero.
export function generateDashboardPeriods(totals: DashboardTotals, today: Date): DashboardPeriod[] {
  const next = seeded(Math.round(Math.abs(totals.totalSales) + Math.abs(totals.grossProfit)), 'dashboard');
  const yearFraction = 0.55 + next() * 0.15;
  const quarterFraction = yearFraction * (0.27 + next() * 0.05);
  const monthFraction = quarterFraction * (0.33 + next() * 0.04);
  const margin = totals.totalSales > 0 ? totals.grossProfit / totals.totalSales : 0;

  const fractions: Record<DashboardPeriodId, number> = {
    month: monthFraction,
    quarter: quarterFraction,
    year: yearFraction,
    all: 1,
  };

  return (['month', 'quarter', 'year', 'all'] as const).map((id): DashboardPeriod => {
    const isAll = id === 'all';
    const sales = isAll ? totals.totalSales : Math.round(totals.totalSales * fractions[id]);
    const profit = isAll
      ? totals.grossProfit
      : Math.min(sales, Math.round(sales * margin * (0.97 + next() * 0.06)));

    const labels = labelsFor(id, today);
    const trend = 0.06 + next() * 0.12;
    const salesWeights = labels.map((_, i) => 1 + i * trend + next() * 0.2);
    const profitWeights = salesWeights.map((w) => w * (0.95 + next() * 0.1));
    const base = 55 + next() * 20;
    const occupancySeries = labels.map((_, i) => Math.round(clamp(base + i * next() * 3 + (next() - 0.5) * 6, 45, 92)));
    const series: PeriodSeries = {
      labels,
      sales: distribute(sales, salesWeights),
      profit: distribute(profit, profitWeights),
      occupancy: occupancySeries,
    };
    const occupancy = Math.round(occupancySeries.reduce((a, b) => a + b, 0) / occupancySeries.length);
    const metrics: PeriodMetrics = { sales, profit, occupancy };

    if (isAll) {
      return { id, label: PERIOD_LABELS[id], metrics, previous: null, change: null, series };
    }
    const salesGrowth = -0.03 + next() * 0.21;
    const profitGrowth = -0.03 + next() * 0.21;
    const previous: PeriodMetrics = {
      sales: Math.round(sales / (1 + salesGrowth)),
      profit: Math.round(profit / (1 + profitGrowth)),
      occupancy: Math.round(clamp(occupancy - (next() * 9 - 3), 45, 95)),
    };
    const pct = (current: number, before: number): number =>
      before > 0 ? Math.round(((current - before) / before) * 100) : 0;
    return {
      id,
      label: PERIOD_LABELS[id],
      metrics,
      previous,
      change: {
        salesPct: pct(metrics.sales, previous.sales),
        profitPct: pct(metrics.profit, previous.profit),
        occupancyPoints: metrics.occupancy - previous.occupancy,
      },
      series,
    };
  });
}
