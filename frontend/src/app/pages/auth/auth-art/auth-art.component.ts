import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';

import {
  ART_COMPOSITIONS,
  ArtCorner,
  ArtItem,
  ArtShape,
  ArtSticker,
  artMotion,
  holdDuration,
  nextCompositionIndex,
} from '../../../core/auth-art/auth-art';
import { StickerComponent } from '../../shared/sticker/sticker.component';

type StyleMap = Record<string, string>;

interface ShapeView {
  className: string;
  style: StyleMap;
}

interface StickerView {
  name: ArtSticker['name'];
  kit: ArtSticker['kit'];
  style: StyleMap;
}

interface CompositionView {
  id: string;
  shapes: ShapeView[];
  stickers: StickerView[];
}

// Radio del cuarto de círculo según la esquina de la que parte: siempre redondeado hacia el interior.
const QUARTER_RADIUS: Record<ArtCorner, string> = {
  tl: '0 0 100% 0',
  tr: '0 0 0 100%',
  bl: '0 100% 0 0',
  br: '100% 0 0 0',
};

function placement(item: ArtItem): StyleMap {
  const fromRight = item.corner === 'tr' || item.corner === 'br';
  const fromBottom = item.corner === 'bl' || item.corner === 'br';
  const { dx, dy, duration, delay } = item.drift;
  return {
    '--s': String(item.size),
    left: fromRight ? 'auto' : `${item.x}%`,
    right: fromRight ? `${item.x}%` : 'auto',
    top: fromBottom ? 'auto' : `${item.y}%`,
    bottom: fromBottom ? `${item.y}%` : 'auto',
    rotate: `${item.rotate}deg`,
    '--dx': `${dx}px`,
    '--dy': `${dy}px`,
    '--dur': `${duration}s`,
    '--delay': `${delay}s`,
  };
}

function shapeView(shape: ArtShape): ShapeView {
  const style = placement(shape);
  if (shape.kind === 'quarter') {
    style['border-radius'] = QUARTER_RADIUS[shape.corner];
  }
  return { className: `art-item ph-geo-${shape.kind}`, style };
}

const VIEWS: readonly CompositionView[] = ART_COMPOSITIONS.map((c) => ({
  id: c.id,
  shapes: c.shapes.map(shapeView),
  stickers: c.stickers.map((s) => ({ name: s.name, kit: s.kit, style: placement(s) })),
}));

const REDUCED_MOTION = '(prefers-reduced-motion: reduce)';

/**
 * Bloque ilustrado del acceso: varias composiciones de geometría y stickers que se alternan con un
 * fundido de 600 ms cada 12 a 16 s, mientras cada elemento deriva despacio (como máximo 12 px).
 * Se detiene con la pestaña oculta y, con movimiento reducido, muestra una sola composición fija.
 * Es decorativo: sin foco ni texto. El tamaño de todo escala con el bloque (cqmin).
 */
@Component({
  selector: 'app-auth-art',
  imports: [StickerComponent],
  templateUrl: './auth-art.component.html',
  styleUrl: './auth-art.component.css',
  host: { class: 'absolute inset-0 block', 'aria-hidden': 'true', '[class.is-paused]': 'motion() !== "running"' },
})
export class AuthArtComponent {
  private readonly document = inject(DOCUMENT);

  protected readonly compositions = VIEWS;
  protected readonly active = signal(0);

  private readonly reducedMotion = signal(false);
  private readonly hidden = signal(this.document.hidden);
  protected readonly motion = computed(() => artMotion({ reducedMotion: this.reducedMotion(), hidden: this.hidden() }));

  constructor() {
    const destroyRef = inject(DestroyRef);

    const query = this.document.defaultView?.matchMedia(REDUCED_MOTION);
    if (query) {
      this.reducedMotion.set(query.matches);
      const onMotionChange = (event: MediaQueryListEvent) => this.reducedMotion.set(event.matches);
      query.addEventListener('change', onMotionChange);
      destroyRef.onDestroy(() => query.removeEventListener('change', onMotionChange));
    }

    const onVisibility = () => this.hidden.set(this.document.hidden);
    this.document.addEventListener('visibilitychange', onVisibility);
    destroyRef.onDestroy(() => this.document.removeEventListener('visibilitychange', onVisibility));

    // Una sola composición fija con movimiento reducido.
    effect(() => {
      if (this.motion() === 'static') {
        this.active.set(0);
      }
    });

    // Un temporizador por composición; se descarta al ocultar la pestaña y se rearma al volver.
    effect((onCleanup) => {
      if (this.motion() !== 'running') {
        return;
      }
      const index = this.active();
      const timer = setTimeout(
        () => this.active.set(nextCompositionIndex(index, this.compositions.length)),
        holdDuration(index),
      );
      onCleanup(() => clearTimeout(timer));
    });
  }
}
