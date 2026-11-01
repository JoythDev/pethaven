// Convierte un parámetro de ruta en id numérico. Number() acepta valores como "12abc"
// o "1e3", así que se valida primero con una expresión regular para no llamar al API
// con un id inválido.
export const parseRouteId = (value: string | null): number | null => {
  if (value === null || !/^\d+$/.test(value)) {
    return null;
  }

  const id = Number(value);
  return Number.isSafeInteger(id) && id > 0 ? id : null;
};
