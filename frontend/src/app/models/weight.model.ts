// Historial de pesos de una mascota (GET /pets/{id}/weights) y la curva que se dibuja con él.

// Pesaje tal como lo devuelve el API (WeightRecordResponse): la fecha llega en hora local, sin zona.
export interface WeightRecordResponse {
  id: number;
  date: string;
  // Kilogramos.
  weight: number;
}

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
  // Pesajes del tramo, del más antiguo al más reciente.
  points: WeightPoint[];
  // Inicio y fin del eje horizontal: el tramo completo hasta hoy.
  from: Date;
  to: Date;
  min: number;
  max: number;
  // Valores del eje vertical (S24).
  axis: WeightAxis;
  // Peso del registro anterior al primer punto; null si el primer punto es el primer registro.
  previous: number | null;
}
