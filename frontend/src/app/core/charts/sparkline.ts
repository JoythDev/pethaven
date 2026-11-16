// Puntos de una línea sin ejes para un SVG de w por h, con 2 px de margen horizontal y 3 px vertical.
// Con todos los valores iguales, la línea va al centro.
export function sparklinePoints(values: readonly number[], width: number, height: number): string {
  if (values.length < 2) {
    return '';
  }
  const min = Math.min(...values);
  const range = Math.max(...values) - min;
  return values
    .map((value, i) => {
      const x = (i / (values.length - 1)) * (width - 4) + 2;
      const y = range === 0 ? height / 2 : height - 3 - ((value - min) / range) * (height - 6);
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(' ');
}
