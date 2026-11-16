// Plan de cuidado diario (demo, S3): funcionalidad pendiente de backend.
import { StickerName } from '../../core/stickers/stickers';

export type CareItemKind = 'medication' | 'activity' | 'food' | 'water';

export interface CareItem {
  // Incluye el día y la mascota: marcar un cuidado vale solo para ese día.
  id: string;
  petId: number;
  // Hora 'HH:mm'.
  time: string;
  title: string;
  detail: string;
  sticker: StickerName;
  kind: CareItemKind;
}

export interface CareProgress {
  done: number;
  total: number;
}

// Lo que el plan necesita de un tratamiento real; lo cumplen Treatment y ClienteTreatment.
export interface CareTreatment {
  date: string;
  diagnosis: string;
  evolution: string | null;
  drugs: { drugId: number; drugName: string; units: number }[];
}
