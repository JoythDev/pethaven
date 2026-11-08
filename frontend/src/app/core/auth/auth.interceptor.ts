import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { API_BASE_URL } from '../api/api-base-url';
import { SKIP_SESSION_EXPIRED_REDIRECT } from './auth-context';

// Añade el token JWT a las peticiones dirigidas al API (nunca a otros hosts) y, si el API responde
// 401 a una petición que llevaba token, da la sesión por expirada y lleva al login.
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const apiUrl = inject(API_BASE_URL);
  const token = auth.token();

  const toApi = request.url.startsWith(`${apiUrl}/`) || request.url === apiUrl;
  if (!token || !toApi) {
    return next(request);
  }

  const authorized = request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  return next(authorized).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !request.context.get(SKIP_SESSION_EXPIRED_REDIRECT)
      ) {
        auth.expireSession();
      }
      return throwError(() => error);
    })
  );
};
