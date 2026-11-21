import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, input } from '@angular/core';

/**
 * Superficie neutra con borde de 1 px. Cabecera opcional: título a la izquierda, contenido junto al
 * título con `title-extra` (p. ej. la etiqueta Demo) y una sola acción a la derecha con `action`.
 * Las tarjetas no se anidan. Con `clickable` tiene estado hover y un único destino (p. ej. `<a app-card>`).
 */
@Component({
  selector: 'app-card, [app-card]',
  template: `
    @if (heading()) {
      <header class="mb-3 flex min-h-[30px] items-center gap-2">
        @if (headingLevel() === 2) {
          <h2 [class]="titleClass">{{ heading() }}<ng-container *ngTemplateOutlet="extra" /></h2>
        } @else {
          <h3 [class]="titleClass">{{ heading() }}<ng-container *ngTemplateOutlet="extra" /></h3>
        }
        <div class="ml-auto flex shrink-0 items-center empty:hidden"><ng-content select="[action]" /></div>
      </header>
    }
    <ng-template #extra><ng-content select="[title-extra]" /></ng-template>
    <ng-content />
  `,
  imports: [NgTemplateOutlet],
  host: { '[class]': 'hostClass()' },
})
export class CardComponent {
  readonly heading = input<string | null>(null);
  readonly headingLevel = input<2 | 3>(2);
  readonly clickable = input(false);

  protected readonly titleClass =
    'min-w-0 flex-1 font-display text-[17px] leading-[22px] font-semibold text-balance text-ink';

  protected readonly hostClass = computed(
    () =>
      'relative block min-w-0 rounded-box border border-line bg-card p-4 text-ink' +
      (this.clickable() ? ' cursor-pointer transition-colors duration-[180ms] ease-out-strong hover:bg-sunken' : ''),
  );
}
