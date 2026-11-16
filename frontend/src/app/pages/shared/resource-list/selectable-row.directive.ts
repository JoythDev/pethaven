import { Directive, computed, input, output } from '@angular/core';

// Elementos de una celda que tienen su propia acción: un clic sobre ellos no selecciona la fila.
const OWN_ACTION = 'a, button, input, select, textarea, [role="button"]';

/**
 * Fila de tabla seleccionable (10.9): un clic, Enter o Espacio la seleccionan. Lleva `aria-selected`,
 * foco visible y los estados hover y seleccionado del sistema. Uso:
 * `<tr [appSelectableRow]="item.id === selectedId" (rowActivate)="select(item.id)">`.
 */
@Directive({
  selector: 'tr[appSelectableRow]',
  host: {
    role: 'row',
    tabindex: '0',
    '[attr.aria-selected]': 'selected()',
    '[class]': 'hostClass()',
    '(click)': 'onClick($event)',
    '(keydown.enter)': 'onKey($event)',
    '(keydown.space)': 'onKey($event)',
  },
})
export class SelectableRowDirective {
  readonly selected = input(false, { alias: 'appSelectableRow' });
  readonly rowActivate = output<void>();

  protected readonly hostClass = computed(
    () =>
      'ph-focus cursor-pointer transition-colors duration-[180ms] ease-out-strong [--ph-focus-offset:-2.5px] motion-reduce:transition-none ' +
      (this.selected() ? 'bg-accent-soft' : 'hover:bg-sunken'),
  );

  protected onClick(event: MouseEvent): void {
    if (!(event.target as HTMLElement).closest(OWN_ACTION)) {
      this.rowActivate.emit();
    }
  }

  protected onKey(event: Event): void {
    // Solo cuando el foco está en la fila misma; dentro de un botón de la fila decide ese botón.
    if (event.target === event.currentTarget) {
      event.preventDefault();
      this.rowActivate.emit();
    }
  }
}
