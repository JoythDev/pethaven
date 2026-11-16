import { DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { StockLevel, stockLevel, stockMeterScale } from '../../../core/status/stock-level';

const FILL_CLASS: Record<StockLevel, string> = {
  ok: 'bg-success',
  low: 'bg-warning',
  out: 'bg-danger',
};

// Barra de existencias con una marca en el mínimo. El valor numérico siempre acompaña a la barra.
@Component({
  selector: 'app-stock-meter',
  imports: [DecimalPipe],
  templateUrl: './stock-meter.component.html',
  host: { class: 'block min-w-[70px]' },
})
export class StockMeterComponent {
  readonly units = input.required<number>();
  readonly minimum = input.required<number>();
  // Tope de la escala; sin él se usa el triple del mínimo.
  readonly max = input<number | undefined>(undefined);

  protected readonly scale = computed(() => stockMeterScale(this.units(), this.minimum(), this.max()));
  protected readonly fillClass = computed(() => FILL_CLASS[stockLevel(this.units(), this.minimum())]);
}
