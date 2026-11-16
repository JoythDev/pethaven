import { Component, computed, effect, input, output, signal } from '@angular/core';

import { identityHue } from '../../../core/identity/identity';
import { BlockComponent } from '../block/block.component';

/**
 * Tarjeta de recurso para la vista de tarjetas (10.12): cabecera de 118 px con el color de
 * identidad y la foto o el sticker (`[art]`, de 96 px), y debajo el nombre, la etiqueta de estado
 * (`[status]`), una línea secundaria, uno o dos datos clave (`[facts]`) y una sola acción (`[action]`).
 *
 * Un clic en la tarjeta la selecciona. Como una tarjeta no puede ser un botón que contenga otro
 * botón, el nombre es el único botón de selección y su pseudoelemento cubre toda la tarjeta
 * («enlace extendido»); la acción queda por encima (`z-10`) como hermana, no anidada. `[facts]` y
 * `[status]` no deben llevar elementos interactivos: la cubierta los taparía.
 */
@Component({
  selector: 'app-resource-card',
  imports: [BlockComponent],
  templateUrl: './resource-card.component.html',
  host: { '[class]': 'hostClass()' },
})
export class ResourceCardComponent {
  /** Id del recurso: de él sale el color de identidad. */
  readonly id = input.required<number | string>();
  readonly heading = input.required<string>();
  readonly subtitle = input<string | null>(null);
  readonly photoUrl = input<string | null>(null);
  readonly selected = input(false);
  /** Se emite al seleccionar la tarjeta con clic, Enter o Espacio. */
  readonly activate = output<void>();

  private readonly photoFailed = signal(false);

  protected readonly hue = computed(() => identityHue(this.id()));
  protected readonly showPhoto = computed(() => !!this.photoUrl() && !this.photoFailed());

  protected readonly hostClass = computed(
    () =>
      'relative flex min-w-0 flex-col overflow-hidden rounded-box border border-line text-ink transition-colors duration-[180ms] ease-out-strong motion-reduce:transition-none ' +
      'has-[.rc-main:focus-visible]:outline-[2.5px] has-[.rc-main:focus-visible]:outline-offset-2 has-[.rc-main:focus-visible]:outline-accent ' +
      (this.selected() ? 'bg-accent-soft' : 'bg-card hover:bg-sunken'),
  );

  constructor() {
    // Una URL nueva merece otro intento de carga.
    effect(() => {
      this.photoUrl();
      this.photoFailed.set(false);
    });
  }

  protected onPhotoError(): void {
    this.photoFailed.set(true);
  }
}
