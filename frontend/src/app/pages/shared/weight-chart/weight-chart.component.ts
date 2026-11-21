import { formatDate, formatNumber } from '@angular/common';
import { Component, computed, input, linkedSignal, signal } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';

import { themeChartColors } from '../../../core/charts/chart-colors';
import { WeightCurve } from '../../../models/demo/weight-curve.model';
import { chartSummary, monthLabel, pointCaption } from './weight-chart';

const LOCALE = 'es-CO';
const FONT = '"Nunito Sans", sans-serif';
const REDUCED_MOTION = '(prefers-reduced-motion: reduce)';

/**
 * Curva de peso (10.18): línea con área, valores en el eje vertical (S24), punto elegido más grande
 * con su valor y variación arriba, y «Ver como tabla» como alternativa de texto. Los colores salen de
 * las variables CSS y se vuelven a leer al cambiar el tema. Chart.js se carga en diferido al pintar el
 * primer gráfico, así que no entra en el paquete inicial. La tarjeta (título, etiqueta Demo) la pone
 * quien lo usa.
 */
@Component({
  selector: 'app-weight-chart',
  imports: [ButtonModule, ChartModule],
  templateUrl: './weight-chart.component.html',
  host: { class: 'block' },
})
export class WeightChartComponent {
  readonly curve = input.required<WeightCurve>();
  /** Nombre de la mascota, para el texto alternativo. */
  readonly petName = input.required<string>();

  protected readonly showTable = signal(false);
  // Punto elegido: el último (hoy) hasta que se pulsa otro; una curva nueva vuelve al último.
  protected readonly selected = linkedSignal<WeightCurve, number>({
    source: this.curve,
    computation: (curve) => curve.points.length - 1,
  });
  private readonly colors = themeChartColors({ accent: '--color-accent', soft: '--color-accent-soft' });
  private readonly reducedMotion = window.matchMedia(REDUCED_MOTION).matches;

  protected readonly rows = computed(() =>
    this.curve().points.map((point) => ({
      date: formatDate(point.date, 'd MMM y', LOCALE),
      weight: formatNumber(point.weight, LOCALE, '1.0-1'),
    })),
  );
  protected readonly current = computed(() => formatNumber(this.curve().points[this.selected()].weight, LOCALE, '1.0-1'));
  protected readonly caption = computed(() => pointCaption(this.curve(), this.selected()));
  protected readonly summary = computed(() => chartSummary(this.curve(), this.petName()));

  protected readonly data = computed(() => {
    const { points } = this.curve();
    const colors = this.colors();
    const selected = this.selected();
    return {
      labels: points.map((point) => monthLabel(point.date)),
      datasets: [
        {
          data: points.map((point) => point.weight),
          fill: true,
          borderColor: colors.accent,
          backgroundColor: colors.soft,
          borderWidth: 3,
          borderJoinStyle: 'round',
          tension: 0,
          pointRadius: points.map((_, i) => (i === selected ? 7 : 4.5)),
          pointHoverRadius: 7,
          pointBorderWidth: 2.5,
          pointBorderColor: colors.accent,
          pointBackgroundColor: points.map((_, i) => (i === selected ? colors.accent : colors.card)),
        },
      ],
    };
  });

  protected readonly options = computed(() => {
    const { points, axis } = this.curve();
    const colors = this.colors();
    const tick = { color: colors.muted, font: { family: FONT, size: 12, weight: 700 } };
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: this.reducedMotion ? false : { duration: 300 },
      interaction: { mode: 'nearest', intersect: false, axis: 'x' },
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
          displayColors: false,
          callbacks: {
            title: (items: { dataIndex: number }[]) => formatDate(points[items[0].dataIndex].date, 'd MMM y', LOCALE),
            label: (item: { parsed: { y: number } }) => `${formatNumber(item.parsed.y, LOCALE, '1.0-1')} kg`,
          },
        },
      },
      scales: {
        x: { grid: { display: false }, border: { color: colors.grid }, ticks: tick },
        y: {
          min: axis.min,
          max: axis.max,
          ticks: {
            ...tick,
            stepSize: axis.step,
            callback: (value: number | string) => formatNumber(Number(value), LOCALE, '1.0-1'),
          },
          grid: { color: colors.grid },
          border: { display: false },
          title: { display: true, text: 'Peso (kg)', color: colors.muted, font: { family: FONT, size: 12, weight: 700 } },
        },
      },
    };
  });

  protected select(event: { element?: { index?: number } }): void {
    const index = event.element?.index;
    if (index !== undefined) {
      this.selected.set(index);
    }
  }
}
