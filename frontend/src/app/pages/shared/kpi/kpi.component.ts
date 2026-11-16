import { Component, input } from '@angular/core';
import { LucideMoveRight, LucideTrendingDown, LucideTrendingUp } from '@lucide/angular';

import { IdentityHue } from '../../../core/identity/identity';
import { BlockComponent } from '../block/block.component';
import { SparklineComponent } from '../sparkline/sparkline.component';

export interface KpiTrend {
  // Comparación con el periodo anterior, ya redactada («+12 % frente a octubre»).
  text: string;
  direction: 'up' | 'down' | 'flat';
}

/**
 * Bloque KPI: etiqueta, cifra, comparación con dirección y, si hay serie, minigráfica. Un objeto
 * pequeño se proyecta con `art` en la esquina. `valueId` conserva ids que usan pruebas existentes.
 */
@Component({
  selector: 'app-kpi',
  imports: [BlockComponent, SparklineComponent, LucideTrendingUp, LucideTrendingDown, LucideMoveRight],
  template: `
    <app-block kind="kpi" [hue]="hue()" class="h-full">
      <span class="text-[12.5px] leading-[18px] font-extrabold">{{ label() }}</span>
      <div class="mt-1.5 flex items-end justify-between gap-3">
        <span [attr.id]="valueId()" class="font-display text-[28px] leading-7 font-semibold tracking-[-0.01em] tabular-nums">{{ value() }}</span>
        @if (series().length > 1) {
          <app-sparkline [values]="series()" [width]="72" [height]="28" />
        }
      </div>
      @if (trend(); as t) {
        <span class="mt-auto flex items-center gap-1.5 pt-2 text-[12.5px] leading-[18px] font-extrabold">
          @switch (t.direction) {
            @case ('up') { <svg lucideTrendingUp [size]="16" aria-hidden="true"></svg> }
            @case ('down') { <svg lucideTrendingDown [size]="16" aria-hidden="true"></svg> }
            @default { <svg lucideMoveRight [size]="16" aria-hidden="true"></svg> }
          }
          {{ t.text }}
        </span>
      }
      <ng-container ngProjectAs="[art]"><ng-content select="[art]" /></ng-container>
      <ng-content />
    </app-block>
  `,
  host: { class: 'block' },
})
export class KpiComponent {
  readonly hue = input.required<IdentityHue>();
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly trend = input<KpiTrend | null>(null);
  readonly series = input<readonly number[]>([]);
  readonly valueId = input<string | null>(null);
}
