import { dayNumber } from '../time/calendar';

const MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'] as const;
const MINUTE = 60_000;
const HOUR = 60 * MINUTE;

function plural(count: number, one: string, many: string): string {
  return `Hace ${count} ${count === 1 ? one : many}`;
}

/**
 * Tiempo relativo en español (12.2): «Hace 20 minutos», «Hace 2 horas», «Ayer» y, desde hace dos
 * días, la fecha corta («12 nov», con el año si no es el actual). Una hora futura cuenta como ahora.
 */
export function formatRelativeTime(at: Date, now: Date): string {
  const elapsed = now.getTime() - at.getTime();
  if (elapsed < MINUTE) {
    return 'Hace un momento';
  }
  if (elapsed < HOUR) {
    return plural(Math.floor(elapsed / MINUTE), 'minuto', 'minutos');
  }
  const days = dayNumber(now) - dayNumber(at);
  if (days === 0) {
    return plural(Math.floor(elapsed / HOUR), 'hora', 'horas');
  }
  if (days === 1) {
    return 'Ayer';
  }
  const date = `${at.getDate()} ${MONTHS[at.getMonth()]}`;
  return at.getFullYear() === now.getFullYear() ? date : `${date} ${at.getFullYear()}`;
}
