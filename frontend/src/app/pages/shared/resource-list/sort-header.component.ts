import { Component, computed, input, output } from '@angular/core';
import { LucideArrowUpDown, LucideChevronDown, LucideChevronUp } from '@lucide/angular';

import { ListSort, toggleSort } from '../../../core/list/list-query';

/**
 * Encabezado de columna ordenable (10.9): un botón con `arrow-up-down` que pasa a chevron al
 * ordenar, y `aria-sort` en la celda. Emite el orden siguiente; la página decide qué hacer con él.
 * Uso: `<th app-sort-header field="name" [sort]="query().sort" (sortChange)="onSort($event)">Nombre</th>`.
 */
@Component({
  selector: 'th[app-sort-header]',
  imports: [LucideArrowUpDown, LucideChevronDown, LucideChevronUp],
  template: `
    <button
      type="button"
      class="ph-focus -mx-1.5 inline-flex min-h-6 items-center gap-1.5 rounded-check px-1.5 py-0.5 transition-colors duration-[180ms] ease-out-strong hover:text-ink motion-reduce:transition-none"
      [class.flex-row-reverse]="align() === 'end'"
      (click)="sortChange.emit(next())"
    >
      <ng-content />
      @switch (direction()) {
        @case ('asc') {
          <svg lucideChevronUp [size]="14" aria-hidden="true"></svg>
        }
        @case ('desc') {
          <svg lucideChevronDown [size]="14" aria-hidden="true"></svg>
        }
        @default {
          <svg lucideArrowUpDown [size]="14" class="opacity-60" aria-hidden="true"></svg>
        }
      }
    </button>
  `,
  host: {
    scope: 'col',
    '[attr.aria-sort]': 'ariaSort()',
    '[class.text-right]': "align() === 'end'",
  },
})
export class SortHeaderComponent {
  readonly field = input.required<string>();
  readonly sort = input<ListSort | null>(null);
  /** `end` para columnas numéricas, alineadas a la derecha. */
  readonly align = input<'start' | 'end'>('start');
  readonly sortChange = output<ListSort>();

  protected readonly direction = computed(() => (this.sort()?.field === this.field() ? this.sort()!.direction : null));
  protected readonly ariaSort = computed(() => {
    const direction = this.direction();
    return direction === 'asc' ? 'ascending' : direction === 'desc' ? 'descending' : 'none';
  });

  protected next(): ListSort {
    return toggleSort(this.sort(), this.field());
  }
}
