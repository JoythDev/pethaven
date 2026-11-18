import { HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, of, startWith } from 'rxjs';
import { Owner } from '../../models/owner.model';
import { Pet } from '../../models/pet.model';
import { getErrorMessage } from '../http/http-error';
import { ViewState } from '../http/view-state';

/** Lo que pinta la ficha: el dueño y sus mascotas, cargados en paralelo. */
export interface OwnerDetail {
  owner: Owner;
  pets: Pet[];
}

// El dueño se pide solo para distinguir «no existe» (404) de un fallo que se puede reintentar.
export type OwnerLoad = ViewState<OwnerDetail> | { status: 'not-found' };

export const OWNER_NOT_FOUND: OwnerLoad = { status: 'not-found' };

export const toOwnerLoad = () => (source: Observable<OwnerDetail>): Observable<OwnerLoad> =>
  source.pipe(
    map((data): OwnerLoad => ({ status: 'ready', data })),
    catchError((error: unknown) =>
      of<OwnerLoad>(
        error instanceof HttpErrorResponse && error.status === 404
          ? OWNER_NOT_FOUND
          : { status: 'error', message: getErrorMessage(error) },
      ),
    ),
    startWith<OwnerLoad>({ status: 'loading' }),
  );
