import { Species } from '../../models/pet.model';
import { identityIndex } from '../identity/identity';

export const CHARACTER_STICKERS = ['dog', 'cat', 'dachs'] as const;

export const OBJECT_STICKERS = [
  'bone', 'pill', 'syringe', 'calendar', 'paw', 'heart', 'food-bowl', 'shield', 'star',
  'stethoscope', 'thermometer', 'bandage', 'shampoo', 'toothbrush', 'leash', 'water-bowl', 'carrier', 'pet-bed', 'treat',
] as const;

export type CharacterSticker = (typeof CHARACTER_STICKERS)[number];
export type StickerName = CharacterSticker | (typeof OBJECT_STICKERS)[number];

// Kits de pelaje aprobados (sistema de diseño 6.1); el 1 es el de las variables por defecto.
export type CoatKit = 1 | 2 | 3 | 4 | 5 | 6;

// Margen alrededor del dibujo para que la silueta no se recorte.
const HALO_PAD = 7;

// Lienzo de cada símbolo y recorte de cabeza para avatares.
const CANVAS: Partial<Record<StickerName, [number, number]>> = { dog: [120, 120], cat: [120, 120], dachs: [150, 110] };
const HEAD_VIEWBOX: Record<CharacterSticker, string> = { dog: '18 6 84 84', cat: '18 2 84 84', dachs: '84 10 66 66' };

export function stickerCanvas(name: StickerName): [number, number] {
  return CANVAS[name] ?? [64, 64];
}

export function isCharacter(name: StickerName): name is CharacterSticker {
  return (CHARACTER_STICKERS as readonly string[]).includes(name);
}

export interface StickerFrame {
  viewBox: string;
  // Alto en píxeles para un ancho dado, respetando la proporción del recorte.
  height: number;
}

export function stickerFrame(name: StickerName, width: number, head = false): StickerFrame {
  if (head && isCharacter(name)) {
    return { viewBox: HEAD_VIEWBOX[name], height: width };
  }
  const [w, h] = stickerCanvas(name);
  const boxW = w + HALO_PAD * 2;
  const boxH = h + HALO_PAD * 2;
  return { viewBox: `${-HALO_PAD} ${-HALO_PAD} ${boxW} ${boxH}`, height: Math.round((width * boxH) / boxW) };
}

// Sin información de pelaje, el kit se elige con el id para que sea estable.
export function coatKitFor(id: number | string): CoatKit {
  return ((identityIndex(id) % 6) + 1) as CoatKit;
}

export function speciesSticker(species: Species): CharacterSticker {
  return species === Species.CAT ? 'cat' : 'dog';
}
