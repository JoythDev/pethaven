import { Component, input } from '@angular/core';

/**
 * Estado vacío centrado: sticker proyectado con el atributo `sticker`, título, una frase que dice
 * por qué está vacío y cómo llenarlo, y una acción opcional proyectada con `action`.
 * Si el rol no puede crear el elemento, no se proyecta acción y el texto dice quién puede.
 */
@Component({
  selector: 'app-empty-state',
  template: `
    <ng-content select="[sticker]" />
    <h3 class="font-display text-[17px] leading-[22px] font-semibold text-balance text-ink">{{ title() }}</h3>
    <p class="max-w-[36ch] text-sm leading-5 text-ink-muted">{{ description() }}</p>
    <div class="mt-2 empty:hidden"><ng-content select="[action]" /></div>
  `,
  host: { class: 'flex flex-col items-center gap-2 px-4 py-6.5 text-center' },
})
export class EmptyStateComponent {
  readonly title = input.required<string>();
  readonly description = input.required<string>();
}
