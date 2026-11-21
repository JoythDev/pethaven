import { Component, computed, effect, input, signal } from '@angular/core';

import { identityHue, initials } from '../../../core/identity/identity';
import { BlockComponent } from '../block/block.component';

/**
 * Bloque de color de la persona en su perfil (11.2): foto o iniciales, nombre, línea de contexto
 * (rol y especialidad) y, proyectados, sus datos clave y la etiqueta de estado. El tono sale del id,
 * como en el resto de pantallas. Sirve a los tres roles: no sabe de qué rol es la persona.
 */
@Component({
  selector: 'app-profile-header',
  imports: [BlockComponent],
  template: `
    <app-block [hue]="hue()" kind="summary" class="h-full">
      <span
        class="mb-3 grid size-[88px] shrink-0 place-items-center overflow-hidden rounded-full bg-[color-mix(in_srgb,var(--color-on-hue)_12%,var(--hue))] font-display text-[36px] leading-none font-semibold"
      >
        @if (showPhoto()) {
          <img
            class="size-full object-cover"
            [src]="photoUrl()"
            [alt]="'Foto de ' + name()"
            (error)="photoFailed.set(true)"
          />
        } @else {
          <span aria-hidden="true">{{ initialsText() }}</span>
        }
      </span>
      <h2 class="font-display text-[22px] leading-7 font-semibold text-balance break-words">{{ name() }}</h2>
      <p class="mt-0.5 text-sm leading-5">{{ subtitle() }}</p>
      <div class="mt-3 flex flex-wrap items-center gap-x-3 gap-y-2 text-sm leading-5 empty:hidden">
        <ng-content />
      </div>
    </app-block>
  `,
  host: { class: 'block' },
})
export class ProfileHeaderComponent {
  readonly id = input.required<number | string>();
  readonly name = input.required<string>();
  readonly subtitle = input.required<string>();
  readonly photoUrl = input<string | null>(null);

  protected readonly photoFailed = signal(false);
  protected readonly hue = computed(() => identityHue(this.id()));
  protected readonly initialsText = computed(() => initials(this.name()));
  protected readonly showPhoto = computed(() => !!this.photoUrl() && !this.photoFailed());

  constructor() {
    // Una URL nueva merece otro intento de carga.
    effect(() => {
      this.photoUrl();
      this.photoFailed.set(false);
    });
  }
}
