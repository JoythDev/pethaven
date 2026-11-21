import { Component, input } from '@angular/core';

export interface DefinitionItem {
  term: string;
  // null o vacío se muestra como «Sin registrar».
  value: string | number | null;
}

/**
 * Lista de dos columnas: término a la izquierda y valor a la derecha. Admite filas desde `items`
 * y también <dt>/<dd> proyectados para valores con formato propio (etiquetas, enlaces).
 * Pensada para ocho filas como máximo.
 */
@Component({
  selector: 'dl[app-definition-list]',
  template: `
    @for (item of items(); track $index) {
      <dt>{{ item.term }}</dt>
      <dd>{{ item.value === null || item.value === '' ? 'Sin registrar' : item.value }}</dd>
    }
    <ng-content />
  `,
  host: {
    class:
      'm-0 grid grid-cols-[auto_minmax(0,1fr)] items-baseline gap-x-3.5 gap-y-[7px] text-sm leading-5 ' +
      '[&>dt]:text-ink-muted [&>dd]:m-0 [&>dd]:min-w-0 [&>dd]:text-right [&>dd]:font-extrabold [&>dd]:text-ink ' +
      '[&>dd]:tabular-nums [&>dd]:break-words',
  },
})
export class DefinitionListComponent {
  readonly items = input<DefinitionItem[]>([]);
}
