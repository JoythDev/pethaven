import { Signal, effect, inject, signal, untracked } from '@angular/core';

import { ThemeService } from '../theme/theme.service';

// Variables de tema que usan todas las gráficas.
const BASE_VARS = {
  grid: '--color-line',
  muted: '--color-ink-muted',
  card: '--color-card',
  ink: '--color-ink',
} as const;

export type ChartColors<K extends string> = Record<K | keyof typeof BASE_VARS, string>;

/**
 * Chart.js pinta en un canvas y no entiende `var(--x)`: se leen los valores ya resueltos del tema (10.18).
 * `extra` asigna un nombre a cada variable propia de la gráfica; las comunes (`grid`, `muted`, `card`, `ink`) siempre van.
 */
export function readChartColors<K extends string>(extra: Record<K, string>): ChartColors<K> {
  const style = getComputedStyle(document.documentElement);
  const result: Record<string, string> = {};
  for (const [name, variable] of Object.entries({ ...BASE_VARS, ...extra })) {
    result[name] = style.getPropertyValue(variable as string).trim();
  }
  return result as ChartColors<K>;
}

export const chartColorsEqual = (a: Record<string, string>, b: Record<string, string>): boolean =>
  JSON.stringify(a) === JSON.stringify(b);

/**
 * Colores de una gráfica que se vuelven a leer al cambiar el tema. Al cambiar el tema la clase `app-dark`
 * se aplica en otro efecto: se espera a que termine para leer los colores nuevos. Va en un contexto de inyección.
 */
export function themeChartColors<K extends string>(extra: Record<K, string>): Signal<ChartColors<K>> {
  const theme = inject(ThemeService);
  const colors = signal(readChartColors(extra), { equal: chartColorsEqual });
  effect(() => {
    theme.theme();
    untracked(() => queueMicrotask(() => colors.set(readChartColors(extra))));
  });
  return colors.asReadonly();
}
