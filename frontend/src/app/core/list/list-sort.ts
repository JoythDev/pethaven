import { ListSort } from './list-query';

export type SortValue = string | number | null | undefined;

/** Cómo leer el valor de cada campo ordenable de un recurso. */
export type SortAccessors<T> = Readonly<Record<string, (item: T) => SortValue>>;

const collator = new Intl.Collator('es', { sensitivity: 'base', numeric: true });

function isMissing(value: SortValue): value is null | undefined {
  return value === null || value === undefined || value === '';
}

/** Copia ordenada; el texto no distingue mayúsculas ni tildes, los vacíos van al final y el orden es estable. */
export function sortItems<T>(items: readonly T[], sort: ListSort | null, accessors: SortAccessors<T>): T[] {
  const read = sort ? accessors[sort.field] : undefined;
  if (!sort || !read) {
    return [...items];
  }
  const sign = sort.direction === 'asc' ? 1 : -1;
  return [...items].sort((a, b) => {
    const left = read(a);
    const right = read(b);
    if (isMissing(left) || isMissing(right)) {
      return Number(isMissing(left)) - Number(isMissing(right));
    }
    const result =
      typeof left === 'number' && typeof right === 'number' ? left - right : collator.compare(String(left), String(right));
    return result * sign;
  });
}
