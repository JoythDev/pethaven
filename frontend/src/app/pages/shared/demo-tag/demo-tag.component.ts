import { Component } from '@angular/core';
import { TooltipModule } from 'primeng/tooltip';

// Marca una función que se muestra con datos de ejemplo y aún no tiene backend.
// Se quita en cuanto la función lee datos reales.
@Component({
  selector: 'app-demo-tag',
  imports: [TooltipModule],
  template: `<span
    class="inline-flex h-[22px] items-center rounded-full border-[1.5px] border-dashed border-line-strong px-2.5 font-body text-xs font-extrabold text-ink-muted"
    tabindex="0"
    pTooltip="Función en demostración: usa datos de ejemplo."
    tooltipPosition="top"
    >Demo</span
  >`,
  host: { class: 'ml-2 inline-flex shrink-0 align-middle' },
})
export class DemoTagComponent {}
