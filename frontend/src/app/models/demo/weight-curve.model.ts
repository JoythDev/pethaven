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

export interface WeightCurve {
  petId: number;
  // De la más antigua a la más reciente; la última es el peso real de la mascota.
  points: WeightPoint[];
  min: number;
  max: number;
  // Valores del eje vertical (S24).
  axis: WeightAxis;
  current: number;
  // Diferencia con el punto anterior, en kilogramos.
  change: number;
}
