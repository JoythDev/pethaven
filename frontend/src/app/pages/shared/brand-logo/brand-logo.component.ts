import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Forma del logo: completo (isotipo y nombre) o solo el isotipo. */
export type BrandLogoVariant = 'full' | 'mark';

// Proporción ancho/alto de cada SVG (su viewBox), para reservar el espacio exacto sin esperar la imagen.
const ASPECT: Record<BrandLogoVariant, number> = { full: 463 / 143, mark: 475 / 458 };
const SOURCE: Record<BrandLogoVariant, string> = {
  full: '/images/brand/pethaven-logo.svg',
  mark: '/images/brand/pethaven-isotype.svg',
};

/**
 * Logo original de PetHaven, vectorizado a un solo color. Se pinta con una máscara CSS sobre el color
 * `--color-brand`, así sigue el tema (verde en claro, crema en oscuro) y el SVG queda en caché aparte.
 * Es decorativo: el enlace que lo contiene lleva el nombre accesible.
 */
@Component({
  selector: 'app-brand-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'aria-hidden': 'true',
    class: 'inline-block shrink-0 bg-brand',
    '[style.height.px]': 'height()',
    '[style.width.px]': 'width()',
    '[style.mask]': 'mask()',
    '[style.-webkit-mask]': 'mask()',
  },
  template: '',
})
export class BrandLogoComponent {
  readonly variant = input<BrandLogoVariant>('full');
  /** Alto en píxeles; el ancho sale de la proporción del logo. */
  readonly height = input(32);

  protected readonly width = computed(() => Math.round(this.height() * ASPECT[this.variant()]));
  protected readonly mask = computed(() => `url(${SOURCE[this.variant()]}) center / contain no-repeat`);
}
