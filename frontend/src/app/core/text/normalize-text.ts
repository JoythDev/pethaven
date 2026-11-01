// Pasa a minúsculas y quita tildes para que la búsqueda no distinga acentos.
// El rango \u0300-\u036f cubre los signos diacríticos combinados (U+0300 a U+036F).
export const normalizeText = (value: string): string =>
  value
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
