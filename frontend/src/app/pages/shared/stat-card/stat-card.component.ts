import { Component, computed, input } from '@angular/core';

export type StatColor = 'honey' | 'sage' | 'secondary' | 'terracotta' | 'tertiary' | 'danger';
/** card: indicador de dashboard; inline: icono junto a la cifra; tile: dato compacto de una ficha. */
export type StatLayout = 'card' | 'inline' | 'tile';
export type StatDecor = 'heart' | 'cross' | 'circles' | 'paw' | 'icon' | 'none';

interface StatTheme {
  surface: string;
  label: string;
  value: string;
  chip: string;
  decor: string;
  fill: string;
  accent: string;
}

// Colores de cada tarjeta. El texto de las tarjetas terracota es oscuro para cumplir el contraste.
const THEMES: Record<StatColor, StatTheme> = {
  honey: { surface: 'bg-honey text-text shadow-honey/30', label: 'text-text/75', value: 'text-text', chip: 'bg-white/60', decor: 'text-white/30', fill: 'bg-white/30', accent: 'bg-terracotta/30' },
  sage: { surface: 'bg-sage text-text shadow-sage/30', label: 'text-text/75', value: 'text-text', chip: 'bg-white/60', decor: 'text-white/30', fill: 'bg-white/30', accent: 'bg-honey/40' },
  secondary: { surface: 'bg-secondary text-text shadow-secondary/50', label: 'text-text/75', value: 'text-text', chip: 'bg-white/60', decor: 'text-white/40', fill: 'bg-white/40', accent: 'bg-terracotta/30' },
  terracotta: { surface: 'bg-terracotta text-text shadow-terracotta/30', label: 'text-text', value: 'text-text', chip: 'bg-white/60', decor: 'text-white/20', fill: 'bg-white/20', accent: 'bg-white/20' },
  tertiary: { surface: 'bg-tertiary text-background shadow-tertiary/30', label: 'text-background/80', value: 'text-honey', chip: 'bg-honey', decor: 'text-honey/25', fill: 'bg-honey/25', accent: 'bg-honey/25' },
  danger: { surface: 'bg-red-100 text-red-800 shadow-red-100', label: 'text-red-800/80', value: 'text-red-800', chip: 'bg-white/60', decor: 'text-white/40', fill: 'bg-white/40', accent: 'bg-white/40' },
};

/**
 * Tarjeta de color con una etiqueta y una cifra o dato. El valor sale de `value` o del contenido
 * proyectado (útil para enlaces o etiquetas); `[extra]` añade contenido bajo el valor.
 */
@Component({
  selector: 'article[app-stat-card], div[app-stat-card]',
  templateUrl: './stat-card.component.html',
  host: { '[class]': 'hostClass()' },
})
export class StatCardComponent {
  readonly label = input.required<string>();
  readonly value = input<string | number | null>(null);
  readonly caption = input<string | null>(null);
  readonly color = input<StatColor>('honey');
  readonly layout = input<StatLayout>('card');
  readonly decor = input<StatDecor>('none');
  /** Clase de PrimeIcons ('pi pi-x') o ruta de una imagen; se muestra en un chip o como decoración. */
  readonly icon = input<string | null>(null);
  /** Mascota dibujada en la esquina; reserva espacio a la derecha. */
  readonly mascot = input<'dog' | 'cat' | null>(null);
  /** Cifra extragrande, para la tarjeta principal de un dashboard. */
  readonly big = input(false);
  /** Clases extra para el valor (tamaño, salto de línea). */
  readonly valueClass = input('');

  protected readonly theme = computed(() => THEMES[this.color()]);
  protected readonly isTile = computed(() => this.layout() === 'tile');
  protected readonly isInline = computed(() => this.layout() === 'inline');
  protected readonly isPrimeIcon = computed(() => (this.icon() ?? '').startsWith('pi '));
  protected readonly showChip = computed(() => !this.isTile() && !!this.icon() && this.decor() !== 'icon');

  protected readonly hostClass = computed(() => {
    const theme = this.theme();
    if (this.isTile()) {
      return `relative block overflow-hidden rounded-2xl p-4 ${theme.surface.replace(/ shadow-\S+/, '')}`;
    }
    return (
      `relative overflow-hidden rounded-3xl p-6 shadow-lg transition duration-300 hover:-translate-y-1 ${theme.surface}` +
      (this.isInline() ? ' flex items-center gap-4' : ' block') +
      (this.mascot() ? ' pr-[40%]' : '')
    );
  });

  protected readonly chipClass = computed(
    () =>
      `relative flex shrink-0 items-center justify-center rounded-2xl ${this.theme().chip} ` +
      (this.isInline() ? 'h-12 w-12' : 'mb-4 h-11 w-11'),
  );

  protected readonly labelClass = computed(
    () =>
      `relative ${this.theme().label} ` +
      (this.isInline()
        ? 'mt-1 text-sm font-semibold'
        : this.isTile()
          ? 'text-xs font-bold uppercase tracking-wider'
          : 'text-sm font-bold uppercase tracking-wider'),
  );

  // Si valueClass trae su propio tamaño de texto, reemplaza al tamaño por defecto del diseño.
  protected readonly valueClassName = computed(() => {
    const custom = this.valueClass();
    const ownSize = /(^|\s)text-(xs|sm|base|\d?xl)(\s|$)/.test(custom);
    const size = ownSize
      ? ''
      : this.isInline()
        ? 'text-5xl leading-none'
        : this.isTile()
          ? 'text-2xl'
          : `leading-none ${this.big() ? 'text-7xl sm:text-8xl' : 'text-6xl'}`;
    const spacing = this.isInline() ? '' : 'mt-1 ';
    return `relative font-display font-bold ${this.theme().value} ${spacing}${size} ${custom}`;
  });

  protected readonly heartClass = computed(
    () =>
      `absolute rotate-12 ${this.theme().decor} ` +
      (this.big() ? '-right-10 -top-10 h-56 w-56' : '-bottom-6 -right-6 h-32 w-32'),
  );
}
