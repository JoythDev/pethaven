import { Component, input } from '@angular/core';

import { IdentityHue } from '../../../core/identity/identity';
import { BlockComponent } from '../block/block.component';

/**
 * Héroe de una pantalla (uno como máximo): antetítulo, titular, una frase, hasta dos botones
 * proyectados con `actions` y un sticker de 180 a 290 px con `art`, que se oculta en compacto.
 */
@Component({
  selector: 'app-hero',
  imports: [BlockComponent],
  template: `
    <app-block kind="hero" [hue]="hue()">
      @if (kicker()) {
        <p class="text-[12.5px] leading-[18px] font-extrabold">{{ kicker() }}</p>
      }
      @if (headingLevel() === 1) {
        <h1 [class]="titleClass">{{ headline() }}</h1>
      } @else {
        <h2 [class]="titleClass">{{ headline() }}</h2>
      }
      @if (description()) {
        <p class="max-w-[52ch] text-sm leading-5">{{ description() }}</p>
      }
      <div class="mt-3.5 flex flex-wrap gap-2 empty:hidden"><ng-content select="[actions]" /></div>
      <ng-container ngProjectAs="[art]"><ng-content select="[art]" /></ng-container>
    </app-block>
  `,
  host: { class: 'block' },
})
export class HeroComponent {
  readonly hue = input.required<IdentityHue>();
  readonly headline = input.required<string>();
  readonly kicker = input<string | null>(null);
  readonly description = input<string | null>(null);
  readonly headingLevel = input<1 | 2>(1);

  protected readonly titleClass =
    'mt-[3px] mb-2 font-display text-[clamp(21px,2.3cqi,29px)] leading-[1.14] font-semibold tracking-[-0.01em] text-balance';
}
