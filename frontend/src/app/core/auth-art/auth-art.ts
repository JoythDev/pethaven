import { CoatKit, StickerName, stickerCanvas } from '../stickers/stickers';

// Bloque de color del acceso (sistema de diseño 9.11 y 7): composiciones de geometría y stickers que se
// alternan despacio. Todo es determinista: no hay azar, así que el orden y los tiempos son los mismos siempre.

export type ArtShapeKind = 'circle' | 'quarter' | 'peak' | 'ring' | 'square' | 'half';
// Esquina desde la que se mide la posición: `x` e `y` son el % del bloque entre esa esquina y el elemento.
export type ArtCorner = 'tl' | 'tr' | 'bl' | 'br';

export interface ArtDrift {
  // Desplazamiento máximo en píxeles (la magnitud no supera 12) y duración en segundos de ida (8 a 12).
  dx: number;
  dy: number;
  duration: number;
  // Negativo: cada elemento arranca en un punto distinto de su ciclo.
  delay: number;
}

interface ArtPlacement {
  corner: ArtCorner;
  x: number;
  y: number;
  // Ancho en cqmin (1 = 1 % del lado menor del bloque), para que todo escale con el bloque.
  size: number;
  rotate: number;
  drift: ArtDrift;
}

export interface ArtShape extends ArtPlacement {
  kind: ArtShapeKind;
}

export interface ArtSticker extends ArtPlacement {
  name: StickerName;
  kit: CoatKit;
}

export type ArtItem = ArtShape | ArtSticker;

export interface ArtComposition {
  id: string;
  shapes: readonly ArtShape[];
  stickers: readonly ArtSticker[];
}

export interface ArtBlockSize {
  width: number;
  height: number;
}

export interface ArtBox {
  left: number;
  top: number;
  width: number;
  height: number;
}

export const ART_FADE_MS = 600;
export const ART_HOLD_RANGE_MS = [12000, 16000] as const;

// Tiempos de permanencia por composición, en bucle: todos dentro de 12 a 16 s.
const HOLD_CYCLE_MS = [14000, 12000, 16000, 13000, 15000] as const;

export function holdDuration(index: number): number {
  return HOLD_CYCLE_MS[((index % HOLD_CYCLE_MS.length) + HOLD_CYCLE_MS.length) % HOLD_CYCLE_MS.length];
}

export function nextCompositionIndex(current: number, count: number): number {
  return count > 0 ? (current + 1) % count : 0;
}

export type ArtMotion = 'static' | 'paused' | 'running';

// Con movimiento reducido no hay deriva ni cambio; con la pestaña oculta todo se detiene.
export function artMotion(state: { reducedMotion: boolean; hidden: boolean }): ArtMotion {
  if (state.reducedMotion) {
    return 'static';
  }
  return state.hidden ? 'paused' : 'running';
}

// Zona del texto de marca (esquina inferior izquierda) más 24 px de margen y el desplazamiento de la deriva.
const BRAND_TEXT = { width: 318, height: 252 };
const BRAND_MARGIN = 24;

export function brandKeepOut(block: ArtBlockSize): ArtBox {
  const width = BRAND_TEXT.width + BRAND_MARGIN;
  const height = BRAND_TEXT.height + BRAND_MARGIN;
  return { left: 0, top: block.height - height, width, height };
}

export function boxesOverlap(a: ArtBox, b: ArtBox): boolean {
  return a.left < b.left + b.width && a.left + a.width > b.left && a.top < b.top + b.height && a.top + a.height > b.top;
}

function isSticker(item: ArtItem): item is ArtSticker {
  return 'name' in item;
}

// Proporción alto/ancho de cada forma; los stickers usan el lienzo del símbolo más el margen del halo.
function aspect(item: ArtItem): number {
  if (isSticker(item)) {
    const [w, h] = stickerCanvas(item.name);
    return (h + 14) / (w + 14);
  }
  switch (item.kind) {
    case 'peak':
      return 0.8;
    case 'half':
      return 0.5;
    default:
      return 1;
  }
}

// Caja del elemento en píxeles dentro de un bloque de ese tamaño, sin contar la rotación ni la deriva.
export function artItemBox(item: ArtItem, block: ArtBlockSize): ArtBox {
  const unit = Math.min(block.width, block.height) / 100;
  const width = item.size * unit;
  const height = width * aspect(item);
  const fromRight = item.corner === 'tr' || item.corner === 'br';
  const fromBottom = item.corner === 'bl' || item.corner === 'br';
  const offsetX = (item.x / 100) * block.width;
  const offsetY = (item.y / 100) * block.height;
  return {
    left: fromRight ? block.width - offsetX - width : offsetX,
    top: fromBottom ? block.height - offsetY - height : offsetY,
    width,
    height,
  };
}

// Vectores de deriva (magnitud 7 a 9 px, siempre bajo 12) y ciclos de 8 a 12 s; se reparten en orden.
const DRIFT_VECTORS: readonly (readonly [number, number])[] = [
  [6, -5], [-5, 7], [7, 4], [-7, -5], [4, 8], [-6, 6], [8, -3], [-3, -8],
];
const DRIFT_SECONDS = [9, 11, 8, 12, 10] as const;

let driftCounter = 0;

function nextDrift(): ArtDrift {
  const n = driftCounter++;
  const [dx, dy] = DRIFT_VECTORS[n % DRIFT_VECTORS.length];
  const duration = DRIFT_SECONDS[n % DRIFT_SECONDS.length];
  return { dx, dy, duration, delay: -((n * 1.7) % duration) - 0.1 };
}

function shape(kind: ArtShapeKind, corner: ArtCorner, x: number, y: number, size: number, rotate = 0): ArtShape {
  return { kind, corner, x, y, size, rotate, drift: nextDrift() };
}

function sticker(name: StickerName, kit: CoatKit, corner: ArtCorner, x: number, y: number, size: number, rotate = 0): ArtSticker {
  return { name, kit, corner, x, y, size, rotate, drift: nextDrift() };
}

// Cuatro composiciones: cada una con dos personajes y tres o cuatro objetos distintos de las demás.
// Las formas pegadas a un borde sobresalen 2 a 3 % de él para que la deriva (hasta 12 px) no deje un hueco.
// Los elementos grandes van arriba (fuera de la zona del texto de marca) y los pequeños en la franja derecha.
export const ART_COMPOSITIONS: readonly ArtComposition[] = [
  {
    id: 'compania',
    shapes: [
      shape('circle', 'tr', -12, -14, 62),
      shape('quarter', 'br', -3, -3, 25),
      shape('peak', 'tl', 5, 7, 24, -6),
      shape('ring', 'tl', 40, 36, 22),
      shape('square', 'tl', 4, 36, 13, 14),
      shape('half', 'tl', 58, 40, 20),
    ],
    stickers: [
      sticker('dog', 1, 'tr', 5, 9, 44),
      sticker('cat', 2, 'tl', 12, 28, 27, -3),
      sticker('bone', 1, 'tl', 6, 11, 12, -14),
      sticker('paw', 3, 'tl', 36, 6, 11, 12),
      sticker('heart', 1, 'tr', 6, 62, 11, 10),
      sticker('star', 1, 'tl', 34, 36, 9, -8),
    ],
  },
  {
    id: 'consulta',
    shapes: [
      shape('circle', 'tl', -12, -8, 46),
      shape('quarter', 'tr', -3, -3, 36),
      shape('ring', 'tr', 8, 34, 20),
      shape('peak', 'br', 3, -2, 22, 8),
      shape('square', 'tl', 50, 20, 12, -16),
      shape('half', 'tl', 6, 38, 22),
    ],
    stickers: [
      sticker('dachs', 4, 'tl', 5, 13, 46, -2),
      sticker('cat', 3, 'tr', 3, 46, 24, 4),
      sticker('stethoscope', 1, 'tl', 54, 7, 12, -12),
      sticker('pill', 1, 'tr', 8, 8, 11, 16),
      sticker('thermometer', 1, 'tl', 56, 34, 11, -10),
      sticker('bandage', 1, 'tr', 5, 68, 11, 12),
    ],
  },
  {
    id: 'cuidado',
    shapes: [
      shape('circle', 'tr', -14, -10, 54),
      shape('half', 'tl', -6, 38, 24),
      shape('quarter', 'br', -3, -3, 26),
      shape('peak', 'tl', 40, 26, 20, 6),
      shape('square', 'tl', 46, 42, 13, 12),
      shape('circle', 'tr', 8, 74, 9),
    ],
    stickers: [
      sticker('dog', 5, 'tl', 6, 12, 40, -4),
      sticker('dachs', 2, 'tr', 3, 48, 24, -2),
      sticker('shampoo', 1, 'tr', 6, 6, 11, 14),
      sticker('toothbrush', 1, 'tl', 50, 9, 11, -10),
      sticker('water-bowl', 1, 'tl', 6, 44, 13, 8),
      sticker('treat', 1, 'tl', 48, 32, 9, -14),
    ],
  },
  {
    id: 'hogar',
    shapes: [
      shape('half', 'tl', -4, 6, 26),
      shape('circle', 'tr', -10, 12, 50),
      shape('quarter', 'br', -3, -3, 26),
      shape('ring', 'tl', 24, 36, 22),
      shape('peak', 'tl', 60, 6, 22, -8),
      shape('square', 'tr', 34, 44, 12, 18),
    ],
    stickers: [
      sticker('cat', 6, 'tr', 5, 10, 40, 3),
      sticker('dog', 3, 'tl', 8, 34, 26, -2),
      sticker('shield', 1, 'tl', 54, 6, 12, 10),
      sticker('carrier', 1, 'tl', 7, 13, 14, -6),
      sticker('pet-bed', 1, 'tl', 44, 44, 13, 8),
      sticker('leash', 1, 'tr', 7, 64, 11, -12),
    ],
  },
];
