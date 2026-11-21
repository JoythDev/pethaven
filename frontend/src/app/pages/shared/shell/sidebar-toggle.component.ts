import { Component, inject } from '@angular/core';
import { LucidePanelLeft } from '@lucide/angular';
import { TooltipModule } from 'primeng/tooltip';

import { ShellSidebarService } from './shell-sidebar.service';

/**
 * Botón que muestra u oculta la barra lateral contextual. Vive en la cabecera de página y, en
 * compacto, en la barra superior; quien lo coloca decide cuándo se ve.
 */
@Component({
  selector: 'app-sidebar-toggle',
  imports: [TooltipModule, LucidePanelLeft],
  template: `
    <button
      type="button"
      class="ph-focus ph-icon-btn"
      aria-label="Mostrar u ocultar la barra lateral"
      pTooltip="Mostrar u ocultar la barra lateral"
      tooltipPosition="bottom"
      [attr.aria-expanded]="sidebar.expanded()"
      (click)="sidebar.toggle()"
    >
      <svg lucidePanelLeft [size]="18" aria-hidden="true"></svg>
    </button>
  `,
  host: { class: 'contents' },
})
export class SidebarToggleComponent {
  protected readonly sidebar = inject(ShellSidebarService);
}
