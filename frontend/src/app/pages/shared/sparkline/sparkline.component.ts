import { Component, computed, input } from '@angular/core';

import { sparklinePoints } from '../../../core/charts/sparkline';

// Línea de tendencia sin ejes, en el color del texto. Decorativa: la cifra va en texto al lado.
@Component({
  selector: 'app-sparkline',
  template: `<svg [attr.width]="width()" [attr.height]="height()" [attr.viewBox]="'0 0 ' + width() + ' ' + height()" aria-hidden="true" focusable="false">
    <polyline [attr.points]="points()" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
  </svg>`,
  host: { class: 'inline-flex shrink-0' },
})
export class SparklineComponent {
  readonly values = input.required<readonly number[]>();
  readonly width = input(96);
  readonly height = input(30);

  protected readonly points = computed(() => sparklinePoints(this.values(), this.width(), this.height()));
}
