// Consejo del día (demo, S22): colección fija; el de cada día se elige por fecha.
import { StickerName } from '../../core/stickers/stickers';

export interface DailyTip {
  id: string;
  headline: string;
  text: string;
  // Sticker que coincide con el tema del consejo.
  sticker: StickerName;
}
