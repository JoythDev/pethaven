import { formatNumber } from '@angular/common';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';

import { compactCurrency, salesChartSeries, salesChartSummary } from '../../../core/admin-dashboard/admin-dashboard';
import { ThemeService } from '../../../core/theme/theme.service';
import { DashboardPeriod } from '../../../models/demo/dashboard-period.model';
import { formatCurrency } from '../../shared/drugs/drug-stock';

const LOCALE = 'es-CO';
const FONT = '"Nunito Sans", sans-serif';
const REDUCED_MOTION = '(prefers-reduced-motion: reduce)';

interface ChartColors {
  sales: string;
  profit: string;
  grid: string;
  muted: string;
  card: string;
  ink: string;
}

// Chart.js pinta en un canvas y no entiende `var(--x)`: se leen los valores ya resueltos del tema (10.18).
function readColors(): ChartColors {
  const style = getComputedStyle(document.documentElement);
  const read = (name: string) => style.getPropertyValue(name).trim();
  return {
    sales: read('--color-accent'),
    profit: read('--color-warning'),
    grid: read('--color-line'),
    muted: read('--color-ink-muted'),
    card: read('--color-card'),
    ink: read('--color-ink'),
  };
}

const sameColors = (a: ChartColors, b: ChartColors): boolean => JSON.stringify(a) === JSON.stringify(b);

/**
 * Barras de ventas y ganancia por tramo del periodo (10.18): leyenda con la unidad del eje, valores en
 * el eje vertical, tooltip con la cifra completa y «Ver como tabla» como alternativa de texto. Los
 * colores salen de las variables CSS y se vuelven a leer al cambiar el tema. Chart.js se carga en
 * diferido al pintar. La tarjeta (título, etiqueta Demo) la pone quien lo usa.
 */
@Component({
  selector: 'app-sales-chart',
  imports: [ButtonModule, ChartModule],
  templateUrl: './sales-chart.component.html',
  host: { class: 'block' },
})
export class SalesChartComponent {
  readonly period = input.required<DashboardPeriod>();

  private readonly theme = inject(ThemeService);

  protected readonly showTable = signal(false);
  protected readonly colors = signal(readColors(), { equal: sameColors });
  private readonly reducedMotion = window.matchMedia(REDUCED_MOTION).matches;

  protected readonly series = computed(() => salesChartSeries(this.period()));
  protected readonly summary = computed(() => salesChartSummary(this.period()));
  protected readonly unitLabel = computed(() => this.series().unit.label);
  protected readonly rows = computed(() => {
    const { labels, sales, profit } = this.period().series;
    return labels.map((label, i) => ({ label, sales: formatCurrency(sales[i]), profit: formatCurrency(profit[i]) }));
  });

  protected readonly data = computed(() => {
    const { labels, sales, profit } = this.series();
    const colors = this.colors();
    const bar = { borderRadius: 7, borderSkipped: false, maxBarThickness: 46 };
    return {
      labels,
      datasets: [
        { ...bar, label: 'Ventas', data: sales, backgroundColor: colors.sales },
        { ...bar, label: 'Ganancia', data: profit, backgroundColor: colors.profit },
      ],
    };
  });

  protected readonly options = computed(() => {
    const colors = this.colors();
    const { sales, profit } = this.period().series;
    const tick = { color: colors.muted, font: { family: FONT, size: 12, weight: 700 } };
    // La cifra completa del tooltip sale de la serie original, no de la que está en la unidad del eje.
    const raw = [sales, profit];
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: this.reducedMotion ? false : { duration: 300 },
      layout: { padding: { top: 8, right: 6 } },
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: colors.card,
          borderColor: colors.grid,
          borderWidth: 1,
          titleColor: colors.ink,
          bodyColor: colors.ink,
          titleFont: { family: FONT, size: 12, weight: 800 },
          bodyFont: { family: FONT, size: 12, weight: 700 },
          padding: 10,
          boxPadding: 4,
          callbacks: {
            label: (item: { dataset: { label?: string }; datasetIndex: number; dataIndex: number }) =>
              `${item.dataset.label}: ${compactCurrency(raw[item.datasetIndex][item.dataIndex])}`,
          },
        },
      },
      scales: {
        x: { grid: { display: false }, border: { color: colors.grid }, ticks: tick },
        y: {
          beginAtZero: true,
          ticks: {
            ...tick,
            maxTicksLimit: 5,
            callback: (value: number | string) => formatNumber(Number(value), LOCALE, '1.0-1'),
          },
          grid: { color: colors.grid },
          border: { display: false },
        },
      },
    };
  });

  constructor() {
    // Al cambiar el tema la clase `app-dark` se aplica en otro efecto: se espera a que termine para leer los colores nuevos.
    effect(() => {
      this.theme.theme();
      untracked(() => queueMicrotask(() => this.colors.set(readColors())));
    });
  }
}
