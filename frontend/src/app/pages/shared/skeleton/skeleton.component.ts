import { Component, computed, input } from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';

export type SkeletonVariant = 'text' | 'row' | 'media' | 'figure' | 'chart';

// Mismo marco que app-card, para que nada salte al llegar los datos. `row` no lleva marco:
// se apila dentro de una tarjeta, lista o tabla. `media` no lleva relleno, como la tarjeta de recurso.
const HOST_CLASS: Record<SkeletonVariant, string> = {
  text: 'relative block min-w-0 rounded-box border border-line bg-card p-4',
  row: 'relative flex min-w-0 items-center gap-3 rounded-row px-2.5 py-[9px]',
  media: 'relative block min-w-0 overflow-hidden rounded-box border border-line bg-card',
  figure: 'relative flex min-h-[128px] min-w-0 flex-col rounded-box border border-line bg-card p-4',
  chart: 'relative block min-w-0 rounded-box border border-line bg-card p-4',
};

// Anchos de las líneas de texto; la última siempre es la más corta.
const TEXT_LINES: Record<3 | 4, readonly string[]> = {
  3: ['100%', '91%', '62%'],
  4: ['100%', '94%', '86%', '57%'],
};

// Alturas de las barras del gráfico, en px, sobre una base común.
const CHART_BARS: readonly number[] = [56, 92, 72, 120, 84];

/**
 * Esqueleto de carga con un conjunto fijo de variantes (sistema de diseño 10.21). Cada variante imita
 * la forma de lo que sustituye. Las barras son `p-skeleton` con el fondo `line` del preset y sin
 * brillo; el pulso de opacidad vive en `.ph-skeleton-bar` (styles.css). La región anuncia «Cargando…»
 * una sola vez; una lista de filas puede dejar `announce` en false salvo en la primera.
 */
@Component({
  selector: 'app-skeleton',
  imports: [SkeletonModule],
  templateUrl: './skeleton.component.html',
  host: {
    '[class]': 'hostClass()',
    '[attr.role]': "announce() ? 'status' : null",
    'aria-busy': 'true',
  },
})
export class SkeletonComponent {
  readonly variant = input.required<SkeletonVariant>();
  readonly lines = input<3 | 4>(3);
  readonly announce = input(true);

  protected readonly hostClass = computed(() => HOST_CLASS[this.variant()]);
  protected readonly textLines = computed(() => TEXT_LINES[this.lines()]);
  protected readonly chartBars = CHART_BARS;
  // Radios del sistema (4.2): full para barras de texto, xs para cifra y gráfico; el bloque de `media`
  // va a sangre y lo recorta la tarjeta.
  protected readonly radiusFull = '999px';
  protected readonly radiusBar = 'var(--radius-check)';
  protected readonly radiusBlock = '0';
}
