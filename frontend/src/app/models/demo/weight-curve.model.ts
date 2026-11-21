// Curva de peso (demo, S3): funcionalidad pendiente de backend.
export interface WeightPoint {
  date: Date;
  // Kilogramos.
  weight: number;
}

export interface WeightAxis {
  min: number;
  max: number;
  step: number;
}

// Tramo que muestra la curva: los últimos 6 o los 12 meses.
export type WeightRange = 6 | 12;

export const WEIGHT_RANGES: { value: WeightRange; label: string }[] = [
  { value: 6, label: '6 meses' },
  { value: 12, label: '12 meses' },
];

export interface WeightCurve {
  petId: number;
  // De la más antigua a la más reciente; la última es el peso real de la mascota.
  points: WeightPoint[];
  min: number;
  max: number;
  // Valores del eje vertical (S24).
  axis: WeightAxis;
  current: number;
  // Peso del mes anterior al primer punto; null si el primer punto es el primer registro.
  previous?: number | null;
  // Diferencia con el punto anterior, en kilogramos.
  change: number;
}
