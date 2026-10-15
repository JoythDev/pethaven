import { OperatorFunction, catchError, map, of, startWith } from 'rxjs';
import { getErrorMessage } from './http-error';

// Estado de una vista que depende de una petición: cargando, con datos o con error.
export type ViewState<T> =
  | { status: 'loading' }
  | { status: 'ready'; data: T }
  | { status: 'error'; message: string };

// Convierte una petición en estados de vista. Va dentro de switchMap, sobre la petición
// interna, para que cada una empiece en 'loading' y termine en 'ready' o 'error'.
export const toViewState = <T>(): OperatorFunction<T, ViewState<T>> => (source) =>
  source.pipe(
    map((data): ViewState<T> => ({ status: 'ready', data })),
    catchError((error: unknown) => of<ViewState<T>>({ status: 'error', message: getErrorMessage(error) })),
    startWith<ViewState<T>>({ status: 'loading' }),
  );
