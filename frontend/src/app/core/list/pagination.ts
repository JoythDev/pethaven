export function pageCount(total: number, pageSize: number): number {
  return Math.max(1, Math.ceil(total / pageSize));
}

/** Página válida para el total actual: al encoger la lista se queda en la última. */
export function clampPage(page: number, total: number, pageSize: number): number {
  if (!Number.isFinite(page)) {
    return 1;
  }
  return Math.min(Math.max(1, Math.trunc(page)), pageCount(total, pageSize));
}

export function paginate<T>(items: readonly T[], page: number, pageSize: number): T[] {
  const current = clampPage(page, items.length, pageSize);
  return items.slice((current - 1) * pageSize, current * pageSize);
}

/** Posiciones (1-based) del primer y último elemento de la página; 0 y 0 si no hay ninguno. */
export function pageRange(total: number, page: number, pageSize: number): { from: number; to: number } {
  if (total <= 0) {
    return { from: 0, to: 0 };
  }
  const current = clampPage(page, total, pageSize);
  return { from: (current - 1) * pageSize + 1, to: Math.min(current * pageSize, total) };
}

/** «1 a 12 de 86»; sin elementos, «0 de 0». */
export function rangeText(total: number, page: number, pageSize: number): string {
  if (total <= 0) {
    return '0 de 0';
  }
  const { from, to } = pageRange(total, page, pageSize);
  return `${from} a ${to} de ${total}`;
}
