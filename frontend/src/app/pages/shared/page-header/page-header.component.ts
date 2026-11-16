import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideChevronLeft, LucidePanelLeft } from '@lucide/angular';
import { TooltipModule } from 'primeng/tooltip';

import { DemoTagComponent } from '../demo-tag/demo-tag.component';
import { ShellSidebarService } from '../shell/shell-sidebar.service';

/**
 * Cabecera de página (9.6): título `title-1` con subtítulo, enlace de regreso opcional y acciones a
 * la derecha (`[actions]`). Muestra el botón de la barra lateral solo si la página aporta contenido
 * contextual; en compacto ese botón vive en la barra superior. El input se llama `heading` porque
 * un atributo `title` dejaría un tooltip nativo.
 */
@Component({
  selector: 'app-page-header',
  imports: [RouterLink, TooltipModule, DemoTagComponent, LucideChevronLeft, LucidePanelLeft],
  templateUrl: './page-header.component.html',
  host: { class: 'mb-4 block' },
})
export class PageHeaderComponent {
  readonly heading = input.required<string>();
  readonly subtitle = input<string | null>(null);
  readonly backLink = input<string | null>(null);
  readonly backLabel = input('Volver');
  /** Marca la pantalla como demo (usa datos de ejemplo). */
  readonly demo = input(false);

  protected readonly sidebar = inject(ShellSidebarService);
}
