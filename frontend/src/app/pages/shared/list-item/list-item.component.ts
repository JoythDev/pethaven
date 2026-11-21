import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Fila de lista (10.8): elemento inicial (`[leading]`, avatar o icono), título `body-strong`,
 * línea secundaria `caption` y elemento final (`[trailing]`, etiqueta o valor).
 * Si la fila entera es interactiva es UN botón (`interactive`, emite `activate`) o UN enlace
 * (`link`); por eso `[trailing]` no puede llevar botones ni enlaces. Sin ninguno es solo texto.
 * Las filas se apilan con 2 px de separación: el contenedor usa `flex flex-col gap-0.5`.
 */
@Component({
  selector: 'app-list-item',
  imports: [NgTemplateOutlet, RouterLink],
  templateUrl: './list-item.component.html',
  host: { class: 'block min-w-0' },
})
export class ListItemComponent {
  readonly heading = input.required<string>();
  readonly subtitle = input<string | null>(null);
  readonly selected = input(false);
  /** Convierte la fila en un botón que emite `activate`. */
  readonly interactive = input(false);
  /** Convierte la fila en un enlace de navegación; tiene prioridad sobre `interactive`. */
  readonly link = input<string | unknown[] | null>(null);
  readonly activate = output<void>();

  protected readonly rowClass = computed(
    () =>
      'flex w-full min-w-0 items-center gap-3 rounded-row px-2.5 py-[9px] text-left text-ink ' +
      (this.link() !== null || this.interactive()
        ? 'ph-focus cursor-pointer transition-colors duration-[180ms] ease-out-strong motion-reduce:transition-none ' +
          (this.selected() ? 'bg-accent-soft' : 'hover:bg-sunken')
        : this.selected()
          ? 'bg-accent-soft'
          : ''),
  );
}
