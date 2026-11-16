import { Component, computed, input } from '@angular/core';

import { IdentityHue } from '../../../core/identity/identity';

export type BlockKind = 'hero' | 'kpi' | 'summary' | 'header';

const KIND_CLASS: Record<BlockKind, string> = {
  hero: 'flex items-center gap-4 px-[22px] py-5 min-h-[196px]',
  kpi: 'flex flex-col p-4 min-h-[128px]',
  summary: 'block p-4',
  header: 'flex items-end gap-3 p-4 min-h-[118px]',
};

/**
 * Bloque de color con geometría plana detrás del contenido. Todo lo de dentro usa `on-hue` y los
 * valores derivados (texto atenuado, divisores, etiquetas y botones invertidos; ver .ph-block en
 * styles.css). El sticker se proyecta con el atributo `art`: en el héroe sobresale por arriba y
 * guarda distancia del borde inferior; en el KPI va en la esquina inferior derecha.
 * La página que pone un héroe al inicio de la rejilla deja margen arriba para el sticker.
 */
@Component({
  selector: 'app-block',
  templateUrl: './block.component.html',
  host: { '[class]': 'hostClass()' },
})
export class BlockComponent {
  readonly hue = input.required<IdentityHue>();
  readonly kind = input<BlockKind>('summary');
  // Sin esquinas redondeadas, para la cabecera dentro de un panel o tarjeta que ya recorta.
  readonly flush = input(false);

  protected readonly hostClass = computed(
    () =>
      `ph-block hue-${this.hue()} relative ${this.flush() ? 'rounded-none' : 'rounded-box'} ${KIND_CLASS[this.kind()]}`,
  );
}
