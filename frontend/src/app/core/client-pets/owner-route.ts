import { ActivatedRoute } from '@angular/router';
import { Observable, combineLatest, map } from 'rxjs';

import { parseRouteId } from '../http/route-id';

/**
 * `:ownerId` de la ruta del portal del cliente. Vive en una ruta ancestra (y las rutas hijas no heredan
 * sus parámetros), así que se busca en toda la cadena; fuera del portal del cliente es null.
 */
export function ownerIdParam(route: ActivatedRoute): Observable<number | null> {
  return combineLatest(route.pathFromRoot.map((r) => r.paramMap)).pipe(
    map((maps) => {
      const found = maps.find((params) => params.has('ownerId'));
      return found ? parseRouteId(found.get('ownerId')) : null;
    }),
  );
}
