import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, effect, inject, input, model } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideArrowUpRight } from '@lucide/angular';
import { ButtonModule } from 'primeng/button';
import { DrawerModule } from 'primeng/drawer';

import { IdentityHue } from '../../../core/identity/identity';
import { BlockComponent } from '../block/block.component';
import { EmptyStateComponent } from '../empty-state/empty-state.component';
import { ShellSidebarService } from '../shell/shell-sidebar.service';
import { SkeletonComponent } from '../skeleton/skeleton.component';
import { StickerComponent } from '../sticker/sticker.component';

const REDUCED_MOTION = '(prefers-reduced-motion: reduce)';

/**
 * Panel de detalle del elemento seleccionado de una lista (10.13), siempre con «Abrir ficha completa».
 *
 * - En `expanded` es una columna de 4 que se queda fija arriba de la región de contenido
 *   (`sticky`, `top: 0`) y hace scroll por su cuenta si es más alta que la región.
 * - En `medium` y `compact` no ocupa columna: se abre como cajón desde la derecha cuando hay un
 *   elemento seleccionado y `open` es true. Cerrar el cajón NO borra la selección: la fila o
 *   tarjeta sigue marcada y volver a pulsarla lo reabre. La página pone `open` en true al
 *   seleccionar; en `expanded` el panel lo ignora y lo devuelve a false.
 *
 * Ranuras (se instancian siempre, aunque no haya selección, así que sus enlaces deben tolerar
 * un valor vacío; el panel solo las muestra con algo seleccionado): `[avatar]` (avatar o sticker
 * de la cabecera), `[figure]` (cifra clave y estado), `[details]` (lista de definición),
 * `[history]` (historial o gráfico pequeño) y `[actions]` (acción principal y secundaria).
 */
@Component({
  selector: 'app-detail-panel',
  imports: [
    NgTemplateOutlet,
    RouterLink,
    ButtonModule,
    DrawerModule,
    BlockComponent,
    EmptyStateComponent,
    SkeletonComponent,
    StickerComponent,
    LucideArrowUpRight,
  ],
  templateUrl: './detail-panel.component.html',
  host: { '[class]': 'hostClass()' },
})
export class DetailPanelComponent {
  /** Hay un elemento seleccionado; si no, el panel muestra el estado vacío. */
  readonly selected = input(false);
  readonly loading = input(false);
  /** Color de identidad del elemento (`identityHue(id)`). */
  readonly hue = input<IdentityHue>('honey');
  readonly heading = input('');
  readonly subtitle = input<string | null>(null);
  /** Destino de «Abrir ficha completa»; sin él no se muestra el botón. */
  readonly fullLink = input<string | unknown[] | null>(null);
  readonly fullLabel = input('Abrir ficha completa');
  /** Nombre del `aside` y título del cajón. */
  readonly label = input('Detalle');
  readonly emptyHeading = input('Elige un elemento');
  readonly emptyText = input('Selecciona una fila o una tarjeta para ver aquí su detalle.');
  /** Cajón abierto (solo `medium` y `compact`). Uso: `[(open)]="panelOpen"`. */
  readonly open = model(false);

  private readonly shell = inject(ShellSidebarService);

  protected readonly inline = computed(() => this.shell.layout() === 'expanded');
  protected readonly drawerVisible = computed(() => !this.inline() && this.open() && this.selected());
  protected readonly hostClass = computed(() =>
    this.inline() ? 'col-span-4 block min-w-0 self-stretch' : 'contents',
  );

  // Cajón: 250 ms con la curva del sistema (7), instantáneo con movimiento reducido.
  protected readonly transition = window.matchMedia(REDUCED_MOTION).matches
    ? '0ms'
    : '250ms cubic-bezier(0.23, 1, 0.32, 1)';

  constructor() {
    // En la columna fija no hay cajón: así, al reducir la ventana, no se abre solo.
    effect(() => {
      if (this.inline() && this.open()) {
        this.open.set(false);
      }
    });
  }
}
