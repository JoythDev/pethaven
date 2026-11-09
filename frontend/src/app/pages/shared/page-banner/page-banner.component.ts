import { Component, computed, input } from '@angular/core';

export type BannerColor = 'honey' | 'sage' | 'secondary';
export type BannerMascot = 'dog' | 'cat';
/** hero: dashboards (título grande, mascota que flota); compact: listados y fichas. */
export type BannerVariant = 'hero' | 'compact';

// Fondo, sombra y formas decorativas de cada color del banner.
const THEMES: Record<BannerColor, { surface: string; circleA: string; circleB: string }> = {
  honey: { surface: 'bg-honey shadow-honey/30', circleA: 'bg-white/30', circleB: 'bg-terracotta/30' },
  sage: { surface: 'bg-sage shadow-sage/30', circleA: 'bg-white/30', circleB: 'bg-honey/40' },
  secondary: { surface: 'bg-secondary shadow-secondary/50', circleA: 'bg-honey/50', circleB: 'bg-white/40' },
};

/**
 * Banner de color al inicio de una página: saludo opcional, título, subtítulo, mascota dibujada y un
 * espacio para el contenido propio (botones, avatar, etiquetas). La mascota aparece desde `lg`.
 */
@Component({
  selector: 'app-page-banner',
  templateUrl: './page-banner.component.html',
  host: { class: 'block' },
})
export class PageBannerComponent {
  readonly color = input<BannerColor>('honey');
  readonly variant = input<BannerVariant>('compact');
  readonly mascot = input<BannerMascot | null>(null);
  /** Texto manuscrito sobre el título (por ejemplo, el saludo). */
  readonly eyebrow = input<string | null>(null);
  readonly title = input<string | null>(null);
  readonly subtitle = input<string | null>(null);
  /** Clases del contenedor del contenido; las fichas lo usan para repartir el texto y los botones. */
  readonly contentClass = input('max-w-xl');

  protected readonly theme = computed(() => THEMES[this.color()]);

  protected readonly bannerClass = computed(
    () =>
      `banner relative overflow-hidden rounded-3xl p-6 shadow-lg sm:p-8 ${this.theme().surface}` +
      (this.variant() === 'hero' ? ' lg:min-h-52' : ''),
  );

  protected readonly mascotClass = computed(() =>
    this.variant() === 'hero'
      ? 'absolute bottom-0 right-6 hidden h-44 w-auto animate-float lg:right-12 lg:block lg:h-52'
      : 'absolute bottom-0 right-6 hidden h-32 w-auto lg:right-12 lg:block',
  );

  protected readonly titleClass = computed(() =>
    this.variant() === 'hero'
      ? 'mt-1 font-display text-3xl font-bold text-text sm:text-5xl'
      : 'font-display text-3xl font-bold text-text',
  );

  protected readonly mascotSrc = computed(() => `/images/illustrations/${this.mascot()}-mascot.svg`);
}
