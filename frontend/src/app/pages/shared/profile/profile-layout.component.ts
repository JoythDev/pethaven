import { Component, input } from '@angular/core';

import { PageHeaderComponent } from '../page-header/page-header.component';

/**
 * Plantilla de la página de perfil de los tres roles: cabecera de página y una rejilla de 12
 * columnas donde cada pieza proyectada indica su ancho (`col-span-12 @min-[960px]:col-span-4`).
 * No conoce datos de ningún rol: el bloque de identidad, las tarjetas y el formulario los pone la página.
 */
@Component({
  selector: 'app-profile-layout',
  imports: [PageHeaderComponent],
  template: `
    <app-page-header [heading]="heading()" [subtitle]="subtitle()" />
    <div class="grid grid-cols-12 items-start gap-[14px]">
      <ng-content />
    </div>
  `,
  host: { class: 'block' },
})
export class ProfileLayoutComponent {
  readonly heading = input('Perfil');
  readonly subtitle = input<string | null>('Tus datos y preferencias');
}
