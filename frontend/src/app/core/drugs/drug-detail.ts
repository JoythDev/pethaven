import { HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, of, startWith } from 'rxjs';
import { Drug } from '../../models/drug.model';
import { getErrorMessage } from '../http/http-error';
import { ViewState } from '../http/view-state';

// El 404 se distingue de un fallo que se puede reintentar.
export type DrugLoad = ViewState<Drug> | { status: 'not-found' };

export const DRUG_NOT_FOUND: DrugLoad = { status: 'not-found' };

export const toDrugLoad = () => (source: Observable<Drug>): Observable<DrugLoad> =>
  source.pipe(
    map((data): DrugLoad => ({ status: 'ready', data })),
    catchError((error: unknown) =>
      of<DrugLoad>(
        error instanceof HttpErrorResponse && error.status === 404
          ? DRUG_NOT_FOUND
          : { status: 'error', message: getErrorMessage(error) },
      ),
    ),
    startWith<DrugLoad>({ status: 'loading' }),
  );
