import { Component, computed, input } from '@angular/core';

import { share } from '../../../core/admin-dashboard/admin-dashboard';

const RADIUS = 34;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

/**
 * Anillo de proporción: `value` de `total` en el color de acento sobre una pista neutra, con el valor
 * al centro. Un SVG sencillo basta (no es una gráfica con ejes); `label` es el texto para lectores.
 */
@Component({
  selector: 'app-ratio-ring',
  template: `<svg class="size-[88px]" viewBox="0 0 88 88" role="img" [attr.aria-label]="label()">
    <circle cx="44" cy="44" [attr.r]="radius" fill="none" stroke="var(--color-sunken)" stroke-width="9" />
    <circle
      cx="44"
      cy="44"
      [attr.r]="radius"
      fill="none"
      stroke="var(--color-accent)"
      stroke-width="9"
      stroke-linecap="round"
      transform="rotate(-90 44 44)"
      [attr.stroke-dasharray]="dash()"
    />
    <text x="44" y="51" text-anchor="middle" class="fill-ink font-display text-[22px] font-semibold tabular-nums">{{ value() }}</text>
  </svg>`,
  host: { class: 'inline-flex shrink-0' },
})
export class RatioRingComponent {
  readonly value = input.required<number>();
  readonly total = input.required<number>();
  readonly label = input.required<string>();

  protected readonly radius = RADIUS;
  protected readonly dash = computed(() => {
    const filled = (share(this.value(), this.total()) / 100) * CIRCUMFERENCE;
    return `${filled} ${CIRCUMFERENCE}`;
  });
}
