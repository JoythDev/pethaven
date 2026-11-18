import { HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, of, startWith } from 'rxjs';
import { Treatment } from '../../models/treatment.model';
import { Veterinarian } from '../../models/veterinarian.model';
import { getErrorMessage } from '../http/http-error';
import { ViewState } from '../http/view-state';

/** Lo que pinta la ficha: el veterinario y sus tratamientos (null si no se pudieron consultar). */
export interface VetDetail {
  vet: Veterinarian;
  treatments: Treatment[] | null;
}

// El veterinario se pide solo para distinguir «no existe» (404) de un fallo que se puede reintentar.
export type VetLoad = ViewState<VetDetail> | { status: 'not-found' };

export const VET_NOT_FOUND: VetLoad = { status: 'not-found' };

export const toVetLoad = () => (source: Observable<VetDetail>): Observable<VetLoad> =>
  source.pipe(
    map((data): VetLoad => ({ status: 'ready', data })),
    catchError((error: unknown) =>
      of<VetLoad>(
        error instanceof HttpErrorResponse && error.status === 404
          ? VET_NOT_FOUND
          : { status: 'error', message: getErrorMessage(error) },
      ),
    ),
    startWith<VetLoad>({ status: 'loading' }),
  );
