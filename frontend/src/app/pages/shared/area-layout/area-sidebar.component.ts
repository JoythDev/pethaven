import { Component, input, output } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TooltipModule } from 'primeng/tooltip';
import { DEFAULT_NAV_ITEMS } from './area-layout-config';
import { AreaNavItem } from './area-nav-item';

/** Navegación lateral de un área; configurable y por defecto sirve al veterinario. */
@Component({
  selector: 'app-area-sidebar',
  imports: [NgClass, RouterLink, RouterLinkActive, TooltipModule],
  templateUrl: './area-sidebar.component.html',
})
export class AreaSidebarComponent {
  /** Notifica al layout para cerrar el drawer en móvil. */
  readonly itemClicked = output<void>();

  readonly items = input<AreaNavItem[]>(DEFAULT_NAV_ITEMS);
  readonly homeLink = input<string>('/veterinario');
  readonly sectionLabel = input<string>('Panel');

  /** PrimeIcons se renderiza como <i>; cualquier otro valor es una ruta de SVG. */
  isPrimeIcon(icon: string): boolean {
    return icon.startsWith('pi ');
  }

  /** Ejecuta la acción de un ítem sin ruta (p. ej. cerrar sesión) y cierra el drawer. */
  runCommand(item: AreaNavItem): void {
    this.itemClicked.emit();
    item.command?.();
  }
}
